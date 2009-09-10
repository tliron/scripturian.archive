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

package com.threecrickets.scripturian;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

/**
 * Encapsulates context for an {@link Document}. Every thread calling
 * {@link Document#run(boolean, Writer, Writer, boolean, DocumentContext, Object, ScriptletController)}
 * must use its own context.
 * 
 * @author Tal Liron
 */
public class DocumentContext
{
	//
	// Construction
	//

	/**
	 * Creates a context for a specific script engine manager.
	 * 
	 * @param scriptEngineManager
	 *        A script engine manager
	 */
	public DocumentContext( ScriptEngineManager scriptEngineManager )
	{
		this.scriptEngineManager = scriptEngineManager;
	}

	//
	// Attributes
	//

	/**
	 * The script engine manager used for {@link #getScriptEngine(String)}.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getScriptEngineManager()
	{
		return scriptEngineManager;
	}

	/**
	 * A cached script engine. All script engines will use the same
	 * {@link ScriptContext}.
	 * 
	 * @param scriptEngineName
	 *        The script engine name
	 * @return The cached script engine
	 * @throws ScriptException
	 */
	public ScriptEngine getScriptEngine( String scriptEngineName ) throws ScriptException
	{
		if( scriptEngines == null )
			scriptEngines = new HashMap<String, ScriptEngine>();

		lastScriptEngine = scriptEngines.get( scriptEngineName );

		if( lastScriptEngine == null )
		{
			lastScriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
			if( lastScriptEngine == null )
				throw new ScriptException( "Unsupported script engine: " + scriptEngineName );

			// (Note that some script engines do not even
			// provide a default context -- Jepp, for example -- so
			// it's generally a good idea to explicitly set one)
			lastScriptEngine.setContext( getScriptContext() );

			scriptEngines.put( scriptEngineName, lastScriptEngine );
		}

		lastScriptEngineName = scriptEngineName;

		return lastScriptEngine;
	}

	/**
	 * The single script context used by all scriptlets in the document.
	 * 
	 * @return The script context
	 */
	public ScriptContext getScriptContext()
	{
		if( scriptContext == null )
		{
			scriptContext = new SimpleScriptContext();
			scriptContext.setBindings( new SimpleBindings(), ScriptContext.ENGINE_SCOPE );
			scriptContext.setBindings( new SimpleBindings(), ScriptContext.GLOBAL_SCOPE );
		}
		return scriptContext;
	}

	/**
	 * The last {@link ScriptEngine} used in the last run of the document.
	 * 
	 * @return The last script engine
	 */
	public ScriptEngine getLastScriptEngine()
	{
		return lastScriptEngine;
	}

	/**
	 * The name of the {@link ScriptEngine} used in the last run of the
	 * document.
	 * 
	 * @return The last script engine name
	 */
	public String getLastScriptEngineName()
	{
		return lastScriptEngineName;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final ScriptEngineManager scriptEngineManager;

	private ScriptContext scriptContext;

	private Map<String, ScriptEngine> scriptEngines;

	private ScriptEngine lastScriptEngine;

	private String lastScriptEngineName;
}
