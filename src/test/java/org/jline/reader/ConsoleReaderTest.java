/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jline.Completer;
import org.jline.ConsoleReader;
import org.jline.History;
import org.jline.reader.completer.AggregateCompleter;
import org.jline.reader.completer.ArgumentCompleter;
import org.jline.reader.completer.NullCompleter;
import org.jline.reader.completer.StringsCompleter;
import org.jline.reader.history.MemoryHistory;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.junit.Before;
import org.junit.Test;

import static org.jline.reader.ConsoleReaderTest.WindowsKey.DELETE_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.END_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.ESCAPE_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.HOME_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.INSERT_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.LEFT_ARROW_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.NUMPAD_KEY_INDICATOR;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.PAGE_DOWN_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.PAGE_UP_KEY;
import static org.jline.reader.ConsoleReaderTest.WindowsKey.SPECIAL_KEY_INDICATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for the {@link ConsoleReaderImpl}.
 */
public class ConsoleReaderTest extends ReaderTestSupport
{
    
    private ByteArrayOutputStream output;

    @Before
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
        System.setProperty("input.encoding", "UTF-8");
        assertLine("\u6771\u00E9\u00E8", new TestBuffer("\u6771\u00E9\u00E8\n"));
    }
    
    @Test
    public void testReadlineWithMask() throws Exception {
        mask = '*';
        assertLine("Sample String", new TestBuffer("Sample String\n"));
        assertEquals("*************", out.toString().trim());
    }

    @Test
    public void testDeleteOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            'S', 's',
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) LEFT_ARROW_KEY.code,
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) DELETE_KEY.code, '\n'
        };
        assertLine("S", new TestBuffer(characters), false);
    }

    @Test
    public void testNumpadDeleteOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            'S', 's',
            (char) NUMPAD_KEY_INDICATOR.code,
            (char) LEFT_ARROW_KEY.code,
            (char) NUMPAD_KEY_INDICATOR.code,
            (char) DELETE_KEY.code, '\n'
        };
        assertLine("S", new TestBuffer(characters), false);
    }

    @Test
    public void testHomeKeyOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            'S', 's',
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) HOME_KEY.code, 'x', '\n'
        };
        assertLine("xSs", new TestBuffer(characters), false);

    }

    @Test
    public void testEndKeyOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            'S', 's',
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) HOME_KEY.code, 'x',
            (char) SPECIAL_KEY_INDICATOR.code, (char) END_KEY.code,
            'j', '\n'
        };
        assertLine("xSsj", new TestBuffer(characters), false);
    }

    @Test
    public void testPageUpOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) PAGE_UP_KEY.code, '\n'
        };
        assertLine("dir", new TestBuffer(characters), false);
    }

    @Test
    public void testPageDownOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) PAGE_DOWN_KEY.code, '\n'
        };
        assertLine("mkdir monkey", new TestBuffer(characters), false);
    }

    @Test
    public void testEscapeOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            's', 's', 's',
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) ESCAPE_KEY.code, '\n'
        };
        assertLine("", new TestBuffer(characters), false);
    }

    @Test
    public void testInsertOnWindowsTerminal() throws Exception {
        // test only works on Windows
//        assumeTrue(TerminalFactory.get() instanceof WindowsTerminal);

        char[] characters = new char[]{
            'o', 'p', 's',
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) HOME_KEY.code,
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) INSERT_KEY.code, 'o', 'o', 'p', 's', '\n'
        };
        assertLine("oops", new TestBuffer(characters), false);
    }

    @Test
    public void testExpansion() throws Exception {
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(3);
        history.add("foo");
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");
        reader.setHistory(history);

        assertEquals("echo a!", reader.expandEvents("echo a!"));
        assertEquals("mkdir monkey ; echo a!", reader.expandEvents("!! ; echo a!"));
        assertEquals("echo ! a", reader.expandEvents("echo ! a"));
        assertEquals("echo !\ta", reader.expandEvents("echo !\ta"));

        assertEquals("mkdir barey", reader.expandEvents("^monk^bar^"));
        assertEquals("mkdir barey", reader.expandEvents("^monk^bar"));
        assertEquals("a^monk^bar", reader.expandEvents("a^monk^bar"));

        assertEquals("mkdir monkey", reader.expandEvents("!!"));
        assertEquals("echo echo a", reader.expandEvents("echo !#a"));

        assertEquals("mkdir monkey", reader.expandEvents("!mk"));
        try {
            reader.expandEvents("!mz");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!mz: event not found", e.getMessage());
        }

        assertEquals("mkdir monkey", reader.expandEvents("!?mo"));
        assertEquals("mkdir monkey", reader.expandEvents("!?mo?"));

        assertEquals("mkdir monkey", reader.expandEvents("!-1"));
        assertEquals("cd c:\\", reader.expandEvents("!-2"));
        assertEquals("cd c:\\", reader.expandEvents("!3"));
        assertEquals("mkdir monkey", reader.expandEvents("!4"));
        try {
            reader.expandEvents("!20");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!20: event not found", e.getMessage());
        }
        try {
            reader.expandEvents("!-20");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!-20: event not found", e.getMessage());
        }
    }

    @Test
    public void testNumericExpansions() throws Exception {
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(3);

        // Seed history with three entries:
        // 1 history1
        // 2 history2
        // 3 history3
        history.add("history1");
        history.add("history2");
        history.add("history3");
        reader.setHistory(history);

        // Validate !n
        assertExpansionIllegalArgumentException(reader, "!0");
        assertEquals("history1", reader.expandEvents("!1"));
        assertEquals("history2", reader.expandEvents("!2"));
        assertEquals("history3", reader.expandEvents("!3"));
        assertExpansionIllegalArgumentException(reader, "!4");

        // Validate !-n
        assertExpansionIllegalArgumentException(reader, "!-0");
        assertEquals("history3", reader.expandEvents("!-1"));
        assertEquals("history2", reader.expandEvents("!-2"));
        assertEquals("history1", reader.expandEvents("!-3"));
        assertExpansionIllegalArgumentException(reader, "!-4");

        // Validate !!
        assertEquals("history3", reader.expandEvents("!!"));

        // Add two new entries. Because maxSize=3, history is:
        // 3 history3
        // 4 history4
        // 5 history5
        history.add("history4");
        history.add("history5");

        // Validate !n
        assertExpansionIllegalArgumentException(reader, "!0");
        assertExpansionIllegalArgumentException(reader, "!1");
        assertExpansionIllegalArgumentException(reader, "!2");
        assertEquals("history3", reader.expandEvents("!3"));
        assertEquals("history4", reader.expandEvents("!4"));
        assertEquals("history5", reader.expandEvents("!5"));
        assertExpansionIllegalArgumentException(reader, "!6");

        // Validate !-n
        assertExpansionIllegalArgumentException(reader, "!-0");
        assertEquals("history5", reader.expandEvents("!-1"));
        assertEquals("history4", reader.expandEvents("!-2"));
        assertEquals("history3", reader.expandEvents("!-3"));
        assertExpansionIllegalArgumentException(reader, "!-4");

        // Validate !!
        assertEquals("history5", reader.expandEvents("!!"));
    }

    @Test
    public void testArgsExpansion() throws Exception {
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(3);
        reader.setHistory(history);

        // we can't go back to previous arguments if there are none
        try {
            reader.expandEvents("!$");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("!$: event not found", e.getMessage());
        }

        // if no arguments were given, it should expand to the command itself
        history.add("ls");
        assertEquals("ls", reader.expandEvents("!$"));

        // now we can expand to the last argument
        history.add("ls /home");
        assertEquals("/home", reader.expandEvents("!$"));

        //we always take the last argument
        history.add("ls /home /etc");
        assertEquals("/etc", reader.expandEvents("!$"));

        //make sure we don't add spaces accidentally
        history.add("ls /home  /foo ");
        assertEquals("/foo", reader.expandEvents("!$"));
    }

    @Test
    public void testIllegalExpansionDoesntCrashReadLine() throws Exception {
        MemoryHistory history = new MemoryHistory();
        reader.setHistory(history);
        reader.setVariable(ConsoleReader.BELL_STYLE, "audible");

        assertLine("", new TestBuffer("!f\n"));
        assertEquals(0, history.size());
    }

    @Test
    public void testStoringHistory() throws Exception {
        MemoryHistory history = new MemoryHistory();
        reader.setHistory(history);

        assertLine("foo ! bar", new TestBuffer("foo ! bar\n"));

        history.previous();
        assertEquals("foo \\! bar", history.current());

        history = new MemoryHistory();
        reader.setHistory(history);
        assertLine("cd c:\\docs", new TestBuffer("cd c:\\docs\n"));

        history.previous();
        assertEquals("cd c:\\docs", history.current());
    }

    @Test
    public void testExpansionAndHistoryWithEscapes() throws Exception {

        /*
         * Tests the results of the ReaderImpl.readLine() call and the line
         * stored in history. For each input, it tests the with-expansion and
         * without-expansion case.
         */

        // \! (escaped expansion v1)
        assertLineAndHistory(
                "echo ab!ef",
                "echo ab\\!ef",
                new TestBuffer("echo ab\\!ef\n"), true, "cd");

        assertLineAndHistory(
                "echo ab\\!ef",
                "echo ab\\!ef",
                new TestBuffer("echo ab\\!ef\n"), false, "cd");

        // \!\! (escaped expansion v2)
        assertLineAndHistory(
                "echo ab!!ef",
                "echo ab\\!\\!ef",
                new TestBuffer("echo ab\\!\\!ef\n"), true, "cd");

        assertLineAndHistory(
                "echo ab\\!\\!ef",
                "echo ab\\!\\!ef",
                new TestBuffer("echo ab\\!\\!ef\n"), false, "cd");

        // !! (expansion)
        assertLineAndHistory(
                "echo abcdef",
                "echo abcdef",
                new TestBuffer("echo ab!!ef\n"), true, "cd");

        assertLineAndHistory(
                "echo ab!!ef",
                "echo ab!!ef",
                new TestBuffer("echo ab!!ef\n"), false, "cd");

        // \G (backslash no expansion)
        assertLineAndHistory(
                "echo abc\\Gdef",
                "echo abc\\Gdef",
                new TestBuffer("echo abc\\Gdef\n"), true, "cd");

        assertLineAndHistory(
                "echo abc\\Gdef",
                "echo abc\\Gdef",
                new TestBuffer("echo abc\\Gdef\n"), false, "cd");

        // \^ (escaped expansion)
        assertLineAndHistory(
                "^abc^def",
                "\\^abc^def",
                new TestBuffer("\\^abc^def\n"), true, "echo abc");

        assertLineAndHistory(
                "\\^abc^def",
                "\\^abc^def",
                new TestBuffer("\\^abc^def\n"), false, "echo abc");

        // ^^ (expansion)
        assertLineAndHistory(
                "echo def",
                "echo def",
                new TestBuffer("^abc^def\n"), true, "echo abc");

        assertLineAndHistory(
                "^abc^def",
                "^abc^def",
                new TestBuffer("^abc^def\n"), false, "echo abc");
    }

    @Test
    public void testStoringHistoryWithExpandEventsOff() throws Exception {
        assertLineAndHistory(
                "foo ! bar",
                "foo ! bar",
                new TestBuffer("foo ! bar\n"), false
        );
    }

    @Test
    public void testMacro() throws Exception {
        assertLine("foofoo", new TestBuffer("\u0018(foo\u0018)\u0018e\n"));
    }

    @Test
    public void testBell() throws Exception {
        reader.setVariable(ConsoleReader.BELL_STYLE, "off");
        reader.beep();
        assertEquals("out should not have received bell", 0, out.size());

        reader.setVariable(ConsoleReader.BELL_STYLE, "audible");
        reader.beep();
        String bellCap = console.getStringCapability(Capability.bell);
        StringWriter sw = new StringWriter();
        Curses.tputs(sw, bellCap);
        assertEquals("out should have received bell", sw.toString(), out.toString());
    }

    @Test
    public void testCallbacks() throws Exception {
        reader.addTriggeredAction('x', r -> r.getCursorBuffer().clear());
        assertLine("", new TestBuffer("sample stringx\n"));
    }

    @Test
    public void testComplete() throws Exception {
        Completer nil = new NullCompleter();
        Completer read = new StringsCompleter("read");
        Completer and = new StringsCompleter("and");
        Completer save = new StringsCompleter("save");
        Completer aggregator = new AggregateCompleter(
                new ArgumentCompleter(read, and, save, nil)
        );
        reader.addCompleter(aggregator);

        assertLine("read and ", new TestBuffer("read an\t\n"));

        assertLine("read andnd", new TestBuffer("read and\033[D\033[D\t\n"));
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

        KeyMap map = new KeyMap("custom");
        map.bind("bc", 1l);
        map.bind("e", 2l);

        Object b = reader.readBinding(map);
        assertEquals(1l, b);
        assertEquals("bc", reader.getLastBinding());
        b = reader.readBinding(map);
        assertEquals(2l, b);
        assertEquals("e", reader.getLastBinding());
        b = reader.readBinding(map);
        assertNull(b);
    }

    private History createSeededHistory() {
        History history = new MemoryHistory();
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");
        return history;
    }

    private void assertLineAndHistory(String expectedLine, String expectedHistory, TestBuffer input, boolean expandEvents, String... historyItems) {
        MemoryHistory history = new MemoryHistory();
        if (historyItems != null) {
            for (String historyItem : historyItems) {
                history.add(historyItem);
            }
        }
        reader.setHistory(history);
        reader.setVariable(ConsoleReader.DISABLE_EVENT_EXPANSION, expandEvents ? "off" : "on");
        assertLine(expectedLine, input, false);
        history.previous();
        assertEquals(expectedHistory, history.current());
    }

    /**
     * Validates that an 'event not found' IllegalArgumentException is thrown
     * for the expansion event.
     */
    protected void assertExpansionIllegalArgumentException(ConsoleReaderImpl reader, String event) throws Exception {
        try {
            reader.expandEvents(event);
            fail("Expected IllegalArgumentException for " + event);
        } catch (IllegalArgumentException e) {
            assertEquals(event + ": event not found", e.getMessage());
        }
    }

    /**
     * Windows keys.
     * <p/>
     * Constants copied <tt>wincon.h</tt>.
     */
    public enum WindowsKey
    {
        /**
         * On windows terminals, this character indicates that a 'special' key has
         * been pressed. This means that a key such as an arrow key, or delete, or
         * home, etc. will be indicated by the next character.
         */
        SPECIAL_KEY_INDICATOR(224),

        /**
         * On windows terminals, this character indicates that a special key on the
         * number pad has been pressed.
         */
        NUMPAD_KEY_INDICATOR(0),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR,
         * this character indicates an left arrow key press.
         */
        LEFT_ARROW_KEY(75),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates an
         * right arrow key press.
         */
        RIGHT_ARROW_KEY(77),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates an up
         * arrow key press.
         */
        UP_ARROW_KEY(72),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates an
         * down arrow key press.
         */
        DOWN_ARROW_KEY(80),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates that
         * the delete key was pressed.
         */
        DELETE_KEY(83),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates that
         * the home key was pressed.
         */
        HOME_KEY(71),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates that
         * the end key was pressed.
         */
        END_KEY(79),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates that
         * the page up key was pressed.
         */
        PAGE_UP_KEY(73),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates that
         * the page down key was pressed.
         */
        PAGE_DOWN_KEY(81),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR
         * this character indicates that
         * the insert key was pressed.
         */
        INSERT_KEY(82),

        /**
         * When following the SPECIAL_KEY_INDICATOR or NUMPAD_KEY_INDICATOR,
         * this character indicates that the escape key was pressed.
         */
        ESCAPE_KEY(0),;

        public final int code;

        WindowsKey(final int code) {
            this.code = code;
        }

        private static final Map<Integer, WindowsKey> codes;

        static {
            Map<Integer, WindowsKey> map = new HashMap<Integer, WindowsKey>();

            for (WindowsKey key : WindowsKey.values()) {
                map.put(key.code, key);
            }

            codes = map;
        }

        public static WindowsKey valueOf(final int code) {
            return codes.get(code);
        }
    }
}
