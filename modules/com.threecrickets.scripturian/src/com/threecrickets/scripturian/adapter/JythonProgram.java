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

package com.threecrickets.scripturian.adapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import org.python.antlr.base.mod;
import org.python.core.BytecodeLoader;
import org.python.core.ParserFacade;
import org.python.core.PyCode;
import org.python.core.PythonCodeBundle;
import org.python.util.PythonInterpreter;

import com.threecrickets.scripturian.Executable;
import com.threecrickets.scripturian.ExecutionContext;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.exception.PreparationException;
import com.threecrickets.scripturian.internal.ScripturianUtil;

/**
 * @author Tal Liron
 */
class JythonProgram extends ProgramBase<JythonAdapter>
{
	//
	// Construction
	//

	/**
	 * Construction.
	 * 
	 * @param sourceCode
	 *        The source code
	 * @param isScriptlet
	 *        Whether the source code is a scriptlet
	 * @param position
	 *        The program's position in the executable
	 * @param startLineNumber
	 *        The line number in the document for where the program's source
	 *        code begins
	 * @param startColumnNumber
	 *        The column number in the document for where the program's source
	 *        code begins
	 * @param executable
	 *        The executable
	 * @param adapter
	 *        The language adapter
	 */
	public JythonProgram( String sourceCode, boolean isScriptlet, int position, int startLineNumber, int startColumnNumber, Executable executable, JythonAdapter adapter )
	{
		super( sourceCode, isScriptlet, position, startLineNumber, startColumnNumber, executable, adapter );
	}

	//
	// Program
	//

	@Override
	public void prepare() throws PreparationException
	{
		if( pythonCodeReference.get() != null )
			return;

		File classFile = ScripturianUtil.getFileForProgramClass( adapter.getCacheDir(), executable, position );
		String classname = ScripturianUtil.getClassnameForProgram( executable, position );

		synchronized( classFile )
		{
			if( classFile.exists() )
			{
				// Use cached compiled code
				try
				{
					byte[] classByteArray = ScripturianUtil.getBytes( classFile );
					pythonCodeReference.compareAndSet( null, BytecodeLoader.makeCode( classname, classByteArray, executable.getDocumentName() ) );
				}
				catch( IOException x )
				{
					x.printStackTrace();
				}
				catch( Exception x )
				{
					x.printStackTrace();
				}
			}
			else
			{
				mod node = ParserFacade.parseExpressionOrModule( new StringReader( sourceCode ), executable.getDocumentName(), adapter.compilerFlags );
				try
				{
					PythonCodeBundle bundle = adapter.compiler.compile( node, classname, executable.getDocumentName(), true, false, adapter.compilerFlags );
					pythonCodeReference.compareAndSet( null, bundle.loadCode() );

					// Cache it!
					classFile.getParentFile().mkdirs();
					FileOutputStream stream = new FileOutputStream( classFile );
					try
					{
						bundle.writeTo( stream );
					}
					finally
					{
						stream.close();
					}
				}
				catch( Exception x )
				{
					x.printStackTrace();
				}
			}
		}

		// pyCode = adapter.compilerInterpreter.compile( sourceCode,
		// executable.getDocumentName() );
	}

	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException
	{
		PythonInterpreter pythonInterpreter = adapter.getPythonInterpreter( executionContext, executable );

		try
		{
			PyCode pythonCode = pythonCodeReference.get();
			if( pythonCode != null )
				pythonInterpreter.exec( pythonCode );
			else
				// We're using a stream because PythonInterpreter does not
				// expose a string-based method that also accepts a filename.
				pythonInterpreter.execfile( new ByteArrayInputStream( sourceCode.getBytes() ), executable.getDocumentName() );
		}
		catch( Exception x )
		{
			throw JythonAdapter.createExecutionException( executable.getDocumentName(), startLineNumber, x );
		}
		finally
		{
			JythonAdapter.flush( pythonInterpreter );
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * The cached compiled code.
	 */
	private final AtomicReference<PyCode> pythonCodeReference = new AtomicReference<PyCode>();
}