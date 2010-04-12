/**
 * Copyright 2009-2010 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Scriptlet;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;

/**
 * A {@link LanguageAdapter} that supports the <a
 * href="http://clojure.org/">Clojure</a> language.
 * <p>
 * Note: Clojure adapters in all {@link LanguageManager} will share the same
 * runtime instance of Clojure, which is a JVM-wide singleton.
 * <p>
 * This is important! Even though every execution context gets its own Clojure
 * namespace, all namespaces exist in the same runtime. If one execution context
 * loads code into a namespace, all execution contexts will be able to access
 * it. This means that two contexts cannot use different versions of a library
 * at the same time if the versions have the same namespace.
 * <p>
 * Note that {@link Scriptlet#prepare()} does not actually compile the source
 * code, but it does parse it, making it quicker to execute later.
 * 
 * @author Tal Liron
 */
public class ClojureAdapter implements LanguageAdapter
{
	//
	// Constants
	//

	public static final Symbol CLOJURE_CORE = Symbol.create( "clojure.core" );

	public static final Var IN_NS = RT.var( "clojure.core", "in-ns" );

	public static final Var REFER = RT.var( "clojure.core", "refer" );

	//
	// Static operations
	//

	public static Namespace getClojureNamespace( ExecutionContext executionContext )
	{
		Namespace ns = (Namespace) executionContext.getAttributes().get( CLOJURE_NAMESPACE );
		if( ns == null )
		{
			// We need to create a fresh namespace for each execution context.

			String name = NAMESPACE_PREFIX + executionContext.hashCode();
			ns = Namespace.findOrCreate( Symbol.intern( name ) );
			executionContext.getAttributes().put( CLOJURE_NAMESPACE, ns );
		}

		// Expose our variables by interning them in the namespace
		for( Map.Entry<String, Object> entry : executionContext.getExposedVariables().entrySet() )
			Var.intern( ns, Symbol.intern( entry.getKey() ), entry.getValue() );

		return ns;
	}

	/**
	 * From somethingLikeThis to something-like-this.
	 * 
	 * @param camelCase
	 * @return
	 */
	public static String toClojureStyle( String camelCase )
	{
		StringBuilder r = new StringBuilder();
		char c = camelCase.charAt( 0 );
		if( Character.isUpperCase( c ) )
			r.append( Character.toLowerCase( c ) );
		else
			r.append( c );
		for( int i = 1; i < camelCase.length(); i++ )
		{
			c = camelCase.charAt( i );
			if( Character.isUpperCase( c ) )
			{
				r.append( '-' );
				r.append( Character.toLowerCase( c ) );
			}
			else
				r.append( c );
		}
		return r.toString();
	}

	//
	// Construction
	//

	public ClojureAdapter() throws LanguageAdapterException
	{
		attributes.put( NAME, "Clojure" );
		attributes.put( VERSION, "?" );
		attributes.put( LANGUAGE_NAME, "Clojure" );
		attributes.put( LANGUAGE_VERSION, "?" );
		attributes.put( EXTENSIONS, Arrays.asList( "clj" ) );
		attributes.put( DEFAULT_EXTENSION, "clj" );
		attributes.put( TAGS, Arrays.asList( "clojure" ) );
		attributes.put( DEFAULT_TAG, "clojure" );
	}

	//
	// LanguageAdapter
	//

	public Map<String, Object> getAttributes()
	{
		return attributes;
	}

	public boolean isThreadSafe()
	{
		return true;
	}

	public Lock getLock()
	{
		return lock;
	}

	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = literal.replaceAll( "\\n", "\\\\n" );
		literal = literal.replaceAll( "\\\"", "\\\\\"" );
		return "(print \"" + literal + "\")";
	}

	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "(print " + expression + ")";
	}

	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		return "(.. " + executable.getExposedExecutableName() + " getContainer (includeDocument " + expression + "))";
	}

	public Scriptlet createScriptlet( String sourceCode, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new ClojureScriptlet( sourceCode, startLineNumber, startColumnNumber, executable );
	}

	public Object invoke( String entryPointName, Executable executable, ExecutionContext executionContext ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		entryPointName = toClojureStyle( entryPointName );
		Namespace ns = getClojureNamespace( executionContext );
		try
		{
			// We must push *ns* in order to use (in-ns) below
			Var.pushThreadBindings( RT.map( RT.CURRENT_NS, RT.CURRENT_NS.deref(), RT.OUT, executionContext.getWriter(), RT.ERR, executionContext.getErrorWriter() ) );

			IN_NS.invoke( ns.getName() );
			REFER.invoke( CLOJURE_CORE );

			Var function = ns.findInternedVar( Symbol.intern( entryPointName ) );
			if( function == null )
				throw new NoSuchMethodException( entryPointName );

			return function.invoke();
		}
		catch( NoSuchMethodException x )
		{
			throw x;
		}
		catch( Exception x )
		{
			throw new ExecutionException( executable.getDocumentName(), x.getMessage(), x );
		}
		finally
		{
			Var.popThreadBindings();
		}
	}

	public void releaseContext( ExecutionContext executionContext )
	{
		// Remove our namespace
		Namespace ns = (Namespace) executionContext.getAttributes().get( CLOJURE_NAMESPACE );
		if( ns != null )
			Namespace.remove( ns.getName() );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final String CLOJURE_NAMESPACE = "clojure.namespace";

	private static final String NAMESPACE_PREFIX = "scripturian";

	// private static final Var COMPILE = RT.var( "clojure.core", "compile" );

	// private static final Var LOGGER_NAME = RT.var( "clojure.core",
	// "-logger-name" );

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private final ReentrantLock lock = new ReentrantLock();
}
