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

import java.beans.IntrospectionException;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.LanguageManager;
import com.threecrickets.scripturian.adapter.SuccinctAdapter;
import com.threecrickets.succinct.CastException;
import com.threecrickets.succinct.Filler;
import com.threecrickets.succinct.filler.BeanFillerWrapper;
import com.threecrickets.succinct.filler.BeanFillerWrappingIterable;

/**
 * A {@link Filler} that supports Sciprturian inclusion scriptlets and getting
 * values from exposed variables via the Java bean mechanism.
 * 
 * @author Tal Liron
 */
public class ScripturianSuccinctFiller implements Filler
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param manager
	 *        The language manager
	 * @param executable
	 *        The executable
	 * @param executionContext
	 *        The execution context
	 */
	public ScripturianSuccinctFiller( LanguageManager manager, Executable executable, ExecutionContext executionContext )
	{
		this.manager = manager;
		this.executable = executable;
		this.executionContext = executionContext;
	}

	//
	// Filler
	//

	public Object getValue( String key ) throws CastException
	{
		if( key.startsWith( SuccinctAdapter.INCLUSION_KEY ) )
		{
			String documentName = key.substring( SuccinctAdapter.INCLUSION_KEY.length() );

			// We don't need to return a value, because include already writes
			// to our writer
			ScripturianUtil.containerInclude( manager, executable, executionContext, documentName );

			// We need to include something in order to be considered cast
			return "";
		}
		else
		{
			String[] split = key.split( "\\." );
			if( split.length > 0 )
			{
				String ourKey = split[0];
				Object value = executionContext.getExposedVariables().get( ourKey );
				if( split.length == 1 )
					// It's us!
					return value;
				else if( value != null )
				{
					// Delegate to property
					try
					{
						return new BeanFillerWrapper( ourKey, value ).getValue( key );
					}
					catch( IntrospectionException x )
					{
						throw new CastException( x, key );
					}
				}
			}
		}

		throw new CastException( key );
	}

	public Iterable<? extends Filler> getFillers( String iteratorKey ) throws CastException
	{
		Object value = executionContext.getExposedVariables().get( iteratorKey );
		if( value instanceof Iterable<?> )
			return new BeanFillerWrappingIterable( iteratorKey, (Iterable<?>) value );
		else
			throw new CastException( iteratorKey );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The language manager.
	 */
	private final LanguageManager manager;

	/**
	 * The executable.
	 */
	private final Executable executable;

	/**
	 * The exection context.
	 */
	private final ExecutionContext executionContext;
}
