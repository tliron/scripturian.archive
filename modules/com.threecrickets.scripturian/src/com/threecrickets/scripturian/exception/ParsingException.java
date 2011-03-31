/**
 * Copyright 2009-2011 Three Crickets LLC.
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
import java.util.List;

import com.threecrickets.scripturian.Executable;

/**
 * A parsing exception. Can occur during the construction phase of an
 * executable.
 * 
 * @author Tal Liron
 * @see Executable
 */
public class ParsingException extends Exception
{
	//
	// Static operations
	//

	public static ParsingException adapterNotFound( String documentName, int lineNumber, int columnNumber, String languageTag )
	{
		return new ParsingException( documentName, lineNumber, columnNumber, "Adapter not available for language: " + languageTag );
	}

	public static ParsingException pluginNotFound( String documentName, int lineNumber, int columnNumber, String languageTag )
	{
		return new ParsingException( documentName, lineNumber, columnNumber, "Plugin not available for code: " + languageTag );
	}

	//
	// Construction
	//

	public ParsingException( String documentName, int lineNumber, int columnNumber, String message )
	{
		super( message );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ParsingException( String documentName, int lineNumber, int columnNumber, String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ParsingException( String documentName, int lineNumber, int columnNumber, Throwable cause )
	{
		super( cause );
		stack.add( new StackFrame( documentName, lineNumber, columnNumber ) );
	}

	public ParsingException( String documentName, String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
		stack.add( new StackFrame( documentName ) );
	}

	public ParsingException( String documentName, String message )
	{
		super( message );
		stack.add( new StackFrame( documentName ) );
	}

	public ParsingException( String message, Throwable cause )
	{
		super( message != null ? message : cause.getClass().getName(), cause );
	}

	public ParsingException( String message )
	{
		super( message );
	}

	public ParsingException( Throwable cause )
	{
		super( cause );
	}

	//
	// Attributes
	//

	/**
	 * The call stack.
	 * 
	 * @return The call stack
	 */
	public List<StackFrame> getStack()
	{
		return stack;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;

	/**
	 * The call stack.
	 */
	private final ArrayList<StackFrame> stack = new ArrayList<StackFrame>();
}
