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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentFileSource;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.DocumentNotFoundException;

/**
 * Document descriptor for {@link DocumentFileSource}.
 * 
 * @author Tal Liron
 */
public class FiledDocumentDescriptor<D> implements DocumentDescriptor<D>
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param documentSource
	 *        The document source
	 * @param defaultName
	 *        The default name to use for directories
	 * @param sourceCode
	 *        The source code
	 * @param tag
	 *        The descriptor tag
	 * @param document
	 *        The document
	 */
	public FiledDocumentDescriptor( DocumentFileSource<D> documentSource, String defaultName, String sourceCode, String tag, D document )
	{
		this.documentSource = documentSource;
		this.defaultName = defaultName;
		file = null;
		timestamp = System.currentTimeMillis();
		this.sourceCode = sourceCode;
		this.tag = tag;
		this.document = document;
	}

	/**
	 * Construction.
	 * 
	 * @param documentSource
	 *        The document source
	 * @param file
	 *        The file
	 * @throws DocumentException
	 */
	public FiledDocumentDescriptor( DocumentFileSource<D> documentSource, File file ) throws DocumentException
	{
		this.documentSource = documentSource;
		this.defaultName = documentSource.getRelativeFilePath( file );
		this.file = file;
		timestamp = file.lastModified();
		try
		{
			sourceCode = ScripturianUtil.getString( file );
		}
		catch( FileNotFoundException x )
		{
			throw new DocumentNotFoundException( "Could not find file " + file, x );
		}
		catch( IOException x )
		{
			throw new DocumentException( "Could not read file " + file, x );
		}
		tag = ScripturianUtil.getExtension( file );
	}

	//
	// Attributes
	//

	/**
	 * The file, or null if we're an in-memory document.
	 */
	public final File file;

	/**
	 * Whether the document is valid. Calling this method will sometimes cause a
	 * validity check.
	 * <p>
	 * Note that the validity check will cascade to document we depend on.
	 * 
	 * @return Whether the document is valid
	 * @see DocumentFileSource#getMinimumTimeBetweenValidityChecks()
	 * @see DocumentDescriptor#getDependencies()
	 */
	public boolean isValid()
	{
		// Check cached validity
		Boolean validity = this.validity;
		if( validity != null )
			return validity;

		// If any of our dependencies is invalid, then so are we
		for( String documentName : dependencies )
		{
			FiledDocumentDescriptor<D> filedDocumentDescriptor = documentSource.getCachedDocumentDescriptor( documentName );
			if( ( filedDocumentDescriptor != null ) && !filedDocumentDescriptor.isValid() )
			{
				this.validity = false;
				return false;
			}
		}

		// Always valid if in-memory document
		if( file == null )
		{
			this.validity = true;
			return true;
		}

		long minimumTimeBetweenValidityChecks = documentSource.getMinimumTimeBetweenValidityChecks();

		// -1 means don't check for validity
		if( minimumTimeBetweenValidityChecks == -1 )
		{
			// Valid, for now (might not be later)
			return true;
		}

		long now = System.currentTimeMillis();

		// Are we in the threshold for checking for validity?
		if( ( now - lastValidityCheckTimestamp ) > minimumTimeBetweenValidityChecks )
		{
			// Check for validity
			lastValidityCheckTimestamp = now;
			boolean isValid = file.lastModified() <= timestamp;
			if( isValid )
			{
				// Valid, for now (might not be later)
				return true;
			}
			else
			{
				// Invalid
				this.validity = false;
				return false;
			}
		}
		else
		{
			// Valid, for now (might not be later)
			return true;
		}
	}

	//
	// DocumentDescriptor
	//

	public String getDefaultName()
	{
		return defaultName;
	}

	public String getSourceCode()
	{
		return sourceCode;
	}

	public String getTag()
	{
		return tag;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public D getDocument()
	{
		documentLock.readLock().lock();
		try
		{
			return document;
		}
		finally
		{
			documentLock.readLock().unlock();
		}
	}

	public D setDocument( D document )
	{
		documentLock.writeLock().lock();
		try
		{
			D last = this.document;
			this.document = document;
			return last;
		}
		finally
		{
			documentLock.writeLock().unlock();
		}
	}

	public D setDocumentIfAbsent( D document )
	{
		documentLock.readLock().lock();
		try
		{
			if( this.document != null )
				return this.document;

			documentLock.readLock().unlock();
			// (Might change here!)
			documentLock.writeLock().lock();
			try
			{
				if( this.document != null )
					return this.document;

				D last = this.document;
				this.document = document;
				return last;
			}
			finally
			{
				documentLock.writeLock().unlock();
				documentLock.readLock().lock();
			}
		}
		finally
		{
			documentLock.readLock().unlock();
		}
	}

	public DocumentSource<D> getSource()
	{
		return documentSource;
	}

	public Set<String> getDependencies()
	{
		return dependencies;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The document source.
	 */
	private final DocumentFileSource<D> documentSource;

	/**
	 * Lock for access to {@link #document}.
	 */
	private final ReadWriteLock documentLock = new ReentrantReadWriteLock();

	/**
	 * Dependencies.
	 */
	private final Set<String> dependencies = new CopyOnWriteArraySet<String>();

	/**
	 * The document.
	 * 
	 * @see #documentLock
	 */
	private D document;

	/**
	 * The default name.
	 */
	private final String defaultName;

	/**
	 * The timestamp or -1.
	 */
	private final long timestamp;

	/**
	 * The document source code.
	 */
	private final String sourceCode;

	/**
	 * The document tag.
	 */
	private final String tag;

	/**
	 * The timestamp of the last validity check.
	 */
	private volatile long lastValidityCheckTimestamp;

	/**
	 * Cached validity.
	 */
	private volatile Boolean validity;
}