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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.core.TinylogLoggingProviderTest.LogEntryValues;
import org.tinylog.rules.SystemStreamCollector;
import org.tinylog.util.FileSystem;
import org.tinylog.util.LogEntryBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;

public final class JsonWriterTest {

	private static final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * Redirects and collects system output streams.
	 */
	@Rule
	public final SystemStreamCollector systemStream = new SystemStreamCollector(true);

	/**
	 * Verifies that with no logging, an empty array is created.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void noWriting() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "false");

		JsonWriter writer = new JsonWriter(properties);
		writer.close();

		String expectedFileContent = "[]";
		String resultingEntry = FileSystem.readFile(file);

		assertThat(resultingEntry).isEqualTo(expectedFileContent);
	}

	/**
	 * Verifies that file is rewritten.
	 *
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void unappendingWriting() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "false");
		properties.put("field.level", "level");
		properties.put("field.message", "message");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.write(givenLogEntry);
		writer.close();

		writer = new JsonWriter(properties);
		writer.write(givenLogEntry);
		writer.write(givenLogEntry);
		writer.close();

		String expectedMessage = String.format("\"message\" : \"%s\"", givenLogEntry.getMessage());
		String expectedLevel = String.format("\"level\" : \"%s\"", givenLogEntry.getLevel());
		String resultingEntry = FileSystem.readFile(file);

		int resultingMessageCount = resultingEntry.split(Pattern.quote(expectedMessage)).length - 1;
		int resultingLevelCount = resultingEntry.split(Pattern.quote(expectedLevel)).length - 1;
		assertThat(resultingMessageCount).isEqualTo(2);
		assertThat(resultingLevelCount).isEqualTo(2);
	}

	/**
	 * Verifies that file is appended with new entries.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void appendingWriting() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "true");
		properties.put("field.level", "level");
		properties.put("field.message", "message");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.write(givenLogEntry);
		writer.close();

		writer = new JsonWriter(properties);
		writer.write(givenLogEntry);
		writer.write(givenLogEntry);
		writer.close();

		String expectedMessage = String.format("\"message\" : \"%s\"", givenLogEntry.getMessage());
		String expectedLevel = String.format("\"level\" : \"%s\"", givenLogEntry.getLevel());
		String resultingEntry = FileSystem.readFile(file);

		int resultingMessageCount = resultingEntry.split(Pattern.quote(expectedMessage)).length - 1;
		int resultingLevelCount = resultingEntry.split(Pattern.quote(expectedLevel)).length - 1;
		assertThat(resultingMessageCount).isEqualTo(4);
		assertThat(resultingLevelCount).isEqualTo(4);
	}

	/**
	 * Verifies that JSON Array is correctly built.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void writesArrayCorrectly() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "false");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.close();

		String resultingEntry = FileSystem.readFile(file);

		int indexOfOpeningArray = resultingEntry.indexOf("[");
		int indexOfOpeningJsonObject = resultingEntry.indexOf("{");
		int indexOfClosingJsonObject = resultingEntry.indexOf("}");
		int indexOfClosingArray = resultingEntry.indexOf("]");

		assertThat(indexOfOpeningJsonObject).isGreaterThan(indexOfOpeningArray).isLessThan(indexOfClosingJsonObject);
		assertThat(indexOfClosingJsonObject).isLessThan(indexOfClosingArray);
	}

	/**
	 * Verifies that JSON entries are correctly added and are separated by commas.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void addsJsonObjectCorrectly() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "true");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.write(givenLogEntry);
		writer.close();

		String resultingEntry = FileSystem.readFile(file);

		int indexOfClosingFirstJsonObject = resultingEntry.indexOf("}");
		int indexOfOpeningSecondJsonObject = resultingEntry.indexOf("{", indexOfClosingFirstJsonObject);
		int indexOfComma = resultingEntry.indexOf(",", indexOfClosingFirstJsonObject);
		assertThat(indexOfComma).isLessThan(indexOfOpeningSecondJsonObject)
				.isGreaterThan(indexOfClosingFirstJsonObject);
	}

	/**
	 * Verifies that input fields match output properties in JSON.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void evaluatesFieldsCorrectly() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("field.msg", "message");
		properties.put("field.lvl", "level");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.close();

		String expectedMessageField = String.format("\"msg\" : \"%s\"", givenLogEntry.getMessage());
		String expectedLevelField = String.format("\"lvl\" : \"%s\"", givenLogEntry.getLevel());
		String resultingEntry = FileSystem.readFile(file);
		assertThat(resultingEntry).contains(expectedMessageField).contains(expectedLevelField);
	}

	/**
	 * Verifies that exception is thrown when there is no file name.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test(expected = IllegalArgumentException.class)
	public void expectsFilename() throws IOException {
		Map<String, String> properties = new HashMap<>();

		new JsonWriter(properties);
	}

	/**
	 * Verifies that an invalid charset is handled with the default charset.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void handlesInvalidCharset() throws IOException {
		String file = FileSystem.createTemporaryFile();
		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("field.message", "message");
		properties.put("charset", "asdf");

		JsonWriter writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.close();

		String resultingEntry = FileSystem.readFile(file, Charset.defaultCharset());
		assertThat(systemStream.consumeErrorOutput()).containsOnlyOnce("ERROR").containsOnlyOnce("charset")
				.containsOnlyOnce("asdf");
		assertThat(resultingEntry).contains(givenLogEntry.getMessage());
	}

	/**
	 * Verifies that special characters get escaped.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void escapesCharacters() throws IOException {
		String file = FileSystem.createTemporaryFile();
		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("field.message", "message");

		JsonWriter writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class)
				.message("Hello World!" + NEW_LINE + "\t\t" + NEW_LINE + "\"\f\b").create();
		writer.write(givenLogEntry);
		writer.close();

		String expectedMessage = "Hello World!\\n\\t\\t\\n\\\"\\f\\b";
		String resultingEntry = FileSystem.readFile(file);

		assertThat(resultingEntry).contains(expectedMessage);
	}

	/**
	 * Verifies that an exception is thrown when there is an invalid JSON file
	 * (currently only missing closing bracket).
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test(expected = IllegalArgumentException.class)
	public void handlesInvalidJsonFile() throws IOException {
		String file = FileSystem.createTemporaryFile("[{}");
		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("append", "true");
		properties.put("field.message", "message");

		new JsonWriter(properties);
	}

	/**
	 * Verifies that the required {@link LogEntryValues} are correctly evaluated.
	 * 
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void evaluatesRequiredLogValuesCorrectly() throws IOException {
		String file = FileSystem.createTemporaryFile();
		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("append", "true");
		properties.put("field.message", "message");
		properties.put("field.date", "date");
		properties.put("field.line", "line");

		Writer writer = new JsonWriter(properties);

		Collection<LogEntryValue> logValues = writer.getRequiredLogEntryValues();

		List<LogEntryValue> expectedValues = List.of(LogEntryValue.MESSAGE, LogEntryValue.EXCEPTION, LogEntryValue.DATE,
				LogEntryValue.LINE);

		assertThat(logValues).containsAll(expectedValues);
	}

	/**
	 * Verifies that file is valid between log entries.
	 *
	 * @throws IOException Failed writing to file
	 */
	@Test
	public void correctStateBetweenWrites() throws IOException {
		String file = FileSystem.createTemporaryFile();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "false");
		properties.put("field.level", "level");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		String expectedFirstEntry = String.format("[%s\t{%s\t\t\"level\" : \"%s\"%s\t}%s]", NEW_LINE, NEW_LINE,
				givenLogEntry.getLevel(), NEW_LINE, NEW_LINE);
		String resultingEntry = FileSystem.readFile(file);
		assertThat(resultingEntry).isEqualTo(expectedFirstEntry);
		writer.close();
	}

	@Test
	public void handleCharset() throws IOException {
		String file = FileSystem.createTemporaryFile();

		String charsetName = "utf-16le";
		Charset utf16Charset = Charset.forName(charsetName);

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file);
		properties.put("buffered", "false");
		properties.put("append", "false");
		properties.put("charset", charsetName);
		properties.put("field.message", "message");

		JsonWriter writer;
		writer = new JsonWriter(properties);
		LogEntry givenLogEntry = LogEntryBuilder.prefilled(JsonWriterTest.class).create();
		writer.write(givenLogEntry);
		writer.close();

		byte[] expectedResult = String.format("[%s\t{%s\t\t\"message\" : \"%s\"%s\t}%s]", NEW_LINE, NEW_LINE,
				givenLogEntry.getMessage(), NEW_LINE, NEW_LINE).getBytes(charsetName);
		byte[] resultingEntry = FileSystem.readFile(file, utf16Charset).getBytes(charsetName);
		assertArrayEquals(resultingEntry, expectedResult);
	}

}
