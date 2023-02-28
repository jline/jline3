/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.keymap.KeyMap;
import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.junit.Before;

import static org.jline.reader.LineReader.ACCEPT_LINE;
import static org.jline.reader.LineReader.BACKWARD_CHAR;
import static org.jline.reader.LineReader.BACKWARD_DELETE_CHAR;
import static org.jline.reader.LineReader.BACKWARD_KILL_WORD;
import static org.jline.reader.LineReader.BACKWARD_WORD;
import static org.jline.reader.LineReader.BEGINNING_OF_LINE;
import static org.jline.reader.LineReader.COMPLETE_WORD;
import static org.jline.reader.LineReader.DOWN_HISTORY;
import static org.jline.reader.LineReader.END_OF_LINE;
import static org.jline.reader.LineReader.FORWARD_WORD;
import static org.jline.reader.LineReader.KILL_WORD;
import static org.jline.reader.LineReader.UP_HISTORY;
import static org.jline.reader.LineReader.YANK;
import static org.jline.reader.LineReader.YANK_POP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Provides support for reader tests.
 */
public abstract class ReaderTestSupport {
    protected Terminal terminal;
    protected TestLineReader reader;
    protected EofPipedInputStream in;
    protected ByteArrayOutputStream out;
    protected Character mask;

    @Before
    public void setUp() throws Exception {
        Handler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        Logger logger = Logger.getLogger("org.jline");
        logger.addHandler(ch);
        // Set the handler log level
        logger.setLevel(Level.INFO);

        in = new EofPipedInputStream();
        out = new ByteArrayOutputStream();
        terminal = new DumbTerminal("terminal", "ansi", in, out, StandardCharsets.UTF_8);
        terminal.setSize(new Size(160, 80));
        reader = new TestLineReader(terminal, "JLine", null);
        reader.setKeyMap(LineReaderImpl.EMACS);
        mask = null;
    }

    protected void assertConsoleOutputContains(String s) {
        String output = out.toString();
        assertTrue(output.contains(s));
    }

    protected void assertBeeped() {
        String bellCap = terminal.getStringCapability(Capability.bell);
        String s = Curses.tputs(bellCap);
        assertConsoleOutputContains(s);
    }

    protected void assertBuffer(final String expected, final TestBuffer buffer) throws IOException {
        assertBuffer(expected, buffer, true);
    }

    protected void assertBuffer(final String expected, final TestBuffer buffer, final boolean clear)
            throws IOException {
        // clear current buffer, if any
        if (clear) {
            reader.getHistory().purge();
        }
        reader.list = false;
        reader.menu = false;

        in.setIn(new ByteArrayInputStream(buffer.getBytes()));

        // run it through the reader
        // String line;
        // while ((line = reader.readLine((String) null)) != null) {
        // System.err.println("Read line: " + line);
        try {
            while (true) {
                reader.readLine(null, null, mask, null);
            }
        } catch (EndOfFileException e) {
            // noop
        }
        //        while ((reader.readLine(null, null, mask, null)) != null) {
        // noop
        //        }

        assertEquals(expected, reader.getBuffer().toString());
    }

    protected void assertLine(final String expected, final TestBuffer buffer) {
        assertLine(expected, buffer, true);
    }

    /**
     * This is used to check the contents of the last completed
     * line of input in the input buffer.
     *
     * @param expected The expected contents of the line.
     * @param buffer The buffer
     * @param clear If true, the current buffer of the reader
     *    is cleared.
     */
    protected void assertLine(final String expected, final TestBuffer buffer, final boolean clear) {
        // clear current buffer, if any
        if (clear) {
            try {
                reader.getHistory().purge();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        in.setIn(new ByteArrayInputStream(buffer.getBytes()));

        String line = null;
        try {
            while (true) {
                line = reader.readLine(null, null, mask, null);
            }
        } catch (EndOfFileException e) {
            // ignore
        }

        assertEquals(expected, line);
    }

    private String getKeyForAction(final String key) {
        switch (key) {
            case BACKWARD_WORD:
                return "\u001Bb";
            case FORWARD_WORD:
                return "\u001Bf";
            case BEGINNING_OF_LINE:
                return "\033[H";
            case END_OF_LINE:
                return "\u0005";
            case KILL_WORD:
                return "\u001Bd";
            case BACKWARD_KILL_WORD:
                return "\u0017";
            case ACCEPT_LINE:
                return "\n";
            case UP_HISTORY:
                return "\033[A";
            case DOWN_HISTORY:
                return "\033[B";
            case BACKWARD_CHAR:
                return "\u0002";
            case COMPLETE_WORD:
                return "\011";
            case BACKWARD_DELETE_CHAR:
                return "\010";
            case YANK:
                return "\u0019";
            case YANK_POP:
                return new String(new char[] {27, 121});
            default:
                throw new IllegalArgumentException(key);
        }
    }

    protected class TestBuffer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public TestBuffer() {
            // nothing
        }

        public TestBuffer(String str) {
            append(str);
        }

        public TestBuffer(char[] chars) {
            append(new String(chars));
        }

        @Override
        public String toString() {
            try {
                return out.toString(StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }

        public TestBuffer op(final String op) {
            return append(getKeyForAction(op));
        }

        public TestBuffer ctrlA() {
            return ctrl('A');
        }

        public TestBuffer ctrlD() {
            return ctrl('D');
        }

        /**
         * Generate a CTRL-X sequence where 'X' is the control character
         * you wish to generate.
         * @param let The letter of the control character. Valid values are
         *   'A' through 'Z'.
         * @return The modified buffer.
         */
        public TestBuffer ctrl(char let) {
            return append(KeyMap.ctrl(let));
        }

        public TestBuffer alt(char let) {
            return append(KeyMap.alt(let));
        }

        public TestBuffer enter() {
            return ctrl('J');
        }

        public TestBuffer CR() {
            return ctrl('M');
        }

        public TestBuffer ctrlU() {
            return ctrl('U');
        }

        public TestBuffer tab() {
            return op(COMPLETE_WORD);
        }

        public TestBuffer escape() {
            return append("\033");
        }

        public TestBuffer back() {
            return op(BACKWARD_DELETE_CHAR);
        }

        public TestBuffer back(int n) {
            for (int i = 0; i < n; i++) op(BACKWARD_DELETE_CHAR);
            return this;
        }

        public TestBuffer left() {
            return append("\033[D");
        }

        public TestBuffer left(int n) {
            for (int i = 0; i < n; i++) left();
            return this;
        }

        public TestBuffer right() {
            return append("\033[C");
        }

        public TestBuffer right(int n) {
            for (int i = 0; i < n; i++) right();
            return this;
        }

        public TestBuffer up() {
            return append(getKeyForAction(UP_HISTORY));
        }

        public TestBuffer down() {
            return append("\033[B");
        }

        public TestBuffer append(final String str) {
            for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
                append(b);
            }
            return this;
        }

        public TestBuffer append(final int i) {
            out.write((byte) i);
            return this;
        }
    }

    public static class EofPipedInputStream extends InputStream {

        private InputStream in;

        public InputStream getIn() {
            return in;
        }

        public void setIn(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            return in != null ? in.read() : -1;
        }

        @Override
        public int available() throws IOException {
            return in != null ? in.available() : 0;
        }
    }

    public static class TestLineReader extends LineReaderImpl {
        boolean list = false;
        boolean menu = false;

        public TestLineReader(Terminal terminal, String appName, Map<String, Object> variables) {
            super(terminal, appName, variables);
        }

        @Override
        protected boolean doList(
                List<Candidate> possible,
                String completed,
                boolean runLoop,
                BiFunction<CharSequence, Boolean, CharSequence> escaper) {
            list = true;
            return super.doList(possible, completed, runLoop, escaper);
        }

        @Override
        protected boolean doMenu(
                List<Candidate> possible, String completed, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
            menu = true;
            return super.doMenu(possible, completed, escaper);
        }
    }
}
