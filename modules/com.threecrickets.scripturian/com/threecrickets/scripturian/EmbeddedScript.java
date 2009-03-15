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

package com.threecrickets.scripturian;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

/**
 * Handles the parsing, optional compilation and running of embedded scripts.
 * <p>
 * Embedded scripts are text streams containing a mix of script code, which is
 * embedded between special delimiters, and plain text. During the parsing
 * stage, which happens only once in the constructor, the entire text stream is
 * converted into script code, with the non-delimited plain text being sent to
 * standard output via whatever method is appropriate for the scripting engine
 * (see {@link EmbeddedScriptParsingHelper}). The exception to this is text
 * streams beginning with plain text -- in such cases, just that first section
 * is sent to your specified output in pure Java. The reason is that no script
 * engine has been specified yet. Also, if the entire file is just plain text,
 * this causes it to be sent only via Java, without invoking any scripting
 * engine.
 * <p>
 * Text streams are often provided by an implementation {@link ScriptSource},
 * though your environment can use its own method.
 * <p>
 * Embedded scripts can easily be abused, becoming hard to read and hard to
 * maintain, since both code and plain text are mixed together. However, they
 * can boost productivity in environments which mostly just output plain text.
 * For example, in an application that dynamically generates HTML web pages, it
 * is likely that most files will be in HTML with only some parts of some files
 * containing embedded script. In such cases, it is easy to have a professional
 * web designer work on the HTML parts while the embedded scripts are reserved
 * for the programmer. Web design software can often recognize embedded script
 * and take care to keep it safe while the web designer makes changes to the
 * file.
 * <p>
 * This class can support multiple script languages and engines within the same
 * text. Each embedded script segment can specify which engine it uses
 * explicitly at the opening delimiter. If the engine is not specified, whatever
 * engine was previously used in the file will be used. If no engine was
 * previously specified, a default value is used (supplied in the constructor).
 * <p>
 * This class support either the JSP/ASP style of delimiting script code (using
 * percentage signs), or the PHP style (using question marks). However, each
 * embedded script file must adhere to only one style throughout.
 * <p>
 * In addition to embedded scripts, this class supports a shorthand for embedded
 * script expressions. These are internally just sent to standard output.
 * However, they can allow for slightly more compact and cleaner code.
 * <p>
 * Another shorthand exists for including other script files. However, for it to
 * work, you must make sure that a container.include(name) method is available
 * to the script, which would then process the include as is appropriate to your
 * environment. To make this available, you can set the "container" global
 * variable by supplied a {@link ScriptContextController} when you call
 * {@link #run(Writer, Writer, Map, ScriptContextController, boolean)} . Note
 * this name can be changed via {@link #containerVariableName}.
 * <p>
 * Examples:
 * <ul>
 * <li><b>JSP/ASP-style delimiters</b>: &lt;% print('Hello World'); %&gt;</li>
 * <li><b>PHP-style delimiters</b>: &lt;? script.cacheDuration.set 5000 ?&gt;</li>
 * <li><b>Specifying engine name</b>: &lt;%groovy print myVariable %&gt;
 * &lt;?php container.include(lib_name); ?&gt;</li>
 * <li><b>Output expression</b>: &lt;?= 15 * 6 ?&gt;</li>
 * <li><b>Output expression with specifying engine name</b>: &lt;?=js
 * sqrt(myVariable) ?&gt;</li>
 * <li><b>Include</b>: &lt;%& 'library.js' %&gt; &lt;?& 'language-' +
 * myObject.getLang + '-support.py' %&gt;</li>
 * </ul>
 * <p>
 * A special container environment is created for scripts, with some useful
 * services. It is available to the script as a global variable named "script"
 * (this name can be changed via {@link #scriptVariableName}).
 * <p>
 * <ul>
 * <li><b>script.cacheDuration</b>: Setting this to something greater than 0
 * enables caching of the script results for a maximum number of milliseconds.
 * By default cacheDuration is 0, so that each request causes the script to be
 * evaluated. This class does not handle caching itself. Caching can be provided
 * by your environment if appropriate.</li>
 * <li><b>script.scriptEngine</b>: This is the {@link ScriptEngine} used by the
 * script. Scripts may use it to get information about the engine's
 * capabilities.</li>
 * <li><b>script.statics</b>: This {@link Map} provides a convenient location
 * for global values shared by all scripts, run by all engines. Note that it is
 * not thread safe! In anything but the most trivial application, you will need
 * to synchronize access to script.statics. For this reason, script.staticsLock
 * is provided (see below).</li>
 * <li><b>script.staticsLock</b>: A {@link ReadWriteLock} meant to be used for
 * the script.statics map, though exact use is up to your application. It can be
 * used to synchronize access to the statics across threads. Note that if more
 * locks are needed for your applications, they can be created and stored as
 * values within script.statics!</li>
 * </ul>
 * 
 * @author Tal Liron
 * @see ScriptStatics
 */
public class EmbeddedScript
{
	//
	// Static attributes
	//

	/**
	 * The start delimiter (first option).
	 */
	public static String delimiter1Start = "<%";

	/**
	 * The end delimiter (first option).
	 */
	public static String delimiter1End = "%>";

	/**
	 * The start delimiter (second option).
	 */
	public static String delimiter2Start = "<?";

	/**
	 * The end delimiter (second option).
	 */
	public static String delimiter2End = "?>";

	/**
	 * The addition to the start delimiter to specify an expression tag.
	 */
	public static String delimiterExpression = "=";

	/**
	 * The addition to the start delimiter to specify an include tag.
	 */
	public static String delimiterInclude = "&";

	/**
	 * The default variable name for the {@link Script} instance. Defaults to
	 * "script".
	 */
	public static String scriptVariableName = "script";

	/**
	 * The default variable name for the container. Used for include tags by
	 * implementations of {@link EmbeddedScriptParsingHelper}. Defaults to
	 * "container".
	 */
	public static String containerVariableName = "container";

	//
	// Types
	//

	/**
	 * This is the type of the "script" variable exposed to the script. The name
	 * is set according to {@link EmbeddedScript#scriptVariableName}.
	 */
	public static class Script
	{
		//
		// Attributes
		//

		/**
		 * Setting this to something greater than 0 enables caching of the
		 * script results for a maximum number of milliseconds. By default
		 * cacheDuration is 0, so that each request causes the script to be
		 * evaluated. This class does not handle caching itself. Caching can be
		 * provided by your environment if appropriate.
		 * 
		 * @return The cache duration in milliseconds
		 * @see #setCacheDuration(long)
		 * @see EmbeddedScript#cacheDuration
		 */
		public long getCacheDuration()
		{
			return cacheDuration.get();
		}

		/**
		 * @param cacheDuration
		 *        The cache duration in milliseconds
		 * @see #getCacheDuration()
		 */
		public void setCacheDuration( long cacheDuration )
		{
			this.cacheDuration.set( cacheDuration );
		}

		/**
		 * This is the {@link ScriptEngine} used by the script. Scripts may use
		 * it to get information about the engine's capabilities.
		 * 
		 * @return The script engine
		 */
		public ScriptEngine getScriptEngine()
		{
			return scriptEngine;
		}

		/**
		 * This {@link Map} provides a convenient location for global values
		 * shared by all scripts, run by all engines. Note that it is not thread
		 * safe! In anything but the most trivial application, you will need to
		 * synchronize access to script.statics. For this reason,
		 * script.staticsLock is provided.
		 * 
		 * @return The statics
		 * @see #getStaticsLock()
		 */
		public Map<String, Object> getStatics()
		{
			return ScriptStatics.statics;
		}

		/**
		 * A {@link ReadWriteLock} meant to be used for the script.statics map,
		 * though exact use is up to your application. It can be used to
		 * synchronize access to the statics across threads. Note that if more
		 * locks are needed for your applications, they can be created and
		 * stored as values within script.statics!
		 * 
		 * @return The statics lock
		 * @see #getStatics()
		 */
		public ReadWriteLock getStaticsLock()
		{
			return ScriptStatics.staticsLock;
		}

		// //////////////////////////////////////////////////////////////////////////
		// Private

		private final AtomicLong cacheDuration;

		private final ScriptEngine scriptEngine;

		private Script( AtomicLong cacheDuration, ScriptEngine scriptEngine )
		{
			this.cacheDuration = cacheDuration;
			this.scriptEngine = scriptEngine;
		}
	}

	/**
	 * A map of script engine names to their {@link EmbeddedScriptParsingHelper}
	 * . Note that embedded scripts will not work without the appropriate
	 * parsing helpers being installed.
	 * <p>
	 * This map is automatically initialized when this class loads according to
	 * resources named
	 * META-INF/services/com.threecrickets.scripturian.EmbeddedParsingHelper.
	 * Each resource is a simple text file with class names, one per line. Each
	 * class listed must implement the {@link EmbeddedScriptParsingHelper}
	 * interface and specify which engine names it supports via the
	 * {@link ScriptEngines} annotation.
	 * <p>
	 * You may also manipulate this map yourself, adding and removing helpers as
	 * necessary.
	 * <p>
	 * The default implementation of this library already contains a few useful
	 * parsing helpers, under the com.threecrickets.scripturian.helper package.
	 */
	public static Map<String, EmbeddedScriptParsingHelper> embeddedScriptParsingHelpers = new HashMap<String, EmbeddedScriptParsingHelper>();

	{
		// Initialize embeddedScriptParsingHelpers (look for them in META-INF)

		// For Java 6

		/*
		 * ServiceLoader<EmbeddedScriptParsingHelper> serviceLoader =
		 * ServiceLoader.load( EmbeddedScriptParsingHelper.class ); for(
		 * EmbeddedScriptParsingHelper embeddedScriptParsingHelper :
		 * serviceLoader ) { ScriptEngines scriptEngines =
		 * embeddedScriptParsingHelper.getClass().getAnnotation(
		 * ScriptEngines.class ); if( scriptEngines != null ) for( String
		 * scriptEngine : scriptEngines.value() )
		 * embeddedScriptParsingHelpers.put( scriptEngine,
		 * embeddedScriptParsingHelper ); }
		 */

		// For Java 5
		String resourceName = "META-INF/services/" + EmbeddedScriptParsingHelper.class.getCanonicalName();
		try
		{
			Enumeration<URL> resources = ClassLoader.getSystemResources( resourceName );
			while( resources.hasMoreElements() )
			{
				InputStream stream = resources.nextElement().openStream();
				BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
				String line = reader.readLine();
				while( line != null )
				{
					line = line.trim();
					if( ( line.length() > 0 ) && !line.startsWith( "#" ) )
					{
						EmbeddedScriptParsingHelper embeddedScriptParsingHelper = (EmbeddedScriptParsingHelper) Class.forName( line ).newInstance();
						ScriptEngines scriptEngines = embeddedScriptParsingHelper.getClass().getAnnotation( ScriptEngines.class );
						if( scriptEngines != null )
							for( String scriptEngine : scriptEngines.value() )
								embeddedScriptParsingHelpers.put( scriptEngine, embeddedScriptParsingHelper );
					}
					line = reader.readLine();
				}
				stream.close();
			}
		}
		catch( IOException x )
		{
			x.printStackTrace();
		}
		catch( InstantiationException x )
		{
			x.printStackTrace();
		}
		catch( IllegalAccessException x )
		{
			x.printStackTrace();
		}
		catch( ClassNotFoundException x )
		{
			x.printStackTrace();
		}
	}

	//
	// Construction
	//

	/**
	 * Parses a text stream containing plan text and embedded script segments
	 * into a compact, optimized script. Parsing requires the appropriate
	 * {@link EmbeddedScriptParsingHelper} implementations to be installed for
	 * the script engines.
	 * 
	 * @param text
	 *        The embedded script
	 * @param scriptEngineManager
	 *        The script engine manager used to create script engines
	 * @param defaultEngineName
	 *        If a script engine name isn't explicitly specified in the embedded
	 *        script file, this one will be used
	 * @param allowCompilation
	 *        Whether script segments will be compiled (note that compilation
	 *        will only happen if the script engine supports it, and that what
	 *        compilation exactly means is left up to the script engine)
	 * @throws ScriptException
	 *         In case of a parsing error
	 * @see EmbeddedScriptParsingHelper
	 */
	public EmbeddedScript( String text, ScriptEngineManager scriptEngineManager, String defaultEngineName, boolean allowCompilation ) throws ScriptException
	{
		this.scriptEngineManager = scriptEngineManager;
		String lastEngineName = defaultEngineName;

		String delimiterStart = null;
		String delimiterEnd = null;
		int delimiterStartLength = 0;
		int delimiterEndLength = 0;
		int expressionLength = delimiterExpression.length();
		int includeLength = delimiterInclude.length();

		// Detect type of delimiter
		int start = text.indexOf( delimiter1Start );
		if( start != -1 )
		{
			delimiterStart = delimiter1Start;
			delimiterEnd = delimiter1End;
			delimiterStartLength = delimiterStart.length();
			delimiterEndLength = delimiterEnd.length();
		}
		else
		{
			start = text.indexOf( delimiter2Start );
			if( start != -1 )
			{
				delimiterStart = delimiter2Start;
				delimiterEnd = delimiter2End;
				delimiterStartLength = delimiterStart.length();
				delimiterEndLength = delimiterEnd.length();
			}
		}

		// Parse segments
		if( start != -1 )
		{
			int last = 0;

			while( start != -1 )
			{
				// Add previous non-script segment
				if( start != last )
					segments.add( new Segment( text.substring( last, start ), false, lastEngineName ) );

				start += delimiterStartLength;

				int end = text.indexOf( delimiterEnd, start );
				if( end == -1 )
					throw new RuntimeException( "ScriptDescriptor block does not have an ending delimiter" );

				if( start + 1 != end )
				{
					String scriptEngineName = lastEngineName;

					boolean isExpression = false;
					boolean isInclude = false;
					// Check if this is an expression
					if( text.substring( start, start + expressionLength ).equals( delimiterExpression ) )
					{
						start += expressionLength;
						isExpression = true;
					}
					// Check if this is an include
					else if( text.substring( start, start + includeLength ).equals( delimiterInclude ) )
					{
						start += includeLength;
						isInclude = true;
					}

					// Get engine name if available
					if( !Character.isWhitespace( text.charAt( start ) ) )
					{
						int endEngineName = start + 1;
						while( !Character.isWhitespace( text.charAt( endEngineName ) ) )
							endEngineName++;

						scriptEngineName = text.substring( start, endEngineName );
						lastEngineName = scriptEngineName;

						start = endEngineName + 1;
					}

					if( start + 1 != end )
					{
						// Add script segment
						if( isExpression || isInclude )
						{
							ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );
							if( scriptEngine == null )
								throw new ScriptException( "Unsupported script engine: " + scriptEngineName );

							EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( scriptEngineName );
							if( embeddedScriptParsingHelper == null )
								throw new ScriptException( "Embedded script parsing helper not available for script engine: " + scriptEngineName );

							if( isExpression )
								segments.add( new Segment( embeddedScriptParsingHelper.getExpressionAsProgram( scriptEngine, text.substring( start, end ) ), true, scriptEngineName ) );
							else
								// if( isInclude )
								segments.add( new Segment( embeddedScriptParsingHelper.getExpressionAsInclude( scriptEngine, text.substring( start, end ) ), true, scriptEngineName ) );
						}
						else
							segments.add( new Segment( text.substring( start, end ), true, scriptEngineName ) );
					}
				}

				last = end + delimiterEndLength;
				start = text.indexOf( delimiterStart, last );
			}

			// Add remaining non-script segment
			if( last < text.length() )
				segments.add( new Segment( text.substring( last ), false, lastEngineName ) );
		}
		else
		{
			// Trivial file: does not include script
			segments.add( new Segment( text, false, lastEngineName ) );
		}

		// Collapse segments of same kind
		Segment previous = null;
		Segment current;
		for( Iterator<Segment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( previous != null )
			{
				if( previous.isScript == current.isScript )
				{
					if( current.scriptEngineName.equals( previous.scriptEngineName ) )
					{
						// Collapse current into previous
						i.remove();
						previous.text += current.text;
						current = previous;
					}
				}
			}

			previous = current;
		}

		// Collapse segments of same engine as scripts
		// (does not convert first segment into script if it isn't one -- that's
		// good)
		previous = null;
		for( Iterator<Segment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( ( previous != null ) && previous.isScript )
			{
				if( previous.scriptEngineName.equals( current.scriptEngineName ) )
				{
					// Collapse current into previous
					// (converting to script if necessary)
					i.remove();

					if( current.isScript )
						previous.text += current.text;
					else
					{
						ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( current.scriptEngineName );
						if( scriptEngine == null )
							throw new ScriptException( "Unsupported script engine: " + current.scriptEngineName );

						EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( current.scriptEngineName );
						if( embeddedScriptParsingHelper == null )
							throw new ScriptException( "Embedded script parsing helper not available for script engine: " + current.scriptEngineName );

						// ScriptEngineFactory factory =
						// scriptEngine.getFactory();
						// previous.text += factory.getProgram(
						// factory.getOutputStatement(
						// embeddedScriptParsingHelper.getTextAsScript(
						// scriptEngine,
						// current.text ) ) );

						previous.text += embeddedScriptParsingHelper.getTextAsProgram( scriptEngine, current.text );
					}

					current = previous;
				}
			}

			previous = current;
		}

		// Compiles segments if possible
		if( allowCompilation )
			for( Segment segment : segments )
			{
				if( segment.isScript )
				{
					ScriptEngine scriptEngine = scriptEngineManager.getEngineByName( segment.scriptEngineName );
					if( scriptEngine == null )
						throw new ScriptException( "Unsupported script engine: " + segment.scriptEngineName );

					EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( segment.scriptEngineName );
					if( embeddedScriptParsingHelper == null )
						throw new ScriptException( "Embedded script parsing helper not available for script engine: " + segment.scriptEngineName );

					// Add header
					String header = embeddedScriptParsingHelper.getScriptHeader( scriptEngine );
					if( header != null )
						segment.text = header + segment.text;

					// Add footer
					String footer = embeddedScriptParsingHelper.getScriptFooter( scriptEngine );
					if( footer != null )
						segment.text += footer;

					if( scriptEngine instanceof Compilable )
						segment.compiledScript = ( (Compilable) scriptEngine ).compile( segment.text );
				}
			}
	}

	//
	// Attributes
	//

	/**
	 * Timestamp of when the script last finished running successfully.
	 * 
	 * @return The timestamp
	 */
	public long getLastRun()
	{
		return lastRun;
	}

	/**
	 * The last {@link ScriptEngine} used in the last run of the script.
	 * 
	 * @return The script engine
	 */
	public ScriptEngine getLastScriptEngine()
	{
		return scriptEngine;
	}

	/**
	 * Setting this to something greater than 0 enables caching of the script
	 * results for a maximum number of milliseconds. By default cacheDuration is
	 * 0, so that each request causes the script to be evaluated. This class
	 * does not handle caching itself. Caching can be provided by your
	 * environment if appropriate.
	 * <p>
	 * This is the same instance provided for Script#getCacheDuration().
	 * 
	 * @return The cache duration in milliseconds
	 * @see #setCacheDuration(long)
	 */
	public long getCacheDuration()
	{
		return cacheDuration.get();
	}

	/**
	 * @param cacheDuration
	 *        The cache duration in milliseconds
	 * @see #getCacheDuration()
	 */
	public void setCacheDuration( long cacheDuration )
	{
		this.cacheDuration.set( cacheDuration );
	}

	/**
	 * Trivial embedded script objects have no embedded scripts, meaning that
	 * they are pure text. Identifying such scripts can save you from making
	 * unnecessary calls to
	 * {@link #run(Writer, Writer, Map, ScriptContextController, boolean)} in
	 * some situations.
	 * 
	 * @return The script content if it's trivial, null if not
	 */
	public String getTrivial()
	{
		if( segments.size() == 1 )
		{
			Segment sole = segments.get( 0 );
			if( !sole.isScript )
				return sole.text;
		}
		return null;
	}

	//
	// Operations
	//

	/**
	 * Runs the script. Optionally supports checking for output caching, by
	 * testing {@link #cacheDuration} versus the value of {@link #getLastRun()}.
	 * If checking the cache is enabled, this method will return false to
	 * signify that the script did not run and the cached output should be used
	 * instead. In such cases, it is up to your running environment to interpret
	 * this accordingly if you wish to support caching.
	 * 
	 * @param writer
	 *        Standard output
	 * @param errorWriter
	 *        Standard error output
	 * @param scriptEngines
	 *        A cache of script engines by engine name
	 * @param scriptContextController
	 *        An optional {@link ScriptContextController} to be applied to the
	 *        script context
	 * @param checkCache
	 *        Whether or not to check for caching versus the value of
	 *        {@link #getLastRun()}
	 * @return True if the script ran, false if it didn't run, because the
	 *         cached output is expected to be used instead
	 * @throws ScriptException
	 */
	public boolean run( Writer writer, Writer errorWriter, Map<String, ScriptEngine> scriptEngines, ScriptContextController scriptContextController, boolean checkCache ) throws ScriptException, IOException
	{
		long now = System.currentTimeMillis();
		if( checkCache && ( now - lastRun < cacheDuration.get() ) )
		{
			// We didn't run this time
			return false;
		}
		else
		{
			for( Segment segment : segments )
			{
				if( !segment.isScript )
				{
					writer.write( segment.text );
				}
				else
				{
					ScriptContext scriptContext;
					scriptEngineName = segment.scriptEngineName;
					scriptEngine = scriptEngines.get( scriptEngineName );
					if( scriptEngine == null )
					{
						scriptEngine = scriptEngineManager.getEngineByName( scriptEngineName );

						if( scriptEngine == null )
							throw new ScriptException( "Unsupported script engine: " + scriptEngineName );

						// We absolutely need a new script context here!
						// Otherwise, we might end up using a context
						// already in use by another thread.
						// (Also, note that some script engines do not even
						// provide a default context -- Jepp, for example -- so
						// it's generally a good idea to explicitly create one)
						scriptContext = new SimpleScriptContext();
						scriptEngine.setContext( scriptContext );

						if( scriptContext.getBindings( ScriptContext.ENGINE_SCOPE ) == null )
							scriptContext.setBindings( scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE );
						if( scriptContext.getBindings( ScriptContext.GLOBAL_SCOPE ) == null )
							scriptContext.setBindings( scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE );

						scriptEngines.put( scriptEngineName, scriptEngine );
					}
					else
					{
						scriptContext = scriptEngine.getContext();
					}

					Object oldScript = scriptContext.getAttribute( scriptVariableName, ScriptContext.ENGINE_SCOPE );
					scriptContext.setAttribute( scriptVariableName, new Script( cacheDuration, scriptEngine ), ScriptContext.ENGINE_SCOPE );

					// Note that some script engines (such as Rhino) expect a
					// PrintWriter, even though the spec defines just a Writer
					scriptContext.setWriter( new PrintWriter( writer ) );
					scriptContext.setErrorWriter( new PrintWriter( errorWriter ) );

					if( scriptContextController != null )
						scriptContextController.initialize( scriptContext );

					try
					{
						if( segment.compiledScript != null )
						{
							segment.compiledScript.eval( scriptContext );
						}
						else
						{
							// Note that we are wrapping our text with a
							// StringReader. Why? Because some implementations
							// of javax.script (notably Jepp) interpret the
							// String version of eval to mean only one line of
							// code.
							scriptEngine.eval( new StringReader( segment.text ), scriptContext );
						}
					}
					catch( ScriptException x )
					{
						throw x;
					}
					catch( Exception x )
					{
						// Some script engines (notably Quercus) throw their own
						// special exceptions
						throw new ScriptException( x );
					}
					finally
					{
						if( scriptContextController != null )
							scriptContextController.finalize( scriptContext );

						// Restore old script value (this is desirable for
						// scripts that run other scripts)
						if( oldScript != null )
							scriptContext.setAttribute( scriptVariableName, oldScript, ScriptContext.ENGINE_SCOPE );
					}
				}
			}

			lastRun = now;
			return true;
		}
	}

	/**
	 * Calls an entry point in the script: a function, method, closure, etc.,
	 * according to how the scripting engine and its language handles
	 * invocations. If not, then this method requires the appropriate
	 * {@link EmbeddedScriptParsingHelper} implementation to be installed for
	 * the script engine. Most likely, the script engine supports the
	 * {@link Invocable} interface. Running the script first (via
	 * {@link #run(Writer, Writer, Map, ScriptContextController, boolean)} ) is
	 * not absolutely required, but probably will be necessary in most useful
	 * scenarios, where running the script causes useful entry point to be
	 * defined.
	 * <p>
	 * Note that this call does not support sending arguments. If you need to
	 * pass data to the script, use a global variable, which you can set via the
	 * optional {@link ScriptContextController}.
	 * 
	 * @param entryPointName
	 *        The name of the entry point
	 * @param scriptContextController
	 *        An optional {@link ScriptContextController} to be applied to the
	 *        script context
	 * @return The value returned by the script call
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 *         Note that this exception will only be thrown if the script engine
	 *         supports the {@link Invocable} interface; otherwise a
	 *         {@link ScriptException} if the method is not found
	 */
	public Object invoke( String entryPointName, ScriptContextController scriptContextController ) throws ScriptException, NoSuchMethodException
	{
		ScriptContext scriptContext = scriptEngine.getContext();

		Object oldScript = scriptContext.getAttribute( scriptVariableName, ScriptContext.ENGINE_SCOPE );
		scriptContext.setAttribute( scriptVariableName, new Script( cacheDuration, scriptEngine ), ScriptContext.ENGINE_SCOPE );

		if( scriptContextController != null )
			scriptContextController.initialize( scriptContext );

		try
		{
			EmbeddedScriptParsingHelper embeddedScriptParsingHelper = embeddedScriptParsingHelpers.get( scriptEngineName );

			if( embeddedScriptParsingHelper == null )
				throw new ScriptException( "Embedded script parsing helper not available for script engine: " + scriptEngineName );

			String program = embeddedScriptParsingHelper.getInvocationAsProgram( scriptEngine, entryPointName );
			if( program == null )
			{
				if( scriptEngine instanceof Invocable )
					return ( (Invocable) scriptEngine ).invokeFunction( entryPointName );
				else
					throw new ScriptException( "Script engine does not support invocations" );
			}
			else
				return scriptEngine.eval( program, scriptContext );
		}
		finally
		{
			if( scriptContextController != null )
				scriptContextController.finalize( scriptContext );

			// Restore old script value (this is desirable for scripts that run
			// other scripts)
			if( oldScript != null )
				scriptContext.setAttribute( scriptVariableName, oldScript, ScriptContext.ENGINE_SCOPE );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final List<Segment> segments = new LinkedList<Segment>();

	private final ScriptEngineManager scriptEngineManager;

	private final AtomicLong cacheDuration = new AtomicLong();

	private long lastRun = 0;

	private ScriptEngine scriptEngine;

	private String scriptEngineName;

	private static class Segment
	{
		public Segment( String text, boolean isScript, String scriptEngineName )
		{
			this.text = text;
			this.isScript = isScript;
			this.scriptEngineName = scriptEngineName;
		}

		public String text;

		public CompiledScript compiledScript;

		public boolean isScript;

		public String scriptEngineName;
	}
}
