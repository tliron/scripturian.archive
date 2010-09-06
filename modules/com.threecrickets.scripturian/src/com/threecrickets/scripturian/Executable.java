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

package com.threecrickets.scripturian;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.threecrickets.scripturian.document.DocumentDescriptor;
import com.threecrickets.scripturian.document.DocumentSource;
import com.threecrickets.scripturian.exception.DocumentException;
import com.threecrickets.scripturian.exception.ExecutionException;
import com.threecrickets.scripturian.exception.ParsingException;
import com.threecrickets.scripturian.internal.ExecutableSegment;
import com.threecrickets.scripturian.service.ExecutableService;

/**
 * Executables are general-purpose operational units that are manifestations of
 * textual "source code" in any supported "language" (see
 * {@link LanguageAdapter} ). Outside of this definition, there is no real limit
 * to how executables are executed or what the language is. Execution can happen
 * in-process, out-of-process, or on a device somewhere in the network. A common
 * use case is to support various programming and templating languages that run
 * in the JVM, adapters for which are included in Scripturian. Another common
 * use case is for executing non-JVM services.
 * <p>
 * The primary design goal is to decouple the code asking for execution from the
 * execution's implementation, while providing clear, predictable concurrent
 * behavior. This abstraction thus lets you 1) plug in diverse execution
 * technologies into your code, 2) dynamically load and execute source code in
 * runtime.
 * <p>
 * Exact performance characteristics are left up to language implementations,
 * but the hope is that this architecture will allow for very high performance,
 * reusability of operational units, and scalability.
 * <p>
 * Source code can be conveniently provided by an implementation of
 * Scripturian's {@link DocumentSource}, which is designed for concurrent use,
 * though you can use any system you like.
 * <p>
 * Usage is divided into three phases: creation, execution and entry.
 * <p>
 * <b>1. Creation.</b> In this phase, the source code is parsed and possibly
 * otherwise analyzed for errors by the language implementation. The intent is
 * for the implementation to perform the bare minimum required for detecting
 * errors in the source code.
 * <p>
 * This phase supports an optional "preparation" sub-phase, with the intent of
 * speeding up usage of later phases at the expense of higher cost during
 * creation. It would be most useful if the executable is intended to be reused.
 * In many implementations, "preparation" would involve compiling the code, and
 * possibly caching the results on disk.
 * <p>
 * The creation phase supports a powerful "text-with-scriptlets" mode, in which
 * source code, wrapped in special delimiters, can be inserted into plain text.
 * "Scriptlets" written in several languages can be mixed into a single
 * executable. The plain text outside of the scriptlets is sent directly to the
 * {@link ExecutionContext} writer.
 * <p>
 * The "text-with-scriptlets" functionality is implemented entirely in this
 * class, and does not have to explicitly supported by language implementations.
 * <p>
 * <b>2. Execution.</b> This phase uses an {@link ExecutionContext} for passing
 * state between the user and the executable, as well as maintaining
 * implementation-specific state. Concurrent reuse is allowed as long as each
 * calling thread uses its own context.
 * <p>
 * <b>3. Entry.</b> This phase allows fine-grained execution via well-defined
 * "entry points" created by the executable during its execution phase.
 * Depending on the language implementation, entry can mean calling a function,
 * method, lambda, closure or macro, or even sending a network request. This
 * phase follows a special execution phase via a call to
 * {@link #makeEnterable(ExecutionContext, Object, ExecutionController)}, after
 * which all entries use the same {@link ExecutionContext}. Passing state is
 * handled differently in entry vs. execution: in entry, support is for sending
 * a list of "argument" states and returning a single state value.
 * <p>
 * Depending on the language implementation, entry can involve better
 * performance than execution due to the use of a single execution context.
 * <p>
 * <h3>"Text-with-scriptlets" executables</h3>
 * <p>
 * During the creation phase, the entire source code document is converted into
 * pure source code. When the code is executed, the non-scriptlet text segments
 * are sent to output via whatever method is appropriate for the language (see
 * {@link LanguageAdapter}).
 * <p>
 * The exception to this behavior is when the first segment is text -- in such
 * cases, just that first segment is sent to output from Java. This is just an
 * optimization.
 * <p>
 * You can detect the trivial case of "text-with-scriptlets" in which no
 * scriptlets are used at all via {@link #getAsPureLiteral()}, and optimize
 * accordingly.
 * <p>
 * Executables can have scriptlets in multiple languages within the same source
 * code. You can specify a different language for each scriptlet in its opening
 * delimiter. If the language is not specified, whatever language was previously
 * used in the source code will be used. If no language was previously
 * specified, the {@code defaultLanguageTag} value from the constructor is used.
 * <p>
 * Two scriptlet delimiting styles are supported: JSP/ASP style (using
 * percentage signs), and the PHP style (using question marks). However, each
 * document must adhere to only one style throughout.
 * <p>
 * In addition to regular scriptlets, Scripturian supports a few shorthand
 * scriptlets for common tasks:
 * <p>
 * The "expression scriptlet (with an equals sign) causes the expression to be
 * sent to standard output. It allows for more readable templates.
 * <p>
 * The "include scriptlet" (with an ampersand) invokes the
 * <code>executable.container.include(name)</code> command as appropriate for
 * the language. Again, it allows for more readable templates.
 * <p>
 * Finally, the "in-flow scriptlet" (with a colon) works like a combination of
 * regular scriptlets with include scriptlets. "In-flow" scriptlets require the
 * use of a {@link DocumentSource}. Read the FAQ for more information.
 * <p>
 * Examples:
 * <ul>
 * <li><b>JSP/ASP-style delimiters</b>:
 * <code>&lt;% print('Hello World'); %&gt;</code></li>
 * <li><b>PHP-style delimiters</b>: <code>&lt;? document.cacheDuration.set 5000
 * ?&gt;</code></li>
 * <li><b>Specifying a language tag</b>:
 * <code>&lt;%groovy print myVariable %&gt; &lt;?php $executable->container->include(lib_name); ?&gt;</code>
 * </li>
 * <li><b>Output expression</b>: <code>&lt;?= 15 * 6 ?&gt;</code></li>
 * <li><b>Output expression with specifying a language tag</b>:
 * <code>&lt;?=js sqrt(myVariable) ?&gt;</code></li>
 * <li><b>Include</b>:
 * <code>&lt;%& 'library.js' %&gt; &lt;?& 'language-' + myObject.getLang + '-support.py' ?&gt;</code>
 * </li>
 * <li><b>In-flow</b>:
 * <code>&lt;%js if(isDebug) { %&gt; &lt;%:python dumpStack(); %&gt; &lt;% } %&gt;</code>
 * </li>
 * </ul>
 * <p>
 * An <code>executable</code> service is exposed to executables for access to
 * this container environment. See {@link ExecutableService}.
 * 
 * @author Tal Liron
 */
public class Executable
{
	//
	// Constants
	//

	/**
	 * The default start delimiter (first option): &lt;%
	 */
	public static final String DEFAULT_DELIMITER1_START = "<%";

	/**
	 * The default end delimiter (first option): %&gt;
	 */
	public static final String DEFAULT_DELIMITER1_END = "%>";

	/**
	 * The default start delimiter (second option): &lt;?
	 */
	public static final String DEFAULT_DELIMITER2_START = "<?";

	/**
	 * The default end delimiter (second option): ?&gt;
	 */
	public static final String DEFAULT_DELIMITER2_END = "?>";

	/**
	 * The default addition to the start delimiter to specify an expression tag:
	 * =
	 */
	public static final String DEFAULT_DELIMITER_EXPRESSION = "=";

	/**
	 * The default addition to the start delimiter to specify an include tag: &
	 */
	public static final String DEFAULT_DELIMITER_INCLUDE = "&";

	/**
	 * The default addition to the start delimiter to specify an in-flow tag: :
	 */
	public static final String DEFAULT_DELIMITER_IN_FLOW = ":";

	/**
	 * The default executable variable name: "executable"
	 */
	public static final String DEFAULT_EXECUTABLE_VARIABLE_NAME = "executable";

	/**
	 * Prefix prepended to in-flow scriptlets stores in the document source.
	 */
	public static final String IN_FLOW_PREFIX = "_IN_FLOW_";

	//
	// Static operations
	//

	/**
	 * If the executable does not yet exist in the document source, retrieves
	 * the source code and parses it into a compact, optimized, executable.
	 * Parsing requires the appropriate {@link LanguageAdapter} implementations
	 * to be available in the language manager.
	 * 
	 * @param documentName
	 *        The document name
	 * @param documentSource
	 *        The document source
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param manager
	 *        The language manager used to parse, prepare and execute the
	 *        executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @return A document descriptor with a valid executable as its document
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a document source error
	 */
	public static DocumentDescriptor<Executable> createOnce( String documentName, DocumentSource<Executable> documentSource, boolean isTextWithScriptlets, LanguageManager manager, String defaultLanguageTag,
		boolean prepare ) throws ParsingException, DocumentException
	{
		DocumentDescriptor<Executable> documentDescriptor = documentSource.getDocument( documentName );
		createOnce( documentDescriptor, documentSource.getIdentifier(), isTextWithScriptlets, manager, defaultLanguageTag, prepare );
		return documentDescriptor;
	}

	/**
	 * If the executable does not yet exist in the document descriptor,
	 * retrieves the source code and parses it into a compact, optimized,
	 * executable. Parsing requires the appropriate {@link LanguageAdapter}
	 * implementations to be available in the language manager.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor
	 * @param partition
	 *        The executable partition
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param manager
	 *        The language manager used to parse, prepare and execute the
	 *        executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @return A new executable or the existing one
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a problem accessing the document source
	 */
	public static Executable createOnce( DocumentDescriptor<Executable> documentDescriptor, String partition, boolean isTextWithScriptlets, LanguageManager manager, String defaultLanguageTag, boolean prepare )
		throws ParsingException, DocumentException
	{
		Executable executable = documentDescriptor.getDocument();
		if( executable == null )
		{
			executable = new Executable( documentDescriptor, partition, isTextWithScriptlets, manager, defaultLanguageTag, prepare );
			Executable existing = documentDescriptor.setDocumentIfAbsent( executable );
			if( existing != null )
				executable = existing;
		}
		return executable;
	}

	//
	// Construction
	//

	/**
	 * Parses source code into a compact, optimized, executable. Parsing
	 * requires the appropriate {@link LanguageAdapter} implementations to be
	 * available in the language manager.
	 * 
	 * @param documentDescriptor
	 *        The document descriptor
	 * @param partition
	 *        The executable partition
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param manager
	 *        The language manager used to parse, prepare and execute the
	 *        executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a problem accessing the document source
	 */
	public Executable( DocumentDescriptor<Executable> documentDescriptor, String partition, boolean isTextWithScriptlets, LanguageManager manager, String defaultLanguageTag, boolean prepare ) throws ParsingException,
		DocumentException
	{
		this( documentDescriptor.getDefaultName(), partition, documentDescriptor.getTimestamp(), documentDescriptor.getSourceCode(), isTextWithScriptlets, manager, manager.getLanguageTagByExtension(
			documentDescriptor.getDefaultName(), documentDescriptor.getTag(), defaultLanguageTag ), documentDescriptor.getSource(), prepare );
	}

	/**
	 * Parses source code into a compact, optimized, executable. Parsing
	 * requires the appropriate {@link LanguageAdapter} implementations to be
	 * available in the language manager.
	 * 
	 * @param documentName
	 *        The document name
	 * @param partition
	 *        The executable partition
	 * @param documentTimestamp
	 *        The executable's document timestamp
	 * @param sourceCode
	 *        The source code -- when {@code isTextWithScriptlets} is false,
	 *        it's considered as pure source code in the language defined by
	 *        {@code defaultLanguageTag}, otherwise it's considered as text with
	 *        embedded scriptlets
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param manager
	 *        The language manager used to parse, prepare and execute the
	 *        executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param documentSource
	 *        A document source used to store in-flow scriptlets; can be null if
	 *        in-flow scriptlets are not used
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @throws ParsingException
	 *         In case of a parsing error
	 * @throws DocumentException
	 *         In case of a problem accessing the document source
	 */
	public Executable( String documentName, String partition, long documentTimestamp, String sourceCode, boolean isTextWithScriptlets, LanguageManager manager, String defaultLanguageTag,
		DocumentSource<Executable> documentSource, boolean prepare ) throws ParsingException, DocumentException
	{
		this( documentName, partition, documentTimestamp, sourceCode, isTextWithScriptlets, manager, defaultLanguageTag, documentSource, prepare, DEFAULT_EXECUTABLE_VARIABLE_NAME, DEFAULT_DELIMITER1_START,
			DEFAULT_DELIMITER1_END, DEFAULT_DELIMITER2_START, DEFAULT_DELIMITER2_END, DEFAULT_DELIMITER_EXPRESSION, DEFAULT_DELIMITER_INCLUDE, DEFAULT_DELIMITER_IN_FLOW );
	}

	/**
	 * Parses source code into a compact, optimized, executable. Parsing
	 * requires the appropriate {@link LanguageAdapter} implementations to be
	 * available in the language manager.
	 * 
	 * @param documentName
	 *        The document name
	 * @param partition
	 *        The executable partition
	 * @param documentTimestamp
	 *        The executable's document timestamp
	 * @param sourceCode
	 *        The source code -- when {@code isTextWithScriptlets} is false,
	 *        it's considered as pure source code in the language defined by
	 *        {@code defaultLanguageTag}, otherwise it's considered as text with
	 *        embedded scriptlets
	 * @param isTextWithScriptlets
	 *        See {@code sourceCode} and {@code defaultLanguageTag}
	 * @param manager
	 *        The language manager used to parse, prepare and execute the
	 *        executable
	 * @param defaultLanguageTag
	 *        When {@code isTextWithScriptlets} is true, this is the language
	 *        used for scriptlets if none is specified
	 * @param documentSource
	 *        A document source used to store in-flow scriptlets; can be null if
	 *        in-flow scriptlets are not used
	 * @param prepare
	 *        Whether to prepare the source code: preparation increases
	 *        initialization time and reduces execution time; note that not all
	 *        languages support preparation as a separate operation
	 * @param exposedExecutableName
	 *        The document variable name
	 * @param delimiter1Start
	 *        The start delimiter (first option)
	 * @param delimiter1End
	 *        The end delimiter (first option)
	 * @param delimiter2Start
	 *        The start delimiter (second option)
	 * @param delimiter2End
	 *        The end delimiter (second option)
	 * @param delimiterExpression
	 *        The default addition to the start delimiter to specify an
	 *        expression tag
	 * @param delimiterInclude
	 *        The default addition to the start delimiter to specify an include
	 *        scriptlet
	 * @param delimiterInFlow
	 *        The default addition to the start delimiter to specify an in-flow
	 *        scriptlet
	 * @throws ParsingException
	 *         In case of a parsing or compilation error
	 * @throws DocumentException
	 *         In case of a problem accessing the document source
	 * @see LanguageAdapter
	 */
	public Executable( String documentName, String partition, long documentTimestamp, String sourceCode, boolean isTextWithScriptlets, LanguageManager manager, String defaultLanguageTag,
		DocumentSource<Executable> documentSource, boolean prepare, String exposedExecutableName, String delimiter1Start, String delimiter1End, String delimiter2Start, String delimiter2End, String delimiterExpression,
		String delimiterInclude, String delimiterInFlow ) throws ParsingException, DocumentException
	{
		this.documentName = documentName;
		this.partition = partition;
		this.documentTimestamp = documentTimestamp;
		this.executableServiceName = exposedExecutableName;
		this.manager = manager;

		if( !isTextWithScriptlets )
		{
			ExecutableSegment segment = new ExecutableSegment( sourceCode, 1, 1, true, false, defaultLanguageTag );
			segments = new ExecutableSegment[]
			{
				segment
			};
			segment.createProgram( this, manager, prepare );
			delimiterStart = null;
			delimiterEnd = null;
			return;
		}

		String lastLanguageTag = defaultLanguageTag;
		LanguageAdapter lastAdapter = manager.getAdapterByTag( defaultLanguageTag );

		int delimiterStartLength = 0;
		int delimiterEndLength = 0;
		int expressionLength = delimiterExpression.length();
		int includeLength = delimiterInclude.length();
		int inFlowLength = delimiterInFlow.length();

		// Detect type of delimiter
		int start = sourceCode.indexOf( delimiter1Start );
		if( start != -1 )
		{
			delimiterStart = delimiter1Start;
			delimiterEnd = delimiter1End;
			delimiterStartLength = delimiterStart.length();
			delimiterEndLength = delimiterEnd.length();
		}
		else
		{
			start = sourceCode.indexOf( delimiter2Start );
			if( start != -1 )
			{
				delimiterStart = delimiter2Start;
				delimiterEnd = delimiter2End;
				delimiterStartLength = delimiterStart.length();
				delimiterEndLength = delimiterEnd.length();
			}
			else
			{
				// No delimiters used
				delimiterStart = null;
				delimiterEnd = null;
			}
		}

		int startLineNumber = 1;
		int startColumnNumber = 1;
		int lastLineNumber = 1;
		int lastColumnNumber = 1;

		if( start != -1 )
			for( int i = sourceCode.indexOf( '\n' ); i >= 0 && i < start; i = sourceCode.indexOf( '\n', i + 1 ) )
				startLineNumber++;

		List<ExecutableSegment> segments = new LinkedList<ExecutableSegment>();

		// Parse segments
		if( start != -1 )
		{
			int last = 0;

			while( start != -1 )
			{
				// Add previous literal segment
				if( start != last )
					segments.add( new ExecutableSegment( sourceCode.substring( last, start ), lastLineNumber, lastColumnNumber, false, false, lastLanguageTag ) );

				start += delimiterStartLength;

				int end = sourceCode.indexOf( delimiterEnd, start );
				if( end == -1 )
					throw new ParsingException( documentName, startLineNumber, startColumnNumber, "Scriptlet does not have an ending delimiter" );

				if( start + 1 != end )
				{
					String languageTag = lastLanguageTag;
					LanguageAdapter adapter = lastAdapter;

					boolean isExpression = false;
					boolean isInclude = false;
					boolean isInFlow = false;

					// Check if this is an expression
					if( sourceCode.substring( start, start + expressionLength ).equals( delimiterExpression ) )
					{
						start += expressionLength;
						isExpression = true;
					}
					// Check if this is an include
					else if( sourceCode.substring( start, start + includeLength ).equals( delimiterInclude ) )
					{
						start += includeLength;
						isInclude = true;
					}
					// Check if this is an in-flow
					else if( sourceCode.substring( start, start + inFlowLength ).equals( delimiterInFlow ) )
					{
						start += inFlowLength;
						isInFlow = true;
					}

					// Get language tag if available
					if( !Character.isWhitespace( sourceCode.charAt( start ) ) )
					{
						int endEngineName = start + 1;
						while( !Character.isWhitespace( sourceCode.charAt( endEngineName ) ) )
							endEngineName++;

						languageTag = sourceCode.substring( start, endEngineName );

						// Optimization: in-flow is unnecessary if we are in the
						// same language
						if( isInFlow && lastLanguageTag.equals( languageTag ) )
							isInFlow = false;

						start = endEngineName + 1;
					}

					if( start + 1 != end )
					{
						// Add scriptlet segment
						if( isExpression || isInclude )
						{
							adapter = manager.getAdapterByTag( languageTag );
							if( adapter == null )
								throw ParsingException.adapterNotFound( documentName, startLineNumber, startColumnNumber, languageTag );

							if( isExpression )
								segments.add( new ExecutableSegment( adapter.getSourceCodeForExpressionOutput( sourceCode.substring( start, end ), this ), startLineNumber, startColumnNumber, true, true, languageTag ) );
							else if( isInclude )
								segments.add( new ExecutableSegment( adapter.getSourceCodeForExpressionInclude( sourceCode.substring( start, end ), this ), startLineNumber, startColumnNumber, true, true, languageTag ) );
						}
						else if( isInFlow && ( documentSource != null ) )
						{
							adapter = manager.getAdapterByTag( languageTag );
							if( adapter == null )
								throw ParsingException.adapterNotFound( documentName, startLineNumber, startColumnNumber, languageTag );

							String inFlowCode = delimiterStart + languageTag + " " + sourceCode.substring( start, end ) + delimiterEnd;
							String inFlowName = IN_FLOW_PREFIX + inFlowCounter.getAndIncrement();

							// Note that the in-flow executable is a single
							// segment, so we can optimize parsing a bit
							Executable inFlowExecutable = new Executable( documentName + "/" + inFlowName, partition, documentTimestamp, inFlowCode, true, manager, languageTag, documentSource, prepare,
								exposedExecutableName, delimiterStart, delimiterEnd, delimiterStart, delimiterEnd, delimiterExpression, delimiterInclude, delimiterInFlow );
							documentSource.setDocument( inFlowName, inFlowCode, "", inFlowExecutable );

							// TODO: would it ever be possible to remove the
							// dependent in-flow instances?

							// Our include scriptlet is in the last language
							segments.add( new ExecutableSegment( lastAdapter.getSourceCodeForExpressionInclude( "\"" + inFlowName + "\"", this ), startLineNumber, startColumnNumber, true, true, lastLanguageTag ) );
						}
						else
							segments.add( new ExecutableSegment( sourceCode.substring( start, end ), startLineNumber, startColumnNumber, true, true, languageTag ) );
					}

					if( !isInFlow )
					{
						lastLanguageTag = languageTag;
						lastAdapter = adapter;
					}
				}

				last = end + delimiterEndLength;
				lastLineNumber = startLineNumber;
				lastColumnNumber = startColumnNumber;
				start = sourceCode.indexOf( delimiterStart, last );
				if( start != -1 )
					for( int i = sourceCode.indexOf( '\n', last ); i >= 0 && i < start; i = sourceCode.indexOf( '\n', i + 1 ) )
						startLineNumber++;
			}

			// Add remaining literal segment
			if( last < sourceCode.length() )
				segments.add( new ExecutableSegment( sourceCode.substring( last ), lastLineNumber, lastColumnNumber, false, false, lastLanguageTag ) );
		}
		else
		{
			// Trivial executable: does not contain scriptlets
			this.segments = new ExecutableSegment[]
			{
				new ExecutableSegment( sourceCode, 1, 1, false, false, lastLanguageTag )
			};
			return;
		}

		// Collapse segments of same kind
		ExecutableSegment previous = null;
		ExecutableSegment current;
		for( Iterator<ExecutableSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( previous != null )
			{
				if( previous.isProgram == current.isProgram )
				{
					if( current.languageTag.equals( previous.languageTag ) )
					{
						// Collapse current into previous
						i.remove();
						previous.startLineNumber = current.startLineNumber;
						previous.startColumnNumber = current.startColumnNumber;
						previous.sourceCode += current.sourceCode;
						current = previous;
					}
				}
			}

			previous = current;
		}

		// Collapse segments of same language (does not convert first segment
		// into a program)
		previous = null;
		for( Iterator<ExecutableSegment> i = segments.iterator(); i.hasNext(); )
		{
			current = i.next();

			if( ( previous != null ) && previous.isProgram )
			{
				if( previous.languageTag.equals( current.languageTag ) )
				{
					// Collapse current into previous
					// (converting to program if necessary)
					i.remove();

					if( current.isProgram )
						previous.sourceCode += current.sourceCode;
					else
					{
						LanguageAdapter adapter = manager.getAdapterByTag( current.languageTag );
						if( adapter == null )
							throw ParsingException.adapterNotFound( documentName, current.startLineNumber, current.startColumnNumber, current.languageTag );

						previous.sourceCode += adapter.getSourceCodeForLiteralOutput( current.sourceCode, this );
					}

					current = previous;
				}
			}

			previous = current;
		}

		// Update positions and create programs
		int position = 0;
		for( ExecutableSegment segment : segments )
		{
			segment.position = position++;
			if( segment.isProgram )
				segment.createProgram( this, manager, prepare );
		}

		// Flatten list into array
		this.segments = new ExecutableSegment[segments.size()];
		segments.toArray( this.segments );
	}

	//
	// Attributes
	//

	/**
	 * The executable's document name.
	 * 
	 * @return The document name
	 * @see #getPartition()
	 */
	public String getDocumentName()
	{
		return documentName;
	}

	/**
	 * The executable partition. It used in addition to the document name to
	 * calculate unique IDs for documents. Partitioning allows you have the same
	 * document name on multiple partitions.
	 * 
	 * @return The executable partition
	 * @see #getDocumentName()
	 */
	public String getPartition()
	{
		return partition;
	}

	/**
	 * The language manager used to parse, prepare and execute the executable.
	 * 
	 * @return The language manager
	 */
	public LanguageManager getManager()
	{
		return manager;
	}

	/**
	 * User-defined attributes.
	 * 
	 * @return The attributes
	 */
	public ConcurrentMap<String, Object> getAttributes()
	{
		return attributes;
	}

	/**
	 * The scriptlet start delimiter used.
	 * 
	 * @return The start delimiter, or null if none was used
	 * @see #getScriptletEndDelimiter()
	 */
	public String getScriptletStartDelimiter()
	{
		return delimiterStart;
	}

	/**
	 * The scrtiplet end delimiter used.
	 * 
	 * @return The end delimiter, or null if none was used
	 * @see #getScriptletStartDelimiter()
	 */
	public String getScriptletEndDelimiter()
	{
		return delimiterEnd;
	}

	/**
	 * The default name for the {@link ExecutableService} instance.
	 */
	public String getExecutableServiceName()
	{
		return executableServiceName;
	}

	/**
	 * The executable's document timestamp.
	 * 
	 * @return The timestamp
	 */
	public long getDocumentTimestamp()
	{
		return documentTimestamp;
	}

	/**
	 * Timestamp of when the executable last finished executing successfully, or
	 * 0 if it was never executed.
	 * 
	 * @return The timestamp or 0
	 */
	public long getLastExecutedTimestamp()
	{
		return lastExecutedTimestamp;
	}

	/**
	 * Returns the source code in the trivial case of a "text-with-scriptlets"
	 * executable that contains no scriptlets. Identifying such executables can
	 * save you from making unnecessary calls to
	 * {@link #execute(ExecutionContext, Object, ExecutionController)} in some
	 * situations.
	 * 
	 * @return The source code if it's pure literal text, null if not
	 */
	public String getAsPureLiteral()
	{
		if( segments.length == 1 )
		{
			ExecutableSegment sole = segments[0];
			if( !sole.isProgram )
				return sole.sourceCode;
		}
		return null;
	}

	/**
	 * The enterable execution context.
	 * 
	 * @return The execution context
	 * @see #makeEnterable(ExecutionContext, Object, ExecutionController)
	 * @see ExecutionContext#enter(Executable, String, Object...)
	 */
	public ExecutionContext getEnterableExecutionContext()
	{
		return enterableExecutionContext.get();
	}

	/**
	 * The container service stored in the context, if it was set.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The container service or null
	 * @see #execute(ExecutionContext, Object, ExecutionController)
	 */
	public Object getContainerService( ExecutionContext executionContext )
	{
		ExecutableService executableService = getExecutableService( executionContext );
		if( executableService != null )
			return executableService.getContainer();
		else
			return null;
	}

	//
	// Operations
	//

	/**
	 * Executes the executable with the current execution context.
	 * 
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws IOException
	 * @see ExecutionContext#getCurrent()
	 */
	public void execute() throws ParsingException, ExecutionException, IOException
	{
		execute( ExecutionContext.getCurrent(), null, null );
	}

	/**
	 * Executes the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public void execute( ExecutionContext executionContext ) throws ParsingException, ExecutionException, IOException
	{
		execute( executionContext, null, null );
	}

	/**
	 * Executes the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @param containerService
	 *        The optional container service
	 * @param executionController
	 *        The optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public void execute( ExecutionContext executionContext, Object containerService, ExecutionController executionController ) throws ParsingException, ExecutionException, IOException
	{
		if( executionContext == null )
			throw new ExecutionException( documentName, "Execute does not have an execution context" );

		executionContext.makeCurrent();

		if( !executionContext.isImmutable() && executionController != null )
			executionController.initialize( executionContext );

		try
		{
			for( ExecutableSegment segment : segments )
			{
				if( !segment.isProgram )
					// Literal
					executionContext.getWriter().write( segment.sourceCode );
				else
				{
					LanguageAdapter adapter = manager.getAdapterByTag( segment.languageTag );
					if( adapter == null )
						throw ParsingException.adapterNotFound( documentName, segment.startLineNumber, segment.startColumnNumber, segment.languageTag );

					if( !executionContext.isImmutable() )
						executionContext.setAdapter( adapter );

					if( !adapter.isThreadSafe() )
						adapter.getLock().lock();

					Object oldExecutableService = null;
					if( !executionContext.isImmutable() )
						oldExecutableService = executionContext.getServices().put( executableServiceName, new ExecutableService( executionContext, manager, containerService ) );

					try
					{
						segment.program.execute( executionContext );
					}
					finally
					{
						if( !executionContext.isImmutable() && oldExecutableService != null )
							executionContext.getServices().put( executableServiceName, oldExecutableService );

						if( !adapter.isThreadSafe() )
							adapter.getLock().unlock();
					}

					if( !executionContext.isImmutable() )
						executionContext.setAdapter( adapter );
				}
			}
		}
		finally
		{
			if( !executionContext.isImmutable() && executionController != null )
				executionController.release( executionContext );
		}

		lastExecutedTimestamp = System.currentTimeMillis();
	}

	/**
	 * Makes an execution context enterable, in preparation for calling
	 * {@link ExecutionContext#enter(Executable, String, Object...)}.
	 * <p>
	 * Note that this can only be done once per executable. If it succeeds and
	 * returns true, the execution context should be considered "consumed" by
	 * this executable. At this point it is immutable, and can only be released
	 * by calling {@link #release()} on the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @param containerService
	 *        The optional container service
	 * @param executionController
	 *        The optional {@link ExecutionController} to be applied to the
	 *        execution context
	 * @return False if we're already prepared and the execution context was not
	 *         consumed, true if preparation succeeded and execution context was
	 *         consumed
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public boolean makeEnterable( ExecutionContext executionContext, Object containerService, ExecutionController executionController ) throws ParsingException, ExecutionException, IOException
	{
		if( enterableExecutionContext.get() != null )
			return false;

		execute( executionContext, containerService, executionController );

		if( !enterableExecutionContext.compareAndSet( null, executionContext ) )
			return false;

		executionContext.enterable = true;
		executionContext.makeImmutable();
		return true;
	}

	/**
	 * Enters the executable at a stored, named location, via the last language
	 * adapter that used the enterable context. According to the language, the
	 * entry point can be a function, method, lambda, closure, etc.
	 * <p>
	 * The context must have been previously made enterable by a call to
	 * {@link #makeEnterable(ExecutionContext, Object, ExecutionController)} .
	 * 
	 * @param entryPointName
	 *        The name of the entry point
	 * @param arguments
	 *        Optional state to pass to the entry point
	 * @return State returned from the entry point or null
	 * @throws ParsingException
	 * @throws ExecutionException
	 * @throws NoSuchMethodException
	 * @see ExecutionContext#enter(Executable, String, Object...)
	 */
	public Object enter( String entryPointName, Object... arguments ) throws ParsingException, ExecutionException, NoSuchMethodException
	{
		ExecutionContext enterableExecutionContext = this.enterableExecutionContext.get();
		if( enterableExecutionContext == null )
			throw new IllegalStateException( "Executable does not have an enterable execution context" );

		return enterableExecutionContext.enter( this, entryPointName, arguments );
	}

	/**
	 * Releases consumed execution contexts.
	 * 
	 * @see #makeEnterable(ExecutionContext, Object, ExecutionController)
	 * @see #finalize()
	 */
	public void release()
	{
		ExecutionContext enterableExecutionContext = this.enterableExecutionContext.getAndSet( null );
		if( enterableExecutionContext != null )
			enterableExecutionContext.release();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Protected

	//
	// Object
	//

	@Override
	protected void finalize()
	{
		release();
	}

	// //////////////////////////////////////////////////////////////////////////
	// Private

	/**
	 * Used to ensure unique names for in-flow scriptlets.
	 */
	private static final AtomicInteger inFlowCounter = new AtomicInteger();

	/**
	 * The executable's partition.
	 */
	private final String partition;

	/**
	 * The executable's document name.
	 */
	private final String documentName;

	/**
	 * The executable's document timestamp.
	 */
	private final long documentTimestamp;

	/**
	 * The language manager used to parse, prepare and execute the executable
	 */
	private final LanguageManager manager;

	/**
	 * User-defined attributes.
	 */
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * The segments, whcih can be scriptlets or plain-text.
	 */
	private final ExecutableSegment[] segments;

	/**
	 * The scriptlet start delimiter used.
	 */
	private final String delimiterStart;

	/**
	 * The scriptlet end delimiter used.
	 */
	private final String delimiterEnd;

	/**
	 * The default name for the {@link ExecutableService} instance.
	 */
	private final String executableServiceName;

	/**
	 * Timestamp of when the executable last finished executing successfully, or
	 * 0 if it was never executed.
	 */
	private volatile long lastExecutedTimestamp = 0;

	/**
	 * The execution context to be used for calls to
	 * {@link #invoke(String, Object...)}.
	 * 
	 * @see #makeEnterable(ExecutionContext, Object, ExecutionController)
	 */
	private final AtomicReference<ExecutionContext> enterableExecutionContext = new AtomicReference<ExecutionContext>();

	/**
	 * Get the exposed service for the executable.
	 * 
	 * @param executionContext
	 *        The execution context
	 * @return The executable service
	 * @see #getExecutableServiceName()
	 */
	private ExecutableService getExecutableService( ExecutionContext executionContext )
	{
		return (ExecutableService) executionContext.getServices().get( executableServiceName );
	}
}
