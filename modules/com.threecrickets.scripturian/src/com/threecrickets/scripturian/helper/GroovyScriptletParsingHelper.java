/**
 * Copyright 2009 Three Crickets LLC.
 * <p>
 * The contents of this file are subject to the terms of the LGPL version 3.0:
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.helper;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.CompositeScript;
import com.threecrickets.scripturian.ScriptletParsingHelper;
import com.threecrickets.scripturian.ScriptEngines;

/**
 * An {@link ScriptletParsingHelper} that supports the <a
 * href="http://groovy.codehaus.org/">Groovy</a> scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"groovy", "Groovy"
})
public class GroovyScriptletParsingHelper implements ScriptletParsingHelper
{
	//
	// ScriptletParsingHelper
	//

	public boolean isPrintOnEval()
	{
		return false;
	}

	public String getScriptletHeader( CompositeScript compositeScript, ScriptEngine scriptEngine )
	{
		// There's a bug in Groovy's script engine implementation (as of version
		// 1.6) that makes it lose the connection between the script's output
		// and our script context writer in some cases. This makes sure that
		// they are connected.
		return "out=context.writer;";
	}

	public String getScriptletFooter( CompositeScript compositeScript, ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	public String getExpressionAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	public String getExpressionAsInclude( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return compositeScript.getScriptVariableName() + ".container.include(" + content + ");";
	}

	public String getInvocationAsProgram( CompositeScript compositeScript, ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
