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

package com.threecrickets.scripturian.internal;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.threecrickets.scripturian.EmbeddedScript;
import com.threecrickets.scripturian.ScriptSource;
import com.threecrickets.scripturian.ScriptedMain;

/**
 * This is the <code>script.container</code> variable exposed to the script.
 * 
 * @author Tal Liron
 * @see ScriptedMain
 */
public class ExposedScriptedMainContainer
{
	//
	// Construction
	//

	public ExposedScriptedMainContainer( ScriptedMain scriptedMain )
	{
		this.scriptedMain = scriptedMain;
	}

	//
	// Operations
	//

	/**
	 * This powerful method allows scripts to execute other scripts in place,
	 * and is useful for creating large, maintainable applications based on
	 * scripts. Included scripts can act as a library or toolkit and can even be
	 * shared among many applications. The included script does not have to be
	 * in the same language or use the same engine as the calling script.
	 * However, if they do use the same engine, then methods, functions,
	 * modules, etc., could be shared. It is important to note that how this
	 * works varies a lot per scripting platform. For example, in JRuby, every
	 * script is run in its own scope, so that sharing would have to be done
	 * explicitly in the global scope. See the included embedded Ruby script
	 * example for a discussion of various ways to do this.
	 * 
	 * @param name
	 *        The script name
	 * @throws IOException
	 * @throws ScriptException
	 */
	public void include( String name ) throws IOException, ScriptException
	{
		include( name, null );
	}

	/**
	 * As {@link #include(String)}, except that the script is not embedded. As
	 * such, you must explicitly specify the name of the scripting engine that
	 * should evaluate it.
	 * 
	 * @param name
	 *        The script name
	 * @param engineName
	 *        The script engine name (if null, behaves identically to
	 *        {@link #include(String)}
	 * @throws IOException
	 * @throws ScriptException
	 */
	public void include( String name, String engineName ) throws IOException, ScriptException
	{
		String text = ScripturianUtil.getString( new File( name ) );
		if( engineName != null )
			text = EmbeddedScript.DEFAULT_DELIMITER1_START + engineName + " " + text + EmbeddedScript.DEFAULT_DELIMITER1_END;

		ScriptSource.ScriptDescriptor<EmbeddedScript> scriptDescriptor = scriptedMain.getScriptSource().getScriptDescriptor( name );
		EmbeddedScript script = scriptDescriptor.getScript();
		if( script == null )
		{
			script = new EmbeddedScript( text, scriptedMain.getScriptEngineManager(), getDefaultEngineName(), scriptedMain.getScriptSource(), scriptedMain.isAllowCompilation() );
			EmbeddedScript existing = scriptDescriptor.setScriptIfAbsent( script );
			if( existing != null )
				script = existing;
		}

		script.run( scriptedMain.getWriter(), scriptedMain.getErrorWriter(), true, scriptEngines, this, scriptedMain.getScriptContextController(), false );
	}

	//
	// Attributes
	//

	/**
	 * The arguments sent to {@link ScriptedMain#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return scriptedMain.getArguments();
	}

	/**
	 * The default script engine name to be used if the script doesn't specify
	 * one. Defaults to "js".
	 * 
	 * @return The default script engine name
	 * @see #setDefaultEngineName(String)
	 */
	public String getDefaultEngineName()
	{
		return defaultEngineName;
	}

	/**
	 * @param defaultEngineName
	 *        The default script engine name
	 * @see #getDefaultEngineName()
	 */
	public void setDefaultEngineName( String defaultEngineName )
	{
		this.defaultEngineName = defaultEngineName;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final ScriptedMain scriptedMain;

	private final ConcurrentMap<String, ScriptEngine> scriptEngines = new ConcurrentHashMap<String, ScriptEngine>();

	private String defaultEngineName = "js";
}
