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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;

import org.jline.Console;
import org.jline.console.AbstractConsole;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Provides support for reader tests.
 */
public abstract class ReaderTestSupport
{
    protected Console console;
    protected ConsoleReader reader;
    protected EofPipedInputStream in;
    protected ByteArrayOutputStream out;

    @Before
    public void setUp() throws Exception {
        in = new EofPipedInputStream();
        out = new ByteArrayOutputStream();
        console = new DumbConsole(null, new URL("file:/do/not/exists"), null, in, out);
        reader = ((AbstractConsole) console).getConsoleReader();
        reader.setKeyMap(KeyMap.EMACS);
    }

    protected void assertConsoleOutputContains(String s) {
        String output = out.toString();
        assertTrue(output.contains(s));
    }

    protected void assertBeeped() throws IOException {
        String bellCap = console.getStringCapability(Capability.bell);
        StringWriter sw = new StringWriter();
        Curses.tputs(sw, bellCap);
        assertConsoleOutputContains(sw.toString());
    }

    protected void assertBuffer(final String expected, final Buffer buffer) throws IOException {
        assertBuffer(expected, buffer, true);
    }

    protected void assertBuffer(final String expected, final Buffer buffer, final boolean clear) throws IOException {
        // clear current buffer, if any
        if (clear) {
            reader.finishBuffer();
            reader.getHistory().clear();
        }

        in.setIn(new ByteArrayInputStream(buffer.getBytes()));

        // run it through the reader
        //String line;
        //while ((line = reader.readLine((String) null)) != null) {
            //System.err.println("Read line: " + line);
        while ((reader.readLine((String) null)) != null) {
            // noop
        }

        assertEquals(expected, reader.getCursorBuffer().toString());
    }

    protected void assertPosition(int pos, final Buffer buffer, final boolean clear) throws IOException {
        // clear current buffer, if any
        if (clear) {
            reader.finishBuffer();
            reader.getHistory().clear();
        }

        in.setIn(new ByteArrayInputStream(buffer.getBytes()));

        // run it through the reader
        //String line;
        //while ((line = reader.readLine((String) null)) != null) {
            //System.err.println("Read line: " + line);
        while ((reader.readLine((String) null)) != null) {
            // noop
        }

        assertEquals(pos, reader.getCursorPosition());
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
    protected void assertLine(final String expected, final Buffer buffer,
            final boolean clear) throws IOException {
        // clear current buffer, if any
        if (clear) {
            reader.finishBuffer();
            reader.getHistory().clear();
        }

        in.setIn(new ByteArrayInputStream(buffer.getBytes()));

        String line;
        String prevLine = null;
        while ((line = reader.readLine((String) null)) != null) {

            prevLine = line;
        }

        assertEquals(expected, prevLine);
    }

    private String getKeyForAction(final Operation key) {
        switch (key) {
            case BACKWARD_WORD:        return "\u001Bb";
            case BEGINNING_OF_LINE:    return "\033[H";
            case END_OF_LINE:          return "\u0005";
            case KILL_WORD:            return "\u001Bd";
            case UNIX_WORD_RUBOUT:     return "\u0017";
            case ACCEPT_LINE:          return "\n";
            case PREVIOUS_HISTORY:     return "\033[A";
            case NEXT_HISTORY:         return "\033[B";
            case BACKWARD_CHAR:        return "\u0002";
            case COMPLETE:             return "\011";
            case BACKWARD_DELETE_CHAR: return "\010";
            case VI_EOF_MAYBE:         return "\004";
            case BACKWARD_KILL_WORD:   return new String(new char[]{27, 127});
            case YANK:                 return "\u0019";
            case YANK_POP:             return new String(new char[]{27, 121});
            default:
              throw new IllegalArgumentException(key.toString());
        }
    }

    protected class Buffer
    {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public Buffer() {
            // nothing
        }

        public Buffer(final String str) {
            append(str);
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }

        public Buffer op(final Operation op) {
            return append(getKeyForAction(op));
        }

        public Buffer ctrlA() {
            return append("\001");
        }

        public Buffer ctrlD() {
            return append("\004");
        }

        /**
         * Generate a CTRL-X sequence where 'X' is the control character
         * you wish to generate.
         * @param let The letter of the control character. Valid values are
         *   'A' through 'Z'.
         * @return The modified buffer.
         */
        public Buffer ctrl(char let) {

            if (let < 'A' || let > 'Z')
                throw new RuntimeException("Cannot generate CTRL code for "
                    + "char '" + let + "' (" + ((int)let) + ")");

            int ch = (((int)let) - 'A') + 1;

            return append((char)ch);
        }

        public Buffer enter() {
            return ctrl('J');
        }

        public Buffer CR() {
        	return ctrl('M');
        }

        public Buffer ctrlU() {
            return append("\025");
        }

        public Buffer tab() {
            return op(Operation.COMPLETE);
        }

        public Buffer escape() {
            return append("\033");
        }

        public Buffer back() {
            return op(Operation.BACKWARD_DELETE_CHAR);
        }

        public Buffer back(int n) {
            for (int i = 0; i < n; i++)
                op(Operation.BACKWARD_DELETE_CHAR);
            return this;
        }

        public Buffer left() {
            return append("\033[D");
        }

        public Buffer left(int n) {
            for (int i = 0; i < n; i++)
                append("\033[D");
            return this;
        }

        public Buffer right() {
            return append("\033[C");
        }

        public Buffer right(int n) {
            for (int i = 0; i < n; i++)
                append("\033[C");
            return this;
        }

        public Buffer up() {
            return append(getKeyForAction(Operation.PREVIOUS_HISTORY));
        }

        public Buffer down() {
            return append("\033[B");
        }

        public Buffer append(final String str) {
            for (byte b : str.getBytes()) {
                append(b);
            }
            return this;
        }

        public Buffer append(final int i) {
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

}
