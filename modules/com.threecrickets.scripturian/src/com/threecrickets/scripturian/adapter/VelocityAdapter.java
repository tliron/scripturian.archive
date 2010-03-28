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

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.exception.LanguageInitializationException;

/**
 * An {@link LanguageAdapter} that supports the <a
 * href="http://velocity.apache.org/">Velocity</a> templating language.
 * 
 * @author Tal Liron
 */
@ScriptEngines(
{
	"velocity", "Velocity"
})
public class VelocityAdapter extends Jsr223LanguageAdapter
{
	//
	// ScriptletHelper
	//

	public VelocityAdapter() throws LanguageInitializationException
	{
		super();
	}

	@Override
	public String getScriptletHeader( Executable document, ScriptEngine scriptEngine )
	{
		return "#set($_d='$')#set($_h='#')";
	}

	@Override
	public String getTextAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		// 
		// content = content.replaceAll( "\\#", "\\\\#" );
		// content = content.replaceAll( "\\$", "\\\\\\$" );

		content = content.replaceAll( "\\$", "\\${_d}" );
		content = content.replaceAll( "\\#", "\\${_h}" );
		return content;

		// content = content.replaceAll( "\\#end", "\n#end\n#literal()\n" );
		// content = content.replaceAll( "\\#\\#", "FIUSH" );
		// return "\n#literal()\n" + content + "\n#end\n";
	}

	@Override
	public String getExpressionAsProgram( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "${" + content.trim() + "}";
	}

	@Override
	public String getExpressionAsInclude( Executable document, ScriptEngine scriptEngine, String content )
	{
		return "#if($" + document.getExecutableVariableName() + ".container.includeDocument(" + content + "))#end ";
	}
}
