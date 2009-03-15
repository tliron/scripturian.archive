/**
 * Copyright 2009 Three Crickets.
 * <p>
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * <p>
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * <p>
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * <p>
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * <p>
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.helper;

import javax.script.ScriptEngine;

import com.threecrickets.scripturian.EmbeddedScript;
import com.threecrickets.scripturian.EmbeddedScriptParsingHelper;
import com.threecrickets.scripturian.ScriptEngines;

/**
 * An {@link EmbeddedScriptParsingHelper} that supports the <a
 * href="http://groovy.codehaus.org/">Groovy</a> scripting language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"groovy", "Groovy"
})
public class GroovyEmbeddedParsingHelper implements EmbeddedScriptParsingHelper
{
	//
	// EmbeddedParsingHelper
	//

	public String getScriptHeader( ScriptEngine scriptEngine )
	{
		// There's a bug in Groovy's script engine implementation (as of version
		// 1.6) that makes it lose the connection between the script's output
		// and our script context writer in some cases. This makes sure that
		// they are connected.
		return "out=context.writer;";
	}

	public String getScriptFooter( ScriptEngine scriptEngine )
	{
		return null;
	}

	public String getTextAsProgram( ScriptEngine scriptEngine, String content )
	{
		content = content.replaceAll( "\\n", "\\\\n" );
		content = content.replaceAll( "\\'", "\\\\'" );
		return "print('" + content + "');";
	}

	public String getExpressionAsProgram( ScriptEngine scriptEngine, String content )
	{
		return "print(" + content + ");";
	}

	public String getExpressionAsInclude( ScriptEngine scriptEngine, String content )
	{
		return EmbeddedScript.containerVariableName + ".include(" + content + ");";
	}

	public String getInvocationAsProgram( ScriptEngine scriptEngine, String content )
	{
		return null;
	}
}
