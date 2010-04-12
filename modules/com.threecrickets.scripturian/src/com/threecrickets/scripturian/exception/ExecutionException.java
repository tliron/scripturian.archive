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

import javax.script.ScriptException;

import com.threecrickets.scripturian.LanguageAdapter;
import com.threecrickets.scripturian.LanguageManager;

/**
 * @author Tal Liron
 */
public class ExecutionException extends Exception
{
	//
	// Static operations
	//

	public static ExecutionException create( String documentName, LanguageManager manager, Throwable throwable )
	{
		Throwable wrapped = throwable;
		while( wrapped != null )
		{
			if( wrapped instanceof ExecutionException )
				return (ExecutionException) wrapped;

			// Try helpers
			Throwable causeOrDocumentRunException = null;
			for( LanguageAdapter adapter : manager.getAdapters() )
			{
				causeOrDocumentRunException = adapter.getCauseOrExecutionException( documentName, wrapped );
				if( causeOrDocumentRunException != null )
					break;
			}

			if( causeOrDocumentRunException != null )
			{
				// Found it!
				if( causeOrDocumentRunException instanceof ExecutionException )
					return (ExecutionException) causeOrDocumentRunException;

				// We are unwrapped
				wrapped = causeOrDocumentRunException;
				continue;
			}

			// Unwrap
			wrapped = wrapped.getCause();
		}

		if( throwable instanceof ScriptException )
			// Extract from ScriptException
			return new ExecutionException( documentName, (ScriptException) throwable );
		else
			// Unknown
			return new ExecutionException( documentName, throwable.getMessage(), throwable );
	}

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

	public ExecutionException( String message, StackFrame... stackFrames )
	{
		super( message );
		for( StackFrame stackFrame : stackFrames )
			stack.add( stackFrame );
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

	public ExecutionException( String documentName, ScriptException scriptException )
	{
		super( scriptException.getMessage(), scriptException.getCause() );
		stack.add( new StackFrame( documentName, scriptException.getLineNumber(), scriptException.getColumnNumber() ) );
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
