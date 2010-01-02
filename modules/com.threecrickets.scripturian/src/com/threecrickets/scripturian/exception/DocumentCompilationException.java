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

package com.threecrickets.scripturian.exception;

import javax.script.ScriptException;

/**
 * @author Tal Liron
 */
public class DocumentCompilationException extends DocumentInitializationException
{
	//
	// Construction
	//

	public DocumentCompilationException( String documentName, String message, ScriptException x )
	{
		super( documentName, message, x );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private static final long serialVersionUID = 1L;
}
