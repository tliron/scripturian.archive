/**
 * Copyright 2009-2012 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.gnu.org/copyleft/lesser.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://threecrickets.com/
 */

package com.threecrickets.scripturian.adapter;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.optimizer.ClassCompiler;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.Program;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.LanguageAdapterException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.StackFrame;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * A {@link LanguageAdapter} that supports the JavaScript language as
 * implemented by <a href="http://www.mozilla.org/rhino/">Rhino</a>.
 * 
 * @author Tal Liron
 */
public class RhinoAdapter extends LanguageAdapterBase
{
	//
	// Constants
	//

	/**
	 * The Rhino context attribute.
	 */
	public static final String RHINO_CONTEXT = "rhino.context";

	/**
	 * The Rhino scope attribute.
	 */
	public static final String RHINO_SCOPE = "rhino.scope";

	/**
	 * The default base directory for cached executables.
	 */
	public static final String JAVASCRIPT_CACHE_DIR = "javascript";

	/**
	 * The Rhino optimization level.
	 */
	public static int OPTIMIZATION_LEVEL = 9; // -1 = interpreted mode;

	//
	// Static operations
	//

	/**
	 * Creates an execution exception with a full stack.
	 * 
	 * @param documentName
	 *        The document name
	 * @param x
	 *        The exception
	 * @return The execution exception
	 */
	public static ExecutionException createExecutionException( String documentName, Exception x )
	{
		if( x instanceof RhinoException )
		{
			RhinoException rhinoException = (RhinoException) x;
			Throwable cause = rhinoException.getCause();
			if( cause instanceof ExecutionException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
				executionException.getStack().addAll( ( (ExecutionException) cause ).getStack() );
				executionException.getStack().add( new StackFrame( rhinoException.sourceName(), rhinoException.lineNumber(), rhinoException.columnNumber() ) );
				return executionException;
			}
			else if( cause instanceof ParsingException )
			{
				ExecutionException executionException = new ExecutionException( cause.getMessage(), cause.getCause() );
				executionException.getStack().addAll( ( (ParsingException) cause ).getStack() );
				executionException.getStack().add( new StackFrame( rhinoException.sourceName(), rhinoException.lineNumber(), rhinoException.columnNumber() ) );
				return executionException;
			}
			else
				return new ExecutionException( rhinoException.sourceName(), rhinoException.lineNumber(), rhinoException.columnNumber(), rhinoException.getMessage(), x );
		}
		else
			return new ExecutionException( documentName, x.getMessage(), x );
	}

	//
	// Construction
	//

	/**
	 * Constructor.
	 * 
	 * @throws LanguageAdapterException
	 */
	public RhinoAdapter() throws LanguageAdapterException
	{
		super( "Rhino", new ContextFactory().enterContext().getImplementationVersion(), "JavaScript", "", Arrays.asList( "js", "javascript" ), "js", Arrays.asList( "javascript", "js", "rhino" ), "rhino" );

		CompilerEnvirons compilerEnvirons = new CompilerEnvirons();
		compilerEnvirons.setOptimizationLevel( OPTIMIZATION_LEVEL );
		classCompiler = new ClassCompiler( compilerEnvirons );
		generatedClassLoader = Context.getCurrentContext().createClassLoader( RhinoAdapter.class.getClassLoader() );
		Context.exit();
	}

	//
	// Attributes
	//

	/**
	 * Enters a Rhino context stored in the execution context, creating it if it
	 * doesn't exist. Each execution context is guaranteed to have its own Rhino
	 * context. Make sure to exit the context when done with it!
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The Rhino context
	 */
	public Context enterContext( ExecutionContext executionContext )
	{
		Context context = (Context) executionContext.getAttributes().get( RHINO_CONTEXT );

		if( context == null )
		{
			context = enterContext();
			executionContext.getAttributes().put( RHINO_CONTEXT, context );
		}
		else
			contextFactory.enterContext( context );

		return context;
	}

	/**
	 * Gets the Rhino scope associated with the Rhino context of the execution
	 * context, creating it if it doesn't exist. Each execution context is
	 * guaranteed to have its own Rhino scope. The scope is updated to match the
	 * writers and services in the execution context.
	 * 
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 * @param context
	 *        The Rhino context
	 * @param startLineNumber
	 *        The start line number of the scriptlet
	 * @return The Rhino scope
	 * @see #enterContext(ExecutionContext)
	 */
	public ScriptableObject getScope( Executable executable, ExecutionContext executionContext, Context context, int startLineNumber )
	{
		ScriptableObject scope = (ScriptableObject) executionContext.getAttributes().get( RHINO_SCOPE );

		if( scope == null )
		{
			scope = new ImporterTopLevel( context );
			context.initStandardObjects( scope );
			classChache.associate( scope );
			executionContext.getAttributes().put( RHINO_SCOPE, scope );

			String printSource = "function print(s){if(undefined===s){return}" + executable.getExecutableServiceName() + ".context.writerOrDefault.write(String(s));" + executable.getExecutableServiceName()
				+ ".context.writerOrDefault.flush()}";
			Function printFunction = context.compileFunction( scope, printSource, null, 0, null );
			scope.defineProperty( "print", printFunction, 0 );

			printSource = "function println(s){print(s);if(undefined===println.separator){println.separator=String(java.lang.System.getProperty('line.separator'))}print(println.separator)}";
			printFunction = context.compileFunction( scope, printSource, null, 0, null );
			scope.defineProperty( "println", printFunction, 0 );
		}

		// Define services as properties in scope
		for( Map.Entry<String, Object> entry : executionContext.getServices().entrySet() )
			scope.defineProperty( entry.getKey(), entry.getValue(), ScriptableObject.PERMANENT | ScriptableObject.READONLY );

		return scope;
	}

	/**
	 * The base directory for cached executables.
	 * 
	 * @return The cache directory
	 */
	public File getCacheDir()
	{
		return new File( LanguageManager.getCachePath(), JAVASCRIPT_CACHE_DIR );
	}

	//
	// LanguageAdapter
	//

	@Override
	public String getSourceCodeForLiteralOutput( String literal, Executable executable ) throws ParsingException
	{
		literal = ScripturianUtil.doubleQuotedLiteral( literal );
		return "print(" + literal + ");";
	}

	@Override
	public String getSourceCodeForExpressionOutput( String expression, Executable executable ) throws ParsingException
	{
		return "print(" + expression + ");";
	}

	@Override
	public String getSourceCodeForExpressionInclude( String expression, Executable executable ) throws ParsingException
	{
		String containerIncludeExpressionCommand = (String) getManager().getAttributes().get( LanguageManager.CONTAINER_INCLUDE_EXPRESSION_COMMAND );
		return executable.getExecutableServiceName() + ".container." + containerIncludeExpressionCommand + "(" + expression + ");";
	}

	public Program createProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable ) throws ParsingException
	{
		return new RhinoProgram( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, this );
	}

	@Override
	public Object enter( String entryPointName, Executable executable, ExecutionContext executionContext, Object... arguments ) throws NoSuchMethodException, ParsingException, ExecutionException
	{
		// We need a fresh context here, because Rhino does not allow us to
		// share contexts between threads
		Context context = enterContext();
		try
		{
			ScriptableObject scope = getScope( executable, executionContext, context, -1 );
			Object o = scope.get( entryPointName, null );
			if( !( o instanceof Function ) )
				throw new NoSuchMethodException( entryPointName );
			Function function = (Function) o;
			Object r = function.call( context, scope, scope, arguments );
			if( r instanceof Wrapper )
				r = ( (Wrapper) r ).unwrap();
			return r;
		}
		catch( NoSuchMethodException x )
		{
			throw x;
		}
		catch( Exception x )
		{
			throw RhinoAdapter.createExecutionException( executable.getDocumentName(), x );
		}
		finally
		{
			Context.exit();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	/**
	 * Rhino class compiler.
	 */
	protected final ClassCompiler classCompiler;

	/**
	 * Rhino class loader.
	 */
	protected final GeneratedClassLoader generatedClassLoader;

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Used to generate and enter Rhino contexts.
	 */
	private final ContextFactory contextFactory = new ContextFactory();

	/**
	 * Class cache shared by all Rhino contexts.
	 */
	private final ClassCache classChache = new ClassCache();

	/**
	 * Creates and enters a context.
	 * 
	 * @return A context
	 */
	private Context enterContext()
	{
		Context context = contextFactory.enterContext();
		context.setLanguageVersion( Context.VERSION_1_8 );
		context.setOptimizationLevel( OPTIMIZATION_LEVEL );
		return context;
	}
}
