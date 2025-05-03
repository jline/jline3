/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Expander;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the {@link LineReaderImpl}.
 */
public class TerminalReaderTest extends ReaderTestSupport {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        reader.setHistory(createSeededHistory());
    }

    @Test
    public void testReadline() throws Exception {
        assertLine("Sample String", new TestBuffer("Sample String\n"));
    }

    @Test
    public void testReadlineWithUnicode() throws Exception {
        System.setProperty("input.encoding", StandardCharsets.UTF_8.name());
        assertLine("\u6771\u00E9\u00E8", new TestBuffer("\u6771\u00E9\u00E8\n"));
    }

    @Test
    public void testReadlineWithMask() throws Exception {
        mask = '*';
        assertLine("Sample String", new TestBuffer("Sample String\n"));
        assertTrue(this.out.toString().contains("*************"));
    }

    @Test
    public void testExpansion() throws Exception {
        DefaultHistory history = new DefaultHistory(reader);
        reader.setVariable(LineReader.HISTORY_SIZE, 3);

        history.add("foo");
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");

        Expander expander = new DefaultExpander();

        assertEquals("echo a!", expander.expandHistory(history, "echo a!"));
        assertEquals("mkdir monkey ; echo a!", expander.expandHistory(history, "!! ; echo a!"));
        assertEquals("echo ! a", expander.expandHistory(history, "echo ! a"));
        assertEquals("echo !\ta", expander.expandHistory(history, "echo !\ta"));

        assertEquals(expander.expandHistory(history, "^monk^bar^"), "mkdir barey");
        assertEquals(expander.expandHistory(history, "^monk^bar"), "mkdir barey");
        assertEquals(expander.expandHistory(history, "a^monk^bar"), "a^monk^bar");

        assertEquals(expander.expandHistory(history, "!!"), "mkdir monkey");
        assertEquals("echo echo a", expander.expandHistory(history, "echo !#a"));

        assertEquals(expander.expandHistory(history, "!mk"), "mkdir monkey");
        try {
            expander.expandHistory(history, "!mz");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!mz: event not found", e.getMessage());
        }

        assertEquals(expander.expandHistory(history, "!?mo"), "mkdir monkey");
        assertEquals(expander.expandHistory(history, "!?mo?"), "mkdir monkey");

        assertEquals(expander.expandHistory(history, "!-1"), "mkdir monkey");
        assertEquals(expander.expandHistory(history, "!-2"), "cd c:\\");
        assertEquals(expander.expandHistory(history, "!3"), "cd c:\\");
        assertEquals(expander.expandHistory(history, "!4"), "mkdir monkey");
        try {
            expander.expandHistory(history, "!20");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!20: event not found", e.getMessage());
        }
        try {
            expander.expandHistory(history, "!-20");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!-20: event not found", e.getMessage());
        }
    }

    @Test
    public void testNumericExpansions() throws Exception {
        DefaultHistory history = new DefaultHistory(reader);
        reader.setVariable(LineReader.HISTORY_SIZE, 3);

        // Seed history with three iterator:
        // 1 history1
        // 2 history2
        // 3 history3
        history.add("history1");
        history.add("history2");
        history.add("history3");

        Expander expander = new DefaultExpander();

        // Validate !n
        assertExpansionIllegalArgumentException(expander, history, "!0");
        assertEquals(expander.expandHistory(history, "!1"), "history1");
        assertEquals(expander.expandHistory(history, "!2"), "history2");
        assertEquals(expander.expandHistory(history, "!3"), "history3");
        assertExpansionIllegalArgumentException(expander, history, "!4");

        // Validate !-n
        assertExpansionIllegalArgumentException(expander, history, "!-0");
        assertEquals(expander.expandHistory(history, "!-1"), "history3");
        assertEquals(expander.expandHistory(history, "!-2"), "history2");
        assertEquals(expander.expandHistory(history, "!-3"), "history1");
        assertExpansionIllegalArgumentException(expander, history, "!-4");

        // Validate !!
        assertEquals(expander.expandHistory(history, "!!"), "history3");

        // Add two new iterator. Because maxSize=3, history is:
        // 3 history3
        // 4 history4
        // 5 history5
        history.add("history4");
        history.add("history5");

        // Validate !n
        assertExpansionIllegalArgumentException(expander, history, "!0");
        assertExpansionIllegalArgumentException(expander, history, "!1");
        assertExpansionIllegalArgumentException(expander, history, "!2");
        assertEquals(expander.expandHistory(history, "!3"), "history3");
        assertEquals(expander.expandHistory(history, "!4"), "history4");
        assertEquals(expander.expandHistory(history, "!5"), "history5");
        assertExpansionIllegalArgumentException(expander, history, "!6");

        // Validate !-n
        assertExpansionIllegalArgumentException(expander, history, "!-0");
        assertEquals(expander.expandHistory(history, "!-1"), "history5");
        assertEquals(expander.expandHistory(history, "!-2"), "history4");
        assertEquals(expander.expandHistory(history, "!-3"), "history3");
        assertExpansionIllegalArgumentException(expander, history, "!-4");

        // Validate !!
        assertEquals(expander.expandHistory(history, "!!"), "history5");
    }

    @Test
    public void testArgsExpansion() throws Exception {
        DefaultHistory history = new DefaultHistory(reader);
        reader.setVariable(LineReader.HISTORY_SIZE, 3);

        Expander expander = new DefaultExpander();

        // we can't go back to previous arguments if there are none
        try {
            expander.expandHistory(history, "!$");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!$: event not found", e.getMessage());
        }

        // if no arguments were given, it should expand to the command itself
        history.add("ls");
        assertEquals(expander.expandHistory(history, "!$"), "ls");

        // now we can expand to the last argument
        history.add("ls /home");
        assertEquals(expander.expandHistory(history, "!$"), "/home");

        // we always take the last argument
        history.add("ls /home /etc");
        assertEquals(expander.expandHistory(history, "!$"), "/etc");

        // make sure we don't add spaces accidentally
        history.add("ls /home  /foo ");
        assertEquals(expander.expandHistory(history, "!$"), "/foo");
    }

    @Test
    public void testIllegalExpansionDoesntCrashReadLine() throws Exception {
        DefaultHistory history = new DefaultHistory();
        reader.setHistory(history);
        reader.setVariable(LineReader.BELL_STYLE, "audible");

        assertLine("!f", new TestBuffer("!f\n"));
        assertEquals(1, history.size());
    }

    @Test
    public void testStoringHistory() throws Exception {
        DefaultHistory history = new DefaultHistory();
        reader.setHistory(history);

        assertLine("foo ! bar", new TestBuffer("foo ! bar\n"));

        history.previous();
        assertEquals("foo ! bar", history.current());

        history = new DefaultHistory();
        reader.setHistory(history);
        assertLine("cd c:\\docs", new TestBuffer("cd c:\\\\docs\n"));

        history.previous();
        assertEquals("cd c:\\\\docs", history.current());
    }

    @Test
    public void testExpansionAndHistoryWithEscapes() throws Exception {

        /*
         * Tests the results of the ReaderImpl.readLine() call and the line
         * stored in history. For each input, it tests the with-expansion and
         * without-expansion case.
         */

        // \! (escaped expansion v1)
        assertLineAndHistory("echo ab!ef", "echo ab\\!ef", new TestBuffer("echo ab\\!ef\n"), true, "cd");

        assertLineAndHistory("echo ab\\!ef", "echo ab\\!ef", new TestBuffer("echo ab\\!ef\n"), false, "cd");

        // \!\! (escaped expansion v2)
        assertLineAndHistory("echo ab!!ef", "echo ab\\!\\!ef", new TestBuffer("echo ab\\!\\!ef\n"), true, "cd");

        assertLineAndHistory("echo ab\\!\\!ef", "echo ab\\!\\!ef", new TestBuffer("echo ab\\!\\!ef\n"), false, "cd");

        // !! (expansion)
        assertLineAndHistory("echo abcdef", "echo abcdef", new TestBuffer("echo ab!!ef\n"), true, "cd");

        assertLineAndHistory("echo ab!!ef", "echo ab!!ef", new TestBuffer("echo ab!!ef\n"), false, "cd");

        // \G (backslash no expansion)
        assertLineAndHistory("echo abcGdef", "echo abc\\Gdef", new TestBuffer("echo abc\\Gdef\n"), true, "cd");

        assertLineAndHistory("echo abc\\Gdef", "echo abc\\Gdef", new TestBuffer("echo abc\\Gdef\n"), false, "cd");

        // \^ (escaped expansion)
        assertLineAndHistory("^abc^def", "\\^abc^def", new TestBuffer("\\^abc^def\n"), true, "echo abc");

        assertLineAndHistory("\\^abc^def", "\\^abc^def", new TestBuffer("\\^abc^def\n"), false, "echo abc");

        // ^^ (expansion)
        assertLineAndHistory("echo def", "echo def", new TestBuffer("^abc^def\n"), true, "echo abc");

        assertLineAndHistory("^abc^def", "^abc^def", new TestBuffer("^abc^def\n"), false, "echo abc");
    }

    @Test
    public void testStoringHistoryWithExpandEventsOff() throws Exception {
        assertLineAndHistory("foo ! bar", "foo ! bar", new TestBuffer("foo ! bar\n"), false);
    }

    @Test
    public void testBell() throws Exception {
        reader.setVariable(LineReader.BELL_STYLE, "off");
        reader.beep();
        assertEquals(0, out.size(), "out should not have received bell");

        reader.setVariable(LineReader.BELL_STYLE, "audible");
        reader.beep();
        String bell = Curses.tputs(terminal.getStringCapability(Capability.bell));
        assertEquals(bell, out.toString(), "out should have received bell");
    }

    @Test
    public void testCallbacks() throws Exception {
        reader.getKeys().bind((Widget) () -> reader.getBuffer().clear(), "x");
        assertLine("", new TestBuffer("sample stringx\n"));
    }

    @Test
    public void testDefaultBuffer() throws Exception {
        in.setIn(new ByteArrayInputStream(new TestBuffer().enter().getBytes()));
        String line = reader.readLine(null, null, "foo");
        assertEquals("foo", line);
    }

    @Test
    public void testReadBinding() throws Exception {
        in.setIn(new ByteArrayInputStream(new TestBuffer("abcde").getBytes()));

        KeyMap<Binding> map = new KeyMap<>();
        map.bind(new Reference("foo"), "bc");
        map.bind(new Reference("bar"), "e");

        Binding b = reader.readBinding(map);
        assertEquals(new Reference("foo"), b);
        assertEquals("bc", reader.getLastBinding());
        b = reader.readBinding(map);
        assertEquals(new Reference("bar"), b);
        assertEquals("e", reader.getLastBinding());
        b = reader.readBinding(map);
        assertNull(b);
    }

    private History createSeededHistory() {
        History history = new DefaultHistory();
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");
        return history;
    }

    private void assertLineAndHistory(
            String expectedLine,
            String expectedHistory,
            TestBuffer input,
            boolean expandEvents,
            String... historyItems) {
        DefaultHistory history = new DefaultHistory();
        reader.setHistory(history);
        if (historyItems != null) {
            for (String historyItem : historyItems) {
                history.add(historyItem);
            }
        }
        if (expandEvents) {
            reader.unsetOpt(Option.DISABLE_EVENT_EXPANSION);
        } else {
            reader.setOpt(Option.DISABLE_EVENT_EXPANSION);
        }
        assertLine(expectedLine, input, false);
        history.previous();
        assertEquals(expectedHistory, history.current());
    }

    /**
     * Validates that an 'event not found' IllegalArgumentException is thrown
     * for the expansion event.
     */
    protected void assertExpansionIllegalArgumentException(Expander expander, History history, String event)
            throws Exception {
        try {
            expander.expandHistory(history, event);
            fail("Expected IllegalArgumentException for " + event);
        } catch (IllegalArgumentException e) {
            assertEquals(event + ": event not found", e.getMessage());
        }
    }
}
