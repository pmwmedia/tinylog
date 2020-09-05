/*
 * Copyright 2018 Martin Winandy
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;
import org.tinylog.configuration.ServiceLoader;
import org.tinylog.converters.FileConverter;
import org.tinylog.core.LogEntryValue;
import org.tinylog.rules.SystemStreamCollector;
import org.tinylog.util.FileSystem;
import org.tinylog.util.LogEntryBuilder;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.tinylog.util.Maps.doubletonMap;
import static org.tinylog.util.Maps.tripletonMap;

/**
 * Tests for {@link RollingFileWriter}.
 */
public final class RollingFileWriterTest {

	private static final String NEW_LINE = System.lineSeparator();

	/**
	 * Redirects and collects system output streams.
	 */
	@Rule
	public final SystemStreamCollector systemStream = new SystemStreamCollector(true);

	/**
	 * Temporary folder for creating volatile files.
	 */
	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	/**
	 * Verifies that log entries will be immediately output, if buffer is disabled.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void unbufferedWriting() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		RollingFileWriter writer = new RollingFileWriter(tripletonMap("file", file, "format", "{message}", "buffered", "false"));

		writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		assertThat(FileSystem.readFile(file)).isEqualTo("Hello World!" + NEW_LINE);

		writer.close();
		assertThat(FileSystem.readFile(file)).isEqualTo("Hello World!" + NEW_LINE);
	}

	/**
	 * Verifies that log entries will be output after flushing, if buffer is enabled.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void bufferedWriting() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		RollingFileWriter writer = new RollingFileWriter(tripletonMap("file", file, "format", "{message}", "buffered", "true"));

		writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		assertThat(FileSystem.readFile(file)).isEmpty();

		writer.flush();
		assertThat(FileSystem.readFile(file)).isEqualTo("Hello World!" + NEW_LINE);

		writer.close();
		assertThat(FileSystem.readFile(file)).isEqualTo("Hello World!" + NEW_LINE);
	}

	/**
	 * Verifies that writing works and the underlying byte array writer is thread-safe, if writing thread is disable.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void writingThreadDisabled() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		RollingFileWriter writer = new RollingFileWriter(tripletonMap("file", file, "format", "{message}", "writingthread", "false"));

		writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		writer.flush();
		assertThat(FileSystem.readFile(file)).isEqualTo("Hello World!" + NEW_LINE);
		writer.close();

		assertThat(Whitebox.<Boolean>getInternalState(writer, "writingThread")).isFalse();
	}

	/**
	 * Verifies that writing works and the underlying byte array writer is not thread-safe, if writing thread is enabled.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void writingThreadEnabled() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		RollingFileWriter writer = new RollingFileWriter(tripletonMap("file", file, "format", "{message}", "writingthread", "true"));

		writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		writer.flush();
		assertThat(FileSystem.readFile(file)).isEqualTo("Hello World!" + NEW_LINE);
		writer.close();

		assertThat(Whitebox.<Boolean>getInternalState(writer, "writingThread")).isTrue();
	}

	/**
	 * Verifies that a configured charset will be used for encoding texts.
	 *
	 * @throws IOException
	 *             Failed opening file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void definedCharset() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();

		RollingFileWriter writer = new RollingFileWriter(tripletonMap("file", file, "format", "{message}", "charset", "UTF-16"));
		writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		writer.close();

		assertThat(FileSystem.readFile(file, StandardCharsets.UTF_16)).isEqualTo("Hello World!" + NEW_LINE);
	}

	/**
	 * Verifies that the default pattern contains a minimum set of information.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void defaultFormatPattern() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		RollingFileWriter writer = new RollingFileWriter(singletonMap("file", file));

		assertThat(writer.getRequiredLogEntryValues())
			.contains(LogEntryValue.DATE, LogEntryValue.LEVEL, LogEntryValue.MESSAGE, LogEntryValue.EXCEPTION);

		writer.write(LogEntryBuilder.prefilled(RollingFileWriterTest.class).create());
		writer.close();

		assertThat(FileSystem.readFile(file))
			.contains("1985").contains("03")
			.contains("TRACE")
			.contains("Hello World!")
			.endsWith(NEW_LINE);
	}

	/**
	 * Verifies that an already existing file can be continued.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void continueExistingFile() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		Map<String, String> properties = tripletonMap("file", file, "format", "{message}", "policies", "size: 1MB");

		RollingFileWriter writer = new RollingFileWriter(properties);
		writer.write(LogEntryBuilder.empty().message("First").create());
		writer.close();

		writer = new RollingFileWriter(properties);
		writer.write(LogEntryBuilder.empty().message("Second").create());
		writer.close();

		assertThat(FileSystem.readFile(file)).isEqualTo("First" + NEW_LINE + "Second" + NEW_LINE);
	}

	/**
	 * Verifies that a new file can be started.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void discontinueExistingFile() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		Map<String, String> properties = tripletonMap("file", file, "format", "{message}", "policies", "startup");

		RollingFileWriter writer = new RollingFileWriter(properties);
		writer.write(LogEntryBuilder.empty().message("First").create());
		writer.close();

		writer = new RollingFileWriter(properties);
		writer.write(LogEntryBuilder.empty().message("Second").create());
		writer.close();

		assertThat(FileSystem.readFile(file)).isEqualTo("Second" + NEW_LINE);
	}

	/**
	 * Verifies that a new file will be created if none exists.
	 *
	 * @throws IOException
	 *             Failed access to temporary file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void createNewFile() throws IOException, InterruptedException {
		File file = folder.newFile();
		file.delete();
		String path = file.getAbsolutePath();

		RollingFileWriter writer = new RollingFileWriter(tripletonMap("file", path, "format", "{message}", "policies", "size: 1MB"));
		writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		writer.close();

		assertThat(FileSystem.readFile(path)).isEqualTo("Hello World!" + NEW_LINE);
	}

	/**
	 * Verifies that a link to the latest log file can be created when creating a new rolling file writer.
	 *
	 * @throws IOException
	 *             Failed access to temporary folder or files
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void linkToLatestAtStartup() throws IOException, InterruptedException {
		File latest = new File(folder.getRoot(), "latest");

		Map<String, String> properties = new HashMap<>();
		properties.put("file", new File(folder.getRoot(), "{count}").getAbsolutePath());
		properties.put("latest", new File(folder.getRoot(), "latest").getAbsolutePath());
		properties.put("format", "{message}");
		properties.put("backups", "3");

		RollingFileWriter writer = new RollingFileWriter(properties);
		try {
			writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		} finally {
			writer.close();
		}

		assertThat(latest).exists();
		assertThat(latest).hasContent("Hello World!");
	}

	/**
	 * Verifies that a link to the latest log file will be updated when starting a new log file.
	 *
	 * @throws IOException
	 *             Failed access to temporary folder or files
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void linkToLatestOnRollover() throws IOException, InterruptedException {
		File latest = new File(folder.getRoot(), "latest");

		Map<String, String> properties = new HashMap<>();
		properties.put("file", new File(folder.getRoot(), "{count}").getAbsolutePath());
		properties.put("latest", new File(folder.getRoot(), "latest").getAbsolutePath());
		properties.put("format", "{message}");
		properties.put("policies", "size: 10");

		RollingFileWriter writer = new RollingFileWriter(properties);
		try {
			writer.write(LogEntryBuilder.empty().message("First").create());
			writer.write(LogEntryBuilder.empty().message("Second").create());
		} finally {
			writer.close();
		}

		assertThat(latest).exists();
		assertThat(latest).hasContent("Second");
	}

	/**
	 * Verifies that a file converter can be used to transform data.
	 *
	 * @throws IOException
	 *             Failed access to temporary folder or files
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void useFileConverter() throws IOException, InterruptedException {
		FileConverter converter = mock(FileConverter.class);
		when(converter.write(any())).thenReturn(("Hallo Welt!" + NEW_LINE).getBytes(StandardCharsets.UTF_8));
		WrapperFileConverter.converter = converter;

		File file = folder.newFile();
		file.delete();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", file.getAbsolutePath());
		properties.put("format", "{message}");
		properties.put("convert", WrapperFileConverter.class.getName());

		RollingFileWriter writer = new RollingFileWriter(properties);
		try {
			writer.write(LogEntryBuilder.empty().message("Hello World!").create());
		} finally {
			writer.close();
		}

		assertThat(FileSystem.readFile(file.getAbsolutePath())).isEqualTo("Hallo Welt!" + NEW_LINE);

		verify(converter).open(file.getAbsolutePath());
		verify(converter).write(("Hello World!" + NEW_LINE).getBytes(StandardCharsets.UTF_8));
		verify(converter).close();
	}

	/**
	 * Verifies that all backup files will be kept if deletion of backups is disabled.
	 *
	 * @throws IOException
	 *             Failed access to temporary folder or files
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void keepAllBackups() throws IOException, InterruptedException {
		File file1 = folder.newFile("0");
		File file2 = folder.newFile("1");
		File file3 = folder.newFile("2");
		File file4 = folder.newFile("3");

		file1.setLastModified(0);
		file2.setLastModified(1000);
		file3.setLastModified(2000);
		file4.setLastModified(3000);

		Map<String, String> properties = new HashMap<>();
		properties.put("file", new File(folder.getRoot(), "{count}").getAbsolutePath());
		properties.put("format", "{message}");
		properties.put("policies", "size: 1MB");
		properties.put("backups", "-1");

		RollingFileWriter writer = new RollingFileWriter(properties);
		try {
			assertThat(file1).exists();
			assertThat(file2).exists();
			assertThat(file3).exists();
			assertThat(file4).exists();
		} finally {
			writer.close();
		}
	}

	/**
	 * Verifies that obsolete backup files will be deleted at start-up.
	 *
	 * @throws IOException
	 *             Failed access to temporary folder or files
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void deleteBackupsAtStartUp() throws IOException, InterruptedException {
		File file1 = folder.newFile("0");
		File file2 = folder.newFile("1");
		File file3 = folder.newFile("2");

		file1.setLastModified(0);
		file2.setLastModified(1000);
		file3.setLastModified(2000);

		Map<String, String> properties = new HashMap<>();
		properties.put("file", new File(folder.getRoot(), "{count}").getAbsolutePath());
		properties.put("format", "{message}");
		properties.put("policies", "size: 1MB");
		properties.put("backups", "2");

		RollingFileWriter writer = new RollingFileWriter(properties);
		try {
			assertThat(file1).doesNotExist();
			assertThat(file2).exists();
			assertThat(file3).exists();
		} finally {
			writer.close();
		}
	}

	/**
	 * Verifies that obsolete backup files will be deleted when rolling log file.
	 *
	 * @throws IOException
	 *             Failed access to temporary folder or files
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void deleteBackupsAtRollOver() throws IOException, InterruptedException {
		File file1 = folder.newFile("0");
		File file2 = folder.newFile("1");
		File file3 = folder.newFile("2");
		File file4 = folder.newFile("3");

		file1.setLastModified(0);
		file2.setLastModified(1000);
		file3.setLastModified(2000);
		file4.delete();

		Map<String, String> properties = new HashMap<>();
		properties.put("file", new File(folder.getRoot(), "{count}").getAbsolutePath());
		properties.put("format", "{message}");
		properties.put("policies", "size: 10");
		properties.put("backups", "3");

		RollingFileWriter writer = new RollingFileWriter(properties);

		assertThat(file1).exists();
		assertThat(file2).exists();
		assertThat(file3).exists();
		assertThat(file4).doesNotExist();

		writer.write(LogEntryBuilder.empty().message("First").create());
		writer.write(LogEntryBuilder.empty().message("Second").create());

		assertThat(file1).doesNotExist();
		assertThat(file2).exists();
		assertThat(file3).hasContent("First" + NEW_LINE);
		assertThat(file4).hasContent("Second" + NEW_LINE);

		writer.close();
	}

	/**
	 * Verifies that an invalid charset will be reported as error.
	 *
	 * @throws IOException
	 *             Failed opening file
	 * @throws InterruptedException
	 *             Interrupted while waiting for the converter
	 */
	@Test
	public void invalidCharset() throws IOException, InterruptedException {
		String file = FileSystem.createTemporaryFile();
		new RollingFileWriter(doubletonMap("file", file, "charset", "UTF-42")).close();

		assertThat(systemStream.consumeErrorOutput()).containsOnlyOnce("ERROR").containsOnlyOnce("charset").containsOnlyOnce("UTF-42");
	}

	/**
	 * Verifies that an exception will be thrown, if no file name is defined. The message of the thrown exception should
	 * contain "file name" or "filename".
	 */
	@Test
	public void missingFileName() {
		assertThatThrownBy(() -> new RollingFileWriter(emptyMap())).hasMessageMatching("(?i).*file ?name.*");
	}

	/**
	 * Verifies that writer is registered as service under the name "rolling file".
	 *
	 * @throws IOException
	 *             Failed creating temporary file
	 */
	@Test
	public void isRegistered() throws IOException {
		String file = FileSystem.createTemporaryFile();
		Writer writer = new ServiceLoader<>(Writer.class, Map.class).create("rolling file", singletonMap("file", file));
		assertThat(writer).isInstanceOf(RollingFileWriter.class);
	}

	/**
	 * Wrapper file converter for wrapping a concrete file converter instance.
	 */
	public static class WrapperFileConverter implements FileConverter {

		private static FileConverter converter;

		@Override
		public String getBackupSuffix() {
			return converter.getBackupSuffix();
		}

		@Override
		public void open(final String fileName) {
			converter.open(fileName);
		}

		@Override
		public byte[] write(final byte[] data) {
			return converter.write(data);
		}

		@Override
		public void close() {
			converter.close();
		}

		@Override
		public void shutdown() throws InterruptedException {
			converter.shutdown();
		}

	}

}
