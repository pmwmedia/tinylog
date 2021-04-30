/*
 * Copyright 2021 Direnc Timur
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.tinylog.writers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.pattern.FormatPatternParser;
import org.tinylog.pattern.Token;
import org.tinylog.provider.InternalLogger;
import org.tinylog.writers.raw.BufferedWriterDecorator;
import org.tinylog.writers.raw.ByteArrayWriter;
import org.tinylog.writers.raw.RandomAccessFileWriter;
import org.tinylog.writers.raw.SynchronizedWriterDecorator;

/**
 * Writer for outputting log entries to a log file in JSON format. Already
 * existing files can be continued.
 */
public final class JsonWriter implements Writer {

	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final int BUFFER_SIZE = 1024;
	private static final String FIELD_PREFIX = "field.";

	private final Charset charset;
	private final RandomAccessFile randomAccessFile;
	private ByteArrayWriter writer;

	private StringBuilder builder;
	private final Map<String, Token> jsonProperties;

	private final byte[] bracketOpenBytes;
	private final byte[] bracketCloseBytes;
	private final byte[] curlyBracketBytes;
	private final byte[] spaceBytes;
	private final byte[] lineFeedBytes;
	private final byte[] carriageReturnBytes;

	/**
	 * @throws IOException              File not found or couldn't access file
	 * @throws IllegalArgumentException Log file is not defined in configuration
	 */
	public JsonWriter() throws IOException {
		this(Collections.<String, String>emptyMap());
	}

	/**
	 * @param properties Configuration for writer
	 * @throws IOException              File not found or couldn't access file
	 * @throws IllegalArgumentException Log file is not defined in configuration
	 */
	public JsonWriter(final Map<String, String> properties) throws IOException {
		jsonProperties = createTokens(properties);
		charset = getCharset(properties);

		bracketOpenBytes = "[".getBytes(charset);
		bracketCloseBytes = "]".getBytes(charset);
		curlyBracketBytes = "{".getBytes(charset);
		spaceBytes = " ".getBytes(charset);
		lineFeedBytes = "\n".getBytes(charset);
		carriageReturnBytes = "\r".getBytes(charset);

		String fileName = getFileName(properties);
		File file = new File(fileName).getAbsoluteFile();
		file.getParentFile().mkdirs();

		boolean append = Boolean.parseBoolean(properties.get("append"));

		randomAccessFile = new RandomAccessFile(file, "rw");
		writer = new RandomAccessFileWriter(randomAccessFile);

		boolean buffered = Boolean.parseBoolean(properties.get("buffered"));
		if (buffered) {
			writer = new BufferedWriterDecorator(writer);
		}

		boolean writingThread = Boolean.parseBoolean(properties.get("writingthread"));
		if (writingThread) {
			builder = new StringBuilder();
		} else {
			writer = new SynchronizedWriterDecorator(writer, randomAccessFile);
		}

		preProcessFile(append);
	}

	@Override
	public void write(final LogEntry logEntry) throws IOException {
		StringBuilder builder;
		if (this.builder == null) {
			builder = new StringBuilder();
		} else {
			builder = this.builder;
			builder.setLength(0);
		}
		removeLastOccurrenceOfIfAvailable(bracketCloseBytes);
		if (!isFirstEntry()) {
			builder.append(",");
		}
		addJsonObject(logEntry, builder);

		byte[] data = builder.toString().getBytes(charset);
		writer.write(data, data.length);
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.flush();
		writer.close();
	}

	@Override
	public Collection<LogEntryValue> getRequiredLogEntryValues() {
		Collection<LogEntryValue> values = EnumSet.noneOf(LogEntryValue.class);
		for (Token token : jsonProperties.values()) {
			values.addAll(token.getRequiredLogEntryValues());
		}
		return values;
	}

	private boolean isFirstEntry() throws IOException {
		if (randomAccessFile.length() == 0) {
			return true;
		} else {
			byte[] bytes = new byte[BUFFER_SIZE];
			randomAccessFile.seek(0);
			int numberOfBytes = randomAccessFile.read(bytes);
			for (int i = numberOfBytes - 1; i >= 0; i--) {
				if (isCharacter(curlyBracketBytes, bytes, i)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Prepares and adds a Json Object. Also escapes special characters.
	 *
	 * @param logEntry LogEntry with information for token
	 * @param builder  Target for the created the JSON object
	 */
	private void addJsonObject(final LogEntry logEntry, final StringBuilder builder) {
		builder.append(NEW_LINE);
		builder.append('\t');
		builder.append("{").append(NEW_LINE);

		Token[] tokenEntries = jsonProperties.values().toArray(new Token[0]);
		String[] fields = jsonProperties.keySet().toArray(new String[0]);

		for (int i = 0; i < tokenEntries.length; i++) {
			builder.append("\t\t\"").append(fields[i]).append("\" : \"");
			int start = builder.length();

			Token token = tokenEntries[i];
			token.render(logEntry, builder);

			escapeCharacter("\\", "\\\\", builder, start);
			escapeCharacter("\"", "\\\"", builder, start);
			escapeCharacter(NEW_LINE, "\\n", builder, start);
			escapeCharacter("\t", "\\t", builder, start);
			escapeCharacter("\b", "\\b", builder, start);
			escapeCharacter("\f", "\\f", builder, start);
			escapeCharacter("\n", "\\n", builder, start);
			escapeCharacter("\r", "\\r", builder, start);

			builder.append("\"");

			if (i + 1 < jsonProperties.size()) {
				builder.append(",").append(NEW_LINE);
			}
		}
		builder.append(NEW_LINE).append("\t}").append(NEW_LINE).append("]");
	}

	private void escapeCharacter(final String character, final String escapeWith, final StringBuilder stringBuilder,
								final int startIndex) {
		for (
			int index = stringBuilder.indexOf(character, startIndex);
			index != -1;
			index = stringBuilder.indexOf(character, index + escapeWith.length())
		) {
			stringBuilder.replace(index, index + character.length(), escapeWith);
		}
	}

	private Charset getCharset(final Map<String, String> properties) {
		String charsetName = properties.get("charset");
		try {
			return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
		} catch (IllegalArgumentException ex) {
			InternalLogger.log(Level.ERROR, "Invalid charset: " + charsetName);
			return Charset.defaultCharset();
		}
	}

	private String getFileName(final Map<String, String> properties) {
		String fileName = properties.get("file");
		if (fileName == null) {
			throw new IllegalArgumentException("File name is missing for file writer");
		}
		return fileName;
	}

	/**
	 * Removes the last occurrence of given character beginning from the end, if
	 * it's available. Searches only for length of {@link #BUFFER_SIZE}
	 *
	 * @param character Character to remove
	 * @return true if the character was removed
	 * @throws IOException File not found or couldn't access file
	 */
	private boolean removeLastOccurrenceOfIfAvailable(final byte[] character) throws IOException {
		long sizeToTruncate = 0;
		boolean foundChar = false;

		byte[] bytes = new byte[BUFFER_SIZE];
		randomAccessFile.seek(Math.max(0, randomAccessFile.length() - BUFFER_SIZE));

		for (int i = randomAccessFile.read(bytes) - character.length; i >= 0; i--) {

			if (isCharacter(character, bytes, i)) {
				sizeToTruncate += character.length;
				foundChar = true;
			} else if (foundChar && !isWhitespace(bytes, i)) {
				break;
			} else {
				sizeToTruncate += 1;
			}
		}
		if (!foundChar) {
			return foundChar;
		}
		long newFileSize = randomAccessFile.length() - sizeToTruncate;
		randomAccessFile.setLength(newFileSize);
		randomAccessFile.seek(randomAccessFile.length());
		return foundChar;
	}

	/**
	 * The method checks for availability of a character in the RandomAccessFile.
	 * Only checks for length of the {@link #BUFFER_SIZE}.
	 *
	 * @param character Character to check for availability
	 * @param start     Offset where the search should begin
	 * @return boolean if the character was found
	 * @throws IOException File not found or couldn't access file
	 */
	private boolean isCharacterAvailable(final byte[] character, final long start) throws IOException {
		byte[] bytes = new byte[BUFFER_SIZE];
		randomAccessFile.seek(start);
		int numberOfBytes = randomAccessFile.read(bytes);

		for (int i = numberOfBytes - 1; i >= 0; i--) {
			if (isCharacter(character, bytes, i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Pre-processes the JSON file. If append mode is on, deletes the closing
	 * bracket and adds a comma instead. If it's a new file, appends an opening
	 * bracket.
	 *
	 * @param append Append Mode on or off
	 * @throws IOException              Error reading or writing file
	 * @throws IllegalArgumentException Invalid file format
	 */
	private void preProcessFile(final boolean append) throws IOException, IllegalArgumentException {
		if (append && randomAccessFile.length() > 0) {
			if (!isCharacterAvailable(bracketOpenBytes, 0)
					|| !isCharacterAvailable(bracketCloseBytes, Math.max(0, randomAccessFile.length() - BUFFER_SIZE))) {
				throw new IllegalArgumentException(
						"Invalid JSON file. The file is missing either an opening or a closing bracket for the array.");
			}
		} else {
			randomAccessFile.setLength(0);
			writer.write(bracketOpenBytes, bracketOpenBytes.length);
			writer.write(bracketCloseBytes, bracketCloseBytes.length);
		}
	}

	private boolean isWhitespace(final byte[] data, final int position) {
		return isCharacter(spaceBytes, data, position)
				|| isCharacter(lineFeedBytes, data, position)
				|| isCharacter(carriageReturnBytes, data, position);
	}

	private boolean isCharacter(final byte[] character, final byte[] data, final int position) {
		if (data.length - position < character.length) {
			return false;
		} else {
			for (int i = 0; i < character.length; ++i) {
				if (character[i] != data[position + i]) {
					return false;
				}
			}
			return true;
		}
	}

	private static Map<String, Token> createTokens(final Map<String, String> properties) {
		FormatPatternParser parser = new FormatPatternParser(properties.get("exception"));

		Map<String, Token> tokens = new HashMap<String, Token>();
		for (Entry<String, String> entry : properties.entrySet()) {
			if (entry.getKey().toLowerCase(Locale.ROOT).startsWith(FIELD_PREFIX)) {
				tokens.put(entry.getKey().substring(FIELD_PREFIX.length()), parser.parse(entry.getValue()));
			}
		}
		return tokens;
	}

}
