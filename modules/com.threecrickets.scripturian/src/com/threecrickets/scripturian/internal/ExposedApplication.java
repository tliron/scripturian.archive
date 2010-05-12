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

package com.threecrickets.scripturian.internal;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.threecrickets.scripturian.Main;

/**
 * This is the <code>application</code> exposed by {@link Main}.
 * 
 * @author Tal Liron
 * @see Main
 */
public class ExposedApplication
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param main
	 *        The main instance
	 */
	public ExposedApplication( Main main )
	{
		this.main = main;
	}

	//
	// Attributes
	//

	/**
	 * An array of the string arguments sent to {@link Main#main(String[])}.
	 * 
	 * @return The arguments
	 */
	public String[] getArguments()
	{
		return main.getArguments();
	}

	/**
	 * A map of all values global to the current VM.
	 * 
	 * @return The globals
	 */
	public ConcurrentMap<String, Object> getGlobals()
	{
		return GlobalScope.getInstance().getAttributes();
	}

	/**
	 * Gets a value global to the current VM.
	 * 
	 * @param name
	 *        The name of the global
	 * @return The global's current value
	 */
	public Object getGlobal( String name )
	{
		return getGlobal( name, null );
	}

	/**
	 * Gets a value global to the current VM, atomically setting it to a default
	 * value if it doesn't exist.
	 * 
	 * @param name
	 *        The name of the global
	 * @param defaultValue
	 *        The default value
	 * @return The global's current value
	 */
	public Object getGlobal( String name, Object defaultValue )
	{
		ConcurrentMap<String, Object> globals = getGlobals();
		Object value = globals.get( name );
		if( ( value == null ) && ( defaultValue != null ) )
		{
			value = defaultValue;
			Object existing = globals.putIfAbsent( name, value );
			if( existing != null )
				value = existing;
		}
		return value;
	}

	/**
	 * Sets the value global to the current VM.
	 * 
	 * @param name
	 *        The name of the global
	 * @param value
	 *        The global's new value
	 * @return The global's previous value
	 */
	public Object setGlobal( String name, Object value )
	{
		ConcurrentMap<String, Object> globals = getGlobals();
		return globals.put( name, value );
	}

	/**
	 * The logger.
	 * 
	 * @return The logger
	 * @see #setLogger(Logger)
	 */
	public Logger getLogger()
	{
		return main.getLogger();
	}

	/**
	 * @param logger
	 *        The logger
	 * @see #getLogger()
	 */
	public void setLogger( Logger logger )
	{
		main.setLogger( logger );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The main instance.
	 */
	private final Main main;
}
