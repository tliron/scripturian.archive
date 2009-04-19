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

package com.threecrickets.scripturian.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used as global space for sharing objects between scripts. Statics will stay
 * alive for the duration of the Java virtual machine. Any object can be stored
 * here.
 * 
 * @author Tal Liron
 */
public class MetaScope
{
	//
	// Static attributes
	//

	public static MetaScope getInstance()
	{
		return instance;
	}

	/**
	 * Any object can be stored here.
	 */
	public ConcurrentMap<String, Object> getValues()
	{
		return values;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private final static MetaScope instance = new MetaScope();

	private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<String, Object>();

	private MetaScope()
	{
	}
}
