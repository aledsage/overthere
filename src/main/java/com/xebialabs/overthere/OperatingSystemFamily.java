/*
 * Copyright (c) 2008-2010 XebiaLabs B.V. All rights reserved.
 *
 * Your use of XebiaLabs Software and Documentation is subject to the Personal
 * License Agreement.
 *
 * http://www.xebialabs.com/deployit-personal-edition-license-agreement
 *
 * You are granted a personal license (i) to use the Software for your own
 * personal purposes which may be used in a production environment and/or (ii)
 * to use the Documentation to develop your own plugins to the Software.
 * "Documentation" means the how to's and instructions (instruction videos)
 * provided with the Software and/or available on the XebiaLabs website or other
 * websites as well as the provided API documentation, tutorial and access to
 * the source code of the XebiaLabs plugins. You agree not to (i) lease, rent
 * or sublicense the Software or Documentation to any third party, or otherwise
 * use it except as permitted in this agreement; (ii) reverse engineer,
 * decompile, disassemble, or otherwise attempt to determine source code or
 * protocols from the Software, and/or to (iii) copy the Software or
 * Documentation (which includes the source code of the XebiaLabs plugins). You
 * shall not create or attempt to create any derivative works from the Software
 * except and only to the extent permitted by law. You will preserve XebiaLabs'
 * copyright and legal notices on the Software and Documentation. XebiaLabs
 * retains all rights not expressly granted to You in the Personal License
 * Agreement.
 */

package com.xebialabs.overthere;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.StringUtils.containsAny;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * The family (flavour) of the operating system running on a host.
 */
public enum OperatingSystemFamily {
	/**
	 * An operating system from the Windows family: NT, XP, Server 2003, Vista, etc.
	 */
	WINDOWS,

	/**
	 * An operating system from the Unix family: Linux, AIX, MacOS, etc.
	 */
	UNIX;

	/**
	 * String containing special characters that require quoting or escaping.
	 */
	private static final String SPECIAL_CHARS = " '\"\\;()${}";

	/**
	 * Returns the extension for scripts used by the operating system family, e.g. <tt>.bat</tt> or <tt>.sh</tt>
	 * 
	 * 
	 * @return the script extension including the preceding dot
	 */
	public String getScriptExtension() {
		if (this == WINDOWS) {
			return ".bat";
		} else {
			return ".sh";
		}
	}

	/**
	 * Returns the characters used by the operating system family to separate line in a text file, e.g. <tt>\r\n</tt> or <tt>\n</tt>
	 * 
	 * @return the line separator
	 */
	public String getLineSeparator() {
		if (this == WINDOWS) {
			return "\r\n";
		} else {
			return "\n";
		}
	}

	/**
	 * Returns the default path of the temporary directory for this operating system family, i.e. <tt>C:\temp</tt> or <tt>/tmp</tt>.
	 * 
	 * @return the path
	 */
	public String getDefaultTemporaryDirectoryPath() {
		if (this == WINDOWS) {
			return "C:\\windows\\temp";
		} else {
			return "/tmp";
		}
	}

	/**
	 * Returns the character used by the operating system family to separate components of a file path, e.g. <tt>\</tt> or <tt>/<tt>.
	 * 
	 * @return the file separator.
	 */
	public String getFileSeparator() {
		if (this == WINDOWS) {
			return "\\";
		} else {
			return "/";
		}
	}

	/**
	 * Returns the character used by the operating system family to separate components of a path, e.g. <tt>;</tt> or <tt>:<tt>.
	 * 
	 * @return the file separator.
	 */
	public String getPathSeparator() {
		if (this == WINDOWS) {
			return ";";
		} else {
			return ":";
		}
	}

	/**
	 * Converts a text to use the {@link #getLineSeparator() line separator} of this operating system family.
	 * 
	 * @param text
	 *            the text to convert.
	 * @return the converted text.
	 */
	public String convertText(String text) {
		if (text == null) {
			return null;
		}

		String lineSep = getLineSeparator();
		try {
			StringBuilder converted = new StringBuilder();
			BufferedReader r = new BufferedReader(new StringReader(text));
			String line;
			while ((line = r.readLine()) != null) {
				converted.append(line).append(lineSep);
			}
			return converted.toString();
		} catch (IOException exc) {
			throw new RuntimeIOException("Unable to read String", exc);
		}
	}

	/**
	 * Encodes a command line array for execution. Any characters that need to be quoted are quoted with a backslash. Empty parameters are encoded as a single
	 * space on {@link #WINDOWS Windows}.
	 * 
	 * @param cmdarray
	 *            the command line to encode
	 * @return the encoded command line
	 */
	public String encodeCommandLineForExecution(String... cmdarray) {
		return encodeCommandLine(false, cmdarray);
	}

	/**
	 * Encodes a command line array for logging. Any passwords in the command line are encoded as a number of stars.
	 * 
	 * @param cmdarray
	 *            the command line to encode
	 * @return the encoded command line
	 */
	public String encodeCommandLineForLogging(String... cmdarray) {
		return encodeCommandLine(true, cmdarray);
	}

	private String encodeCommandLine(boolean hidePassword, String... cmdarray) {
		checkNotNull(cmdarray, "Cannot encode a null command line");
		checkArgument(cmdarray.length > 0, "Cannot encode an empty command line");

		StringBuilder sb = new StringBuilder();
		boolean passwordKeywordSeen = false;
		for (int i = 0; i < cmdarray.length; i++) {
			if (i != 0) {
				sb.append(' ');
			}

			if (cmdarray[i] == null) {
				throw new NullPointerException("Cannot encode a command line with a null parameter (#" + (i + 1) + ")");
			}

			String argument = cmdarray[i];
			if (passwordKeywordSeen && hidePassword) {
				encodePasswordArgument(argument, sb);
			} else {
				encodeArgument(argument, sb);
			}

			passwordKeywordSeen = endsWithIgnoreCase(cmdarray[i], "password");
		}
		return sb.toString();
	}

	private void encodePasswordArgument(String argument, StringBuilder collector) {
		collector.append("********");
	}

	private void encodeArgument(String argument, StringBuilder collector) {
		if (argument.length() == 0) {
			encodeEmptyArgument(collector);
		} else if (!containsAny(argument, SPECIAL_CHARS)) {
			encodeRegularArgument(argument, collector);
		} else {
			if (this == WINDOWS) {
				encodeArgumentWithSpecialCharactersForWindows(argument, collector);
			} else {
				encodeArgumentWithSpecialCharactersForNonWindows(argument, collector);
			}
		}
	}

	private void encodeEmptyArgument(StringBuilder collector) {
		collector.append("\"\"");
	}

	private void encodeRegularArgument(String argument, StringBuilder collector) {
		collector.append(argument);
	}

	private void encodeArgumentWithSpecialCharactersForWindows(String argument, StringBuilder collector) {
		collector.append("\"");
		for (int j = 0; j < argument.length(); j++) {
			char c = argument.charAt(j);
			if (c == '\"') {
				collector.append(c);
			}
			collector.append(c);
		}
		collector.append("\"");
	}

	private void encodeArgumentWithSpecialCharactersForNonWindows(String argument, StringBuilder collector) {
		for (int j = 0; j < argument.length(); j++) {
			char c = argument.charAt(j);
			if (SPECIAL_CHARS.indexOf(c) != -1) {
				collector.append('\\');
			}
			collector.append(c);
		}
	}

}