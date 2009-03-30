package com.threecrickets.scripturian.internal;

import java.io.Writer;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.threecrickets.scripturian.EmbeddedScript;

/**
 * This is the type of the "script" variable exposed to the script. The name is
 * set according to {@link EmbeddedScript#getScriptVariableName()}.
 * 
 * @author Tal Liron
 * @see EmbeddedScript
 */
public class EmbeddedScriptScript
{
	//
	// Construction
	//

	public EmbeddedScriptScript( EmbeddedScript embeddedScript, ScriptEngine scriptEngine, ScriptContext scriptContext )
	{
		this.embeddedScript = embeddedScript;
		this.scriptEngine = scriptEngine;
		this.scriptContext = scriptContext;
	}

	//
	// Attributes
	//

	/**
	 * Setting this to something greater than 0 enables caching of the script
	 * results for a maximum number of milliseconds. By default cacheDuration is
	 * 0, so that each request causes the script to be evaluated. This class
	 * does not handle caching itself. Caching can be provided by your
	 * environment if appropriate.
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 * @see EmbeddedScript#cacheDuration
	 */
	public long getCacheDuration()
	{
		return embeddedScript.getCacheDuration();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		embeddedScript.setCacheDuration( cacheDuration );
	}

	/**
	 * The source of the script.
	 * 
	 * @return The script source
	 */
	public Object getSource()
	{
		return embeddedScript.getSource();
	}

	/**
	 * This is the {@link ScriptEngine} used by the script. Scripts may use it
	 * to get information about the engine's capabilities.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getEngine()
	{
		return scriptEngine;
	}

	/**
	 * This is the {@link ScriptEngineManager} used to create the script engine.
	 * Scripts may use it to get information about what other engines are
	 * available.
	 * 
	 * @return The script engine manager
	 */
	public ScriptEngineManager getEngineManager()
	{
		return embeddedScript.getScriptEngineManager();
	}

	/**
	 * This {@link ConcurrentMap} provides a convenient location for global
	 * values shared by all scripts, run by all engines.
	 * 
	 * @return The values
	 */
	public ConcurrentMap<String, Object> getMeta()
	{
		return MetaScope.getInstance().getValues();
	}

	/**
	 * This is the {@link ScriptContext} used by the script. Scripts may use it
	 * to get access to the {@link Writer} objects used for standard output and
	 * standard error.
	 * 
	 * @return The script context
	 */
	public ScriptContext getContext()
	{
		return scriptContext;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final EmbeddedScript embeddedScript;

	private final ScriptEngine scriptEngine;

	private final ScriptContext scriptContext;
}