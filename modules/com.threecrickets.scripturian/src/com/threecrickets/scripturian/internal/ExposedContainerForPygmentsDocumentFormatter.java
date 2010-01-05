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

import com.threecrickets.scripturian.formatter.PygmentsDocumentFormatter;

/**
 * @author Tal Liron
 * @see PygmentsDocumentFormatter
 */
public class ExposedContainerForPygmentsDocumentFormatter
{
	//
	// Construction
	//

	public ExposedContainerForPygmentsDocumentFormatter( String text, int lineNumber, String language, String title, String style, String background, String highlight )
	{
		this.text = text;
		this.lineNumber = lineNumber;
		this.language = language;
		this.title = title;
		this.style = style;
		this.background = background;
		this.highlight = highlight;
	}

	//
	// Attributes
	//

	public String getText()
	{
		return text;
	}

	public void setText( String text )
	{
		this.text = text;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	public String getLanguage()
	{
		return language;
	}

	public String getTitle()
	{
		return title;
	}

	public String getStyle()
	{
		return style;
	}

	public String getBackground()
	{
		return background;
	}

	public String getHighlight()
	{
		return highlight;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private String text;

	private final int lineNumber;

	private final String language;

	private final String title;

	private final String style;

	private final String background;

	private final String highlight;
}