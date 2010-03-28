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

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports the Python scripting language as
 * implemented by <a href="http://jepp.sourceforge.net/">Jepp</a>.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"jepp", "jep"
})
public class JeppAdapter extends Jsr223LanguageAdapter
{
	//
	// ScriptletHelper
	//

	public JeppAdapter() throws LanguageInitializationException
	{
		super();
	}

	@Override
	public String getScriptletHeader( Executable document, ScriptEngine scriptEngine )
	{
		// Apparently the Java Scripting support for Jepp does not correctly
		// set global variables, not redirect stdout and stderr. Luckily, the
		// Python interface is compatible with Java's Writer interface, so we
		// can redirect them explicitly.
		return "import sys;sys.stdout=context.getWriter();sys.stderr=context.getErrorWriter();";
	}

	@Override
	public void beforeCall( ScriptEngine scriptEngine, ExecutionContext executionContext )
	{
		StringBuilder r = new StringBuilder();
		for( String var : executionContext.getExposedVariables().keySet() )
		{
			r.append( var );
			r.append( "=context.getAttribute('" );
			r.append( var );
			r.append( "');" );
		}

		try
		{
			scriptEngine.eval( r.toString() );
		}
		catch( ScriptException x )
		{
		}
	}

	@Override
	public String getTextAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\\"", "\\\\\"" );
		return "sys.stdout.write(\"" + content + "\"),;";
	}

	@Override
	public String getExpressionAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "sys.stdout.write(" + content + ");";
	}

	@Override
	public String getExpressionAsInclude( Executable document, ScriptEngine scriptEngine, String content )
	{
		return document.getExposedExecutableName() + ".getContainer().includeDocument(" + content + ");";
	}
}
