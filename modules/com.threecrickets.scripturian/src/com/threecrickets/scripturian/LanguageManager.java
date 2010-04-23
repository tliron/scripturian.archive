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

package com.threecrickets.scripturian;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ServiceLoader;

/**
 * Provides access to {@link LanguageAdapter} instances.
 * <p>
 * Instances of this class are safe for concurrent access.
 * 
 * @author Tal Liron
 * @see LanguageAdapter
 */
public class LanguageManager
{
	//
	// Construction
	//

	/**
	 * Adds all language adapters found in the {@code
	 * META-INF/services/com.threecrickets.scripturian.LanguageAdapter}
	 * resource.
	 * 
	 * @see ServiceLoader
	 */
	public LanguageManager()
	{
		// Initialize adapters
		ServiceLoader<LanguageAdapter> adapterLoader = ServiceLoader.load( LanguageAdapter.class );
		for( LanguageAdapter adapter : adapterLoader )
		{
			try
			{
				addAdapter( adapter );
			}
			catch( Throwable x )
			{
				throw new RuntimeException( "Could not initialize " + adapter, x );
			}
		}
	}

	//
	// Attributes
	//

	/**
	 * General-purpose attributes for this language manager. Language adapters
	 * can use this for sharing state with other language adapters.
	 * 
	 * @return The attributes
	 */
	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	/**
	 * All language adapters. Note that this set is unmodifiable. To add an
	 * adapter use {@link #addAdapter(LanguageAdapter)}
	 * 
	 * @return The adapters
	 */
	public Set<LanguageAdapter> getAdapters()
	{
		return Collections.unmodifiableSet( languageAdapters );
	}

	/**
	 * A language adapter for a scriptlet tag.
	 * 
	 * @param tag
	 *        The scriptlet adapter
	 * @return The language adapter or null if not found
	 * @throws ParsingException
	 */
	public LanguageAdapter getAdapterByTag( String tag ) throws ParsingException
	{
		if( tag == null )
			return null;
		return languageAdapterByTag.get( tag );
	}

	/**
	 * A language adapter for a document name according to its filename
	 * extension.
	 * 
	 * @param documentName
	 *        The document name
	 * @param defaultExtension
	 *        The default extension to assume in case the document name does not
	 *        have one
	 * @return The language adapter or null if not found
	 * @throws ParsingException
	 */
	public LanguageAdapter getAdapterByExtension( String documentName, String defaultExtension ) throws ParsingException
	{
		int slash = documentName.lastIndexOf( '/' );
		if( slash != -1 )
			documentName = documentName.substring( slash + 1 );

		int dot = documentName.lastIndexOf( '.' );
		String extension = dot != -1 ? documentName.substring( dot + 1 ) : defaultExtension;
		if( extension == null )
			throw new ParsingException( documentName, -1, -1, "Name must have an extension" );

		return languageAdapterByExtension.get( extension );
	}

	/**
	 * A language adapter for a document name according to its filename
	 * extension.
	 * 
	 * @param documentName
	 *        The document name
	 * @param defaultExtension
	 *        The default extension to assume in case the document name does not
	 *        have one
	 * @param defaultTag
	 *        The language tag to use in case a language adapter wasn't found
	 *        according to the extension
	 * @return The default language adapter tag or null if not found
	 * @throws ParsingException
	 */
	public String getLanguageTagByExtension( String documentName, String defaultExtension, String defaultTag ) throws ParsingException
	{
		LanguageAdapter languageAdapter = getAdapterByExtension( documentName, defaultExtension );
		if( languageAdapter == null )
			languageAdapter = getAdapterByTag( defaultTag );
		if( languageAdapter != null )
			return (String) languageAdapter.getAttributes().get( LanguageAdapter.DEFAULT_TAG );
		else
			return null;
	}

	//
	// Operations
	//

	/**
	 * Adds a language adapter to this manager.
	 * 
	 * @param adapter
	 *        The language adapter
	 */
	@SuppressWarnings("unchecked")
	public void addAdapter( LanguageAdapter adapter )
	{
		languageAdapters.add( adapter );

		Iterable<String> tags = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.TAGS );
		for( String tag : tags )
			languageAdapterByTag.put( tag, adapter );

		Iterable<String> extensions = (Iterable<String>) adapter.getAttributes().get( LanguageAdapter.EXTENSIONS );
		for( String extension : extensions )
			languageAdapterByExtension.put( extension, adapter );

	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * General-purpose attributes for this language manager.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * The language adapters.
	 */
	private final CopyOnWriteArraySet<LanguageAdapter> languageAdapters = new CopyOnWriteArraySet<LanguageAdapter>();

	/**
	 * A map of language tags to their {@link LanguageAdapter} instances.
	 */
	private final ConcurrentMap<String, LanguageAdapter> languageAdapterByTag = new ConcurrentHashMap<String, LanguageAdapter>();

	/**
	 * A map of filename extensions to their {@link LanguageAdapter} instances.
	 */
	private final ConcurrentMap<String, LanguageAdapter> languageAdapterByExtension = new ConcurrentHashMap<String, LanguageAdapter>();
}
