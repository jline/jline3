/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl.history;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.ReaderTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link DefaultHistory}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class HistoryTest extends ReaderTestSupport {
    private DefaultHistory history;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        history = new DefaultHistory(reader);
    }

    @AfterEach
    public void tearDown() {
        history = null;
    }

    @Test
    public void testAdd() {
        assertEquals(0, history.size());

        history.add("test");

        assertEquals(1, history.size());
        assertEquals("test", history.get(0));
        assertEquals(1, history.index());
    }

    private void assertHistoryContains(final int offset, final String... items) {
        assertEquals(items.length, history.size());
        int i = 0;
        for (History.Entry entry : history) {
            assertEquals(offset + i, entry.index());
            assertEquals(items[i++], entry.line());
        }
    }

    @Test
    public void testOffset() {
        reader.setVariable(LineReader.HISTORY_SIZE, 5);

        assertEquals(0, history.size());
        assertEquals(0, history.index());

        history.add("a");
        history.add("b");
        history.add("c");
        history.add("d");
        history.add("e");

        assertEquals(5, history.size());
        assertEquals(5, history.index());
        assertHistoryContains(0, "a", "b", "c", "d", "e");

        history.add("f");

        assertEquals(5, history.size());
        assertEquals(6, history.index());

        assertHistoryContains(1, "b", "c", "d", "e", "f");
        assertEquals("f", history.get(5));
    }

    @Test
    public void testTrimIterate() throws IOException {
        Path histFile = Files.createTempFile(null, null);

        reader.setVariable(LineReader.HISTORY_FILE, histFile);
        reader.setVariable(LineReader.HISTORY_SIZE, 4);
        reader.setVariable(LineReader.HISTORY_FILE_SIZE, 4);

        assertEquals(0, history.size());
        assertEquals(0, history.index());

        history.add("a");
        history.add("b");
        history.add("c");
        history.add("d");
        history.add("e");
        history.add("f");
        history.add("g");

        assertEquals(4, history.size());
        assertEquals(7, history.index());
        assertHistoryContains(3, "d", "e", "f", "g");

        assertEquals("e", history.get(4));
        assertEquals(3, history.iterator().next().index());

        try (BufferedReader reader = Files.newBufferedReader(histFile)) {
            // We should have 5 lines: c, d, e, f, g
            // The history file was trimmed while adding f, but we later added g without trimming
            assertEquals(5, reader.lines().count());
        }
    }

    @Test
    public void testTrim() {
        List<History.Entry> entries = new ArrayList<>();
        entries.add(new DefaultHistory.EntryImpl(0, Instant.now(), "a"));
        entries.add(new DefaultHistory.EntryImpl(1, Instant.now(), "b"));
        entries.add(new DefaultHistory.EntryImpl(2, Instant.now(), "c"));
        entries.add(new DefaultHistory.EntryImpl(3, Instant.now(), "d"));
        entries.add(new DefaultHistory.EntryImpl(4, Instant.now(), "e"));
        entries.add(new DefaultHistory.EntryImpl(5, Instant.now(), "d"));
        entries.add(new DefaultHistory.EntryImpl(6, Instant.now(), "c"));
        entries.add(new DefaultHistory.EntryImpl(7, Instant.now(), "b"));
        entries.add(new DefaultHistory.EntryImpl(8, Instant.now(), "a"));

        List<History.Entry> trimmed = new ArrayList<>(entries);
        trimmed = DefaultHistory.doTrimHistory(trimmed, 6);
        assertEquals(5, trimmed.size());

        trimmed = DefaultHistory.doTrimHistory(trimmed, 3);
        assertEquals(3, trimmed.size());
        assertEquals("c", trimmed.get(0).line());
        assertEquals("b", trimmed.get(1).line());
        assertEquals("a", trimmed.get(2).line());
    }

    @Test
    public void testAddHistoryLine() throws IOException {
        final Path histFile = Files.createTempFile(null, null);

        reader.setOpt(LineReader.Option.HISTORY_TIMESTAMPED);
        try {
            history.addHistoryLine(histFile, ":test");
            fail("Wrong handling of timestamped history");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Bad history file syntax!"));
        }

        try {
            history.addHistoryLine(histFile, "test:test");
            fail("Wrong handling of timestamped history");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Bad history file syntax!"));
        }

        try {
            history.addHistoryLine(histFile, "123456789123456789123456789:test");
            fail("Wrong handling of timestamped history ");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Bad history file syntax!"));
        }
    }

    @Test
    public void testMatchPatterns() {
        DefaultHistory defaultHistory = new DefaultHistory();
        assertFalse(defaultHistory.matchPatterns("foo", "bar"));
        assertTrue(defaultHistory.matchPatterns("foo", "foo"));
        assertTrue(defaultHistory.matchPatterns("foo*", "foobar"));
        assertTrue(defaultHistory.matchPatterns("foo:bar", "bar"));
        assertFalse(defaultHistory.matchPatterns("foo*", "bar"));
    }
}
