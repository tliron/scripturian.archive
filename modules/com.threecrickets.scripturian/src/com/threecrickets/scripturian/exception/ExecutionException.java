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

package com.threecrickets.scripturian.exception;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Tal Liron
 */
public class ExecutionException extends Exception
{
	//
	// Construction
	//

	public ExecutionException( String documentName, int lineNumber, int columnNumber, String message )
	{
		super( message );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ExecutionException( String documentName, int lineNumber, int columnNumber, String message, Throwable cause )
	{
		super( message, cause );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ExecutionException( String documentName, int lineNumber, int columnNumber, Throwable cause )
	{
		super( cause );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ExecutionException( String documentName, String message, Throwable cause )
	{
		super( message, cause );
		stack.add( new StackFrame( documentName ) );
	}

	public ExecutionException( String documentName, String message )
	{
		super( message );
		stack.add( new StackFrame( documentName ) );
	}

	public ExecutionException( String message, Throwable cause )
	{
		super( message, cause );
	}

	//
	// Attributes
	//

	public Collection<StackFrame> getStack()
	{
		return stack;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	private final ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
}
