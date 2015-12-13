/*
 * Copyright 2012 Martin Winandy
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

package org.pmw.tinylog.labelers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.pmw.tinylog.hamcrest.ClassMatchers.type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.mocks.SystemTimeMock;
import org.pmw.tinylog.util.ConfigurationCreator;
import org.pmw.tinylog.util.FileHelper;

/**
 * Tests for timestamp labeler.
 *
 * @see TimestampLabeler
 */
public class TimestampLabelerTest extends AbstractLabelerTest {

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH-mm-ss";

	private SystemTimeMock systemTimeMock;

	/**
	 * Set time zone to UTC and set up the mock for {@link System} (to control time).
	 */
	@Before
	public final void init() {
		systemTimeMock = new SystemTimeMock();
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Tear down mock and reset time zone.
	 */
	@After
	public final void dispose() {
		systemTimeMock.tearDown();
		TimeZone.setDefault(null);
	}

	/**
	 * Test labeling for log file with file extension.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testLabelingWithFileExtension() throws IOException {
		File baseFile = FileHelper.createTemporaryFile("tmp");
		baseFile.delete();

		systemTimeMock.setCurrentTimeMillis(0L);
		File targetFile1 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		TimestampLabeler labeler = new TimestampLabeler(TIMESTAMP_FORMAT);
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile1, labeler.getLogFile(baseFile));
		targetFile1.createNewFile();
		targetFile1.setLastModified(systemTimeMock.currentTimeMillis());

		systemTimeMock.setCurrentTimeMillis(1000L);
		File targetFile2 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		FilePair files = labeler.roll(targetFile1, 2);
		assertEquals(targetFile2, files.getFile());
		assertEquals(targetFile1, files.getBackup());
		targetFile2.createNewFile();
		targetFile2.setLastModified(systemTimeMock.currentTimeMillis());
		assertTrue(targetFile1.exists());
		assertTrue(targetFile2.exists());

		systemTimeMock.setCurrentTimeMillis(2000L);
		File targetFile3 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		files = labeler.roll(targetFile2, 2);
		assertEquals(targetFile3, files.getFile());
		assertEquals(targetFile2, files.getBackup());
		targetFile3.createNewFile();
		targetFile3.setLastModified(systemTimeMock.currentTimeMillis());
		assertTrue(targetFile1.exists());
		assertTrue(targetFile2.exists());
		assertTrue(targetFile3.exists());

		systemTimeMock.setCurrentTimeMillis(3000L);
		File targetFile4 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		files = labeler.roll(targetFile3, 2);
		assertEquals(targetFile4, files.getFile());
		assertEquals(targetFile3, files.getBackup());
		targetFile4.createNewFile();
		targetFile4.setLastModified(systemTimeMock.currentTimeMillis());
		assertFalse(targetFile1.exists());
		assertTrue(targetFile2.exists());
		assertTrue(targetFile3.exists());
		assertTrue(targetFile4.exists());

		baseFile.delete();
		targetFile1.delete();
		targetFile2.delete();
		targetFile3.delete();
		targetFile4.delete();
	}

	/**
	 * Test labeling for log file without file extension.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testLabelingWithoutFileExtension() throws IOException {
		File baseFile = FileHelper.createTemporaryFile(null);
		baseFile.delete();

		systemTimeMock.setCurrentTimeMillis(0L);
		File targetFile1 = getBackupFile(baseFile, null, formatCurrentTime());

		TimestampLabeler labeler = new TimestampLabeler(TIMESTAMP_FORMAT);
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile1, labeler.getLogFile(baseFile));
		targetFile1.createNewFile();
		targetFile1.setLastModified(systemTimeMock.currentTimeMillis());

		systemTimeMock.setCurrentTimeMillis(1000L);
		File targetFile2 = getBackupFile(baseFile, null, formatCurrentTime());

		FilePair files = labeler.roll(targetFile1, 1);
		assertEquals(targetFile2, files.getFile());
		assertEquals(targetFile1, files.getBackup());
		targetFile2.createNewFile();
		targetFile2.setLastModified(systemTimeMock.currentTimeMillis());
		assertTrue(targetFile1.exists());
		assertTrue(targetFile2.exists());

		systemTimeMock.setCurrentTimeMillis(2000L);
		File targetFile3 = getBackupFile(baseFile, null, formatCurrentTime());

		files = labeler.roll(targetFile2, 1);
		assertEquals(targetFile3, files.getFile());
		assertEquals(targetFile2, files.getBackup());
		targetFile3.createNewFile();
		targetFile3.setLastModified(systemTimeMock.currentTimeMillis());
		assertFalse(targetFile1.exists());
		assertTrue(targetFile2.exists());
		assertTrue(targetFile3.exists());

		baseFile.delete();
		targetFile1.delete();
		targetFile2.delete();
		targetFile3.delete();
	}

	/**
	 * Test labeling without storing backups.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testLabelingWithoutBackups() throws IOException {
		File baseFile = File.createTempFile("test", ".tmp");
		baseFile.delete();

		systemTimeMock.setCurrentTimeMillis(0L);
		File targetFile1 = getBackupFile(baseFile, "tmp", formatCurrentTime());
		targetFile1.deleteOnExit();

		TimestampLabeler labeler = new TimestampLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile1, labeler.getLogFile(baseFile));
		targetFile1.createNewFile();
		targetFile1.setLastModified(systemTimeMock.currentTimeMillis());

		systemTimeMock.setCurrentTimeMillis(1000L);
		File targetFile2 = getBackupFile(baseFile, "tmp", formatCurrentTime());
		targetFile2.setLastModified(systemTimeMock.currentTimeMillis());

		assertTrue(targetFile1.exists());
		FilePair files = labeler.roll(targetFile1, 0);
		assertEquals(targetFile2, files.getFile());
		assertFalse(targetFile1.exists());
		assertNull(files.getBackup());

		baseFile.delete();
		targetFile1.delete();
		targetFile2.delete();
	}

	/**
	 * Test deleting if backup file is in use.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testDeletingOfBackupFileFails() throws IOException {
		skipOnNonWindowsPlatforms();
		File baseFile = FileHelper.createTemporaryFile("tmp");

		File backupFile = getBackupFile(baseFile, "tmp", formatCurrentTime());
		backupFile.createNewFile();

		try (FileInputStream stream = new FileInputStream(backupFile)) {
			TimestampLabeler labeler = new TimestampLabeler();
			labeler.init(ConfigurationCreator.getDummyConfiguration());
			File currentFile = labeler.getLogFile(baseFile);

			labeler.roll(currentFile, 0);
			assertEquals("LOGGER WARNING: Failed to delete \"" + backupFile + "\"", getErrorStream().nextLine());
		}

		backupFile.delete();
	}

	/**
	 * Test reading timestamp labeler from properties.
	 */
	@Test
	public final void testFromProperties() {
		Labeler labeler = createFromProperties("timestamp");
		assertThat(labeler, type(TimestampLabeler.class));

		labeler = createFromProperties("timestamp: yyyy");
		assertThat(labeler, type(TimestampLabeler.class));
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(new File(MessageFormat.format("test.{0,date,yyyy}.log", new Date())).getAbsoluteFile(), labeler.getLogFile(new File("test.log")));
	}

	private static String formatCurrentTime() {
		return DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT, Locale.ROOT).format(ZonedDateTime.now());
	}

}
