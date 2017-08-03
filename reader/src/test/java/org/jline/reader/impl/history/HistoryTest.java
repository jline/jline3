/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl.history;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.ReaderTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DefaultHistory}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class HistoryTest extends ReaderTestSupport
{
    private DefaultHistory history;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        history = new DefaultHistory(reader);
    }

    @After
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
        int i=0;
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
        DefaultHistory.doTrimHistory(trimmed, 6);
        assertEquals(5, trimmed.size());

        DefaultHistory.doTrimHistory(trimmed, 3);
        assertEquals(3, trimmed.size());
        assertEquals("c", trimmed.get(0).line());
        assertEquals("b", trimmed.get(1).line());
        assertEquals("a", trimmed.get(2).line());
    }

}
