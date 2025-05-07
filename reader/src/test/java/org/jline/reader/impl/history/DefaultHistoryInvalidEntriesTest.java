/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl.history;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.ReaderTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for handling invalid entries in history files.
 */
public class DefaultHistoryInvalidEntriesTest extends ReaderTestSupport {

    /**
     * Test that the DefaultHistory class can handle invalid entries in the history file
     * when loading history.
     */
    @Test
    public void testHandleInvalidHistoryEntries(@TempDir Path tempDir) throws IOException {
        // Create a history file with valid and invalid entries
        Path historyFile = tempDir.resolve("history");
        try (BufferedWriter writer = Files.newBufferedWriter(historyFile)) {
            // Valid entries with timestamps
            long timestamp = Instant.now().toEpochMilli();
            writer.write(timestamp + ":valid command 1\n");
            writer.write((timestamp + 1000) + ":valid command 2\n");

            // Invalid entry for timestamped history (missing colon)
            writer.write("1234567890\n");

            // Valid entry
            writer.write((timestamp + 2000) + ":valid command 3\n");

            // Another invalid entry
            writer.write("abc:invalid timestamp\n");
        }

        // Create a reader with timestamped history option
        LineReader reader = LineReaderBuilder.builder()
                .option(LineReader.Option.HISTORY_TIMESTAMPED, true)
                .build();

        DefaultHistory history = new DefaultHistory();
        history.attach(reader);

        // Read the history file directly
        history.read(historyFile, false);

        // Verify that only valid entries were loaded
        assertEquals(3, history.size(), "History should contain only valid entries");

        // Save the history to rewrite the file
        history.write(historyFile, false);

        // Verify the history file was rewritten with only valid entries
        List<String> lines = Files.readAllLines(historyFile);
        assertEquals(3, lines.size(), "History file should contain only valid entries");
    }
}
