/**
 * Copyright 2009 Three Crickets.
 * <p>
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * <p>
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * <p>
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * <p>
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * <p>
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * <p>
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * <p>
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly from Three Crickets
 * at http://www.threecrickets.com/
 */

package com.threecrickets.scripturian.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

/**
 * Utility methods.
 * 
 * @author Tal Liron
 */
public abstract class ScripturianUtil
{
	/**
	 * Size (in bytes) of the buffer used by {@link #getString(File)}.
	 */
	public static final int BUFFFER_SIZE = 1024 * 1024;

	/**
	 * Reads a reader into a string.
	 * 
	 * @param reader
	 *        The reader
	 * @return The string read from the reader
	 * @throws IOException
	 */
	public static String getString( Reader reader ) throws IOException
	{
		StringWriter writer = new StringWriter();
		int c;
		while( true )
		{
			c = reader.read();
			if( c == -1 )
				break;

			writer.write( c );
		}

		return writer.toString();
	}

	/**
	 * Reads a file into a string. Uses a buffer (see {@link #BUFFFER_SIZE}).
	 * 
	 * @param file
	 *        The file
	 * @return The string read from the file
	 * @throws IOException
	 */
	public static String getString( File file ) throws IOException
	{
		BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ) ), BUFFFER_SIZE );
		return getString( reader );
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	private ScripturianUtil()
	{
	}
}
