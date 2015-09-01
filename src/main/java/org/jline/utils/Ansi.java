/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Ansi support.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.13
 */
public final class Ansi {

    private Ansi() {
    }

    public static String stripAnsi(String str) {
        if (str == null) return "";
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AnsiRemovalOutputStream aos = new AnsiRemovalOutputStream(baos);
            aos.write(str.getBytes());
            aos.close();
            return baos.toString();
        } catch (IOException e) {
            return str;
        }
    }

    /**
     * Simplified version of org.fusesource.jansi.AnsiOutputStream
     * that only discards recognized escape sequences.
     */
    private static class AnsiRemovalOutputStream extends FilterOutputStream {

        public AnsiRemovalOutputStream(OutputStream os) {
            super(os);
        }

        private final static int MAX_ESCAPE_SEQUENCE_LENGTH = 100;
        private byte buffer[] = new byte[MAX_ESCAPE_SEQUENCE_LENGTH];
        private int pos = 0;

        private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
        private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;
        private static final int LOOKING_FOR_NEXT_ARG = 2;
        private static final int LOOKING_FOR_STR_ARG_END = 3;
        private static final int LOOKING_FOR_INT_ARG_END = 4;
        private static final int LOOKING_FOR_OSC_COMMAND = 5;
        private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
        private static final int LOOKING_FOR_OSC_PARAM = 7;
        private static final int LOOKING_FOR_ST = 8;

        int state = LOOKING_FOR_FIRST_ESC_CHAR;

        private static final int FIRST_ESC_CHAR = 27;
        private static final int SECOND_ESC_CHAR = '[';
        private static final int SECOND_OSC_CHAR = ']';
        private static final int BEL = 7;
        private static final int SECOND_ST_CHAR = '\\';

        public void write(int data) throws IOException {
            switch (state) {
                case LOOKING_FOR_FIRST_ESC_CHAR:
                    if (data == FIRST_ESC_CHAR) {
                        buffer[pos++] = (byte) data;
                        state = LOOKING_FOR_SECOND_ESC_CHAR;
                    } else {
                        out.write(data);
                    }
                    break;

                case LOOKING_FOR_SECOND_ESC_CHAR:
                    buffer[pos++] = (byte) data;
                    if (data == SECOND_ESC_CHAR) {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else if (data == SECOND_OSC_CHAR) {
                        state = LOOKING_FOR_OSC_COMMAND;
                    } else {
                        reset(false);
                    }
                    break;

                case LOOKING_FOR_NEXT_ARG:
                    buffer[pos++] = (byte) data;
                    if ('"' == data) {
                        state = LOOKING_FOR_STR_ARG_END;
                    } else if ('0' <= data && data <= '9') {
                        state = LOOKING_FOR_INT_ARG_END;
                    } else if (data != ';' && data != '?' && data != '=') {
                        reset(true);
                    }
                    break;

                case LOOKING_FOR_INT_ARG_END:
                    buffer[pos++] = (byte) data;
                    if (!('0' <= data && data <= '9')) {
                        if (data == ';') {
                            state = LOOKING_FOR_NEXT_ARG;
                        } else {
                            reset(true);
                        }
                    }
                    break;

                case LOOKING_FOR_STR_ARG_END:
                    buffer[pos++] = (byte) data;
                    if ('"' != data) {
                        if (data == ';') {
                            state = LOOKING_FOR_NEXT_ARG;
                        } else {
                            reset(true);
                        }
                    }
                    break;

                case LOOKING_FOR_OSC_COMMAND:
                    buffer[pos++] = (byte) data;
                    if ('0' <= data && data <= '9') {
                        state = LOOKING_FOR_OSC_COMMAND_END;
                    } else {
                        reset(false);
                    }
                    break;

                case LOOKING_FOR_OSC_COMMAND_END:
                    buffer[pos++] = (byte) data;
                    if (';' == data) {
                        state = LOOKING_FOR_OSC_PARAM;
                    } else if (!('0' <= data && data <= '9')) {
                        reset(false);
                    }
                    break;

                case LOOKING_FOR_OSC_PARAM:
                    buffer[pos++] = (byte) data;
                    if (BEL == data) {
                        reset(true);
                    } else if (FIRST_ESC_CHAR == data) {
                        state = LOOKING_FOR_ST;
                    }
                    break;

                case LOOKING_FOR_ST:
                    buffer[pos++] = (byte) data;
                    if (SECOND_ST_CHAR == data) {
                        reset(true);
                    } else {
                        state = LOOKING_FOR_OSC_PARAM;
                    }
                    break;
            }

            // Is it just too long?
            if (pos >= buffer.length) {
                reset(false);
            }
        }

        /**
         * Resets all state to continue with regular parsing
         */
        private void reset(boolean valid) throws IOException {
            if (!valid) {
                out.write(buffer, 0, pos);
            }
            pos = 0;
            state = LOOKING_FOR_FIRST_ESC_CHAR;
        }

    }
}
