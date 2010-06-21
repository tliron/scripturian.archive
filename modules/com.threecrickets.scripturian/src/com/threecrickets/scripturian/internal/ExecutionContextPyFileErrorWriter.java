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

import org.python.core.PyFileWriter;

import com.threecrickets.scripturian.ExecutionContext;

/**
 * Like {@link ExecutionContextPyFileWriter}, but for the error writer.
 * 
 * @author Tal Liron
 */
public class ExecutionContextPyFileErrorWriter extends ExecutionContextPyFileWriter
{
	//
	// Constants
	//

	public static final String WRITER = "com.threecrickets.scripturian.internal.ExecutionContextPyFileErrorWriter.writer";

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	//
	// ExecutionContextPyFileWriter
	//

	@Override
	protected PyFileWriter getWriter()
	{
		ExecutionContext executionContext = ExecutionContext.getCurrent();
		PyFileWriter writer = (PyFileWriter) executionContext.getAttributes().get( WRITER );
		if( writer == null )
		{
			writer = new PyFileWriter( executionContext.getErrorWriterOrDefault() );
			executionContext.getAttributes().put( WRITER, writer );
		}
		return writer;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
