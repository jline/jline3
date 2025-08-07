/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.jline.jansi.AnsiColors;
import org.jline.jansi.AnsiMode;
import org.jline.jansi.AnsiType;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * A ANSI print stream extracts ANSI escape codes written to
 * an output stream and calls corresponding <code>AnsiProcessor.process*</code> methods.
 * This particular class is not synchronized for improved performances.
 *
 * <p>For more information about ANSI escape codes, see
 * <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">Wikipedia article</a>
 *
 * @since 1.0
 * @see AnsiProcessor
 */
public class AnsiOutputStream extends FilterOutputStream {

    public static final byte[] RESET_CODE = "\033[0m".getBytes(US_ASCII);

    @FunctionalInterface
    public interface IoRunnable {
        void run() throws IOException;
    }

    @FunctionalInterface
    public interface WidthSupplier {
        int getTerminalWidth();
    }

    public static class ZeroWidthSupplier implements WidthSupplier {
        /**
         * Creates a new ZeroWidthSupplier.
         */
        public ZeroWidthSupplier() {
            // Default constructor
        }

        @Override
        public int getTerminalWidth() {
            return 0;
        }
    }

    private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
    private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;
    private static final int LOOKING_FOR_NEXT_ARG = 2;
    private static final int LOOKING_FOR_STR_ARG_END = 3;
    private static final int LOOKING_FOR_INT_ARG_END = 4;
    private static final int LOOKING_FOR_OSC_COMMAND = 5;
    private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
    private static final int LOOKING_FOR_OSC_PARAM = 7;
    private static final int LOOKING_FOR_ST = 8;
    private static final int LOOKING_FOR_CHARSET = 9;

    private static final int FIRST_ESC_CHAR = 27;
    private static final int SECOND_ESC_CHAR = '[';
    private static final int SECOND_OSC_CHAR = ']';
    private static final int BEL = 7;
    private static final int SECOND_ST_CHAR = '\\';
    private static final int SECOND_CHARSET0_CHAR = '(';
    private static final int SECOND_CHARSET1_CHAR = ')';

    private AnsiProcessor ap;
    private static final int MAX_ESCAPE_SEQUENCE_LENGTH = 100;
    private final byte[] buffer = new byte[MAX_ESCAPE_SEQUENCE_LENGTH];
    private int pos = 0;
    private int startOfValue;
    private final ArrayList<Object> options = new ArrayList<>();
    private int state = LOOKING_FOR_FIRST_ESC_CHAR;
    private final Charset cs;

    private final WidthSupplier width;
    private final AnsiProcessor processor;
    private final AnsiType type;
    private final AnsiColors colors;
    private final IoRunnable installer;
    private final IoRunnable uninstaller;
    private AnsiMode mode;
    private boolean resetAtUninstall;

    public AnsiOutputStream(
            OutputStream os,
            WidthSupplier width,
            AnsiMode mode,
            AnsiProcessor processor,
            AnsiType type,
            AnsiColors colors,
            Charset cs,
            IoRunnable installer,
            IoRunnable uninstaller,
            boolean resetAtUninstall) {
        super(os);
        this.width = width;
        this.processor = processor;
        this.type = type;
        this.colors = colors;
        this.installer = installer;
        this.uninstaller = uninstaller;
        this.resetAtUninstall = resetAtUninstall;
        this.cs = cs;
        setMode(mode);
    }

    public int getTerminalWidth() {
        return width.getTerminalWidth();
    }

    public AnsiType getType() {
        return type;
    }

    public AnsiColors getColors() {
        return colors;
    }

    public AnsiMode getMode() {
        return mode;
    }

    public final void setMode(AnsiMode mode) {
        ap = mode == AnsiMode.Strip
                ? new AnsiProcessor(out)
                : mode == AnsiMode.Force || processor == null ? new ColorsAnsiProcessor(out, colors) : processor;
        this.mode = mode;
    }

    public boolean isResetAtUninstall() {
        return resetAtUninstall;
    }

    public void setResetAtUninstall(boolean resetAtUninstall) {
        this.resetAtUninstall = resetAtUninstall;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
                } else if (data == SECOND_CHARSET0_CHAR) {
                    options.add(0);
                    state = LOOKING_FOR_CHARSET;
                } else if (data == SECOND_CHARSET1_CHAR) {
                    options.add(1);
                    state = LOOKING_FOR_CHARSET;
                } else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_NEXT_ARG:
                buffer[pos++] = (byte) data;
                if ('"' == data) {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_STR_ARG_END;
                } else if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_INT_ARG_END;
                } else if (';' == data) {
                    options.add(null);
                } else if ('?' == data) {
                    options.add('?');
                } else if ('=' == data) {
                    options.add('=');
                } else {
                    processEscapeCommand(data);
                }
                break;
            default:
                break;

            case LOOKING_FOR_INT_ARG_END:
                buffer[pos++] = (byte) data;
                if (!('0' <= data && data <= '9')) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue);
                    Integer value = Integer.valueOf(strValue);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else {
                        processEscapeCommand(data);
                    }
                }
                break;

            case LOOKING_FOR_STR_ARG_END:
                buffer[pos++] = (byte) data;
                if ('"' != data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue, cs);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else {
                        processEscapeCommand(data);
                    }
                }
                break;

            case LOOKING_FOR_OSC_COMMAND:
                buffer[pos++] = (byte) data;
                if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_OSC_COMMAND_END;
                } else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_COMMAND_END:
                buffer[pos++] = (byte) data;
                if (';' == data) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue);
                    Integer value = Integer.valueOf(strValue);
                    options.add(value);
                    startOfValue = pos;
                    state = LOOKING_FOR_OSC_PARAM;
                } else if ('0' <= data && data <= '9') {
                    // already pushed digit to buffer, just keep looking
                } else {
                    // oops, did not expect this
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_PARAM:
                buffer[pos++] = (byte) data;
                if (BEL == data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue, cs);
                    options.add(value);
                    processOperatingSystemCommand();
                } else if (FIRST_ESC_CHAR == data) {
                    state = LOOKING_FOR_ST;
                } else {
                    // just keep looking while adding text
                }
                break;

            case LOOKING_FOR_ST:
                buffer[pos++] = (byte) data;
                if (SECOND_ST_CHAR == data) {
                    String value = new String(buffer, startOfValue, (pos - 2) - startOfValue, cs);
                    options.add(value);
                    processOperatingSystemCommand();
                } else {
                    state = LOOKING_FOR_OSC_PARAM;
                }
                break;

            case LOOKING_FOR_CHARSET:
                options.add((char) data);
                processCharsetSelect();
                break;
        }

        // Is it just too long?
        if (pos >= buffer.length) {
            reset(false);
        }
    }

    private void processCharsetSelect() throws IOException {
        try {
            reset(ap != null && ap.processCharsetSelect(options));
        } catch (RuntimeException e) {
            reset(true);
            throw e;
        }
    }

    private void processOperatingSystemCommand() throws IOException {
        try {
            reset(ap != null && ap.processOperatingSystemCommand(options));
        } catch (RuntimeException e) {
            reset(true);
            throw e;
        }
    }

    private void processEscapeCommand(int data) throws IOException {
        try {
            reset(ap != null && ap.processEscapeCommand(options, data));
        } catch (RuntimeException e) {
            reset(true);
            throw e;
        }
    }

    /**
     * Resets all state to continue with regular parsing
     * @param skipBuffer if current buffer should be skipped or written to out
     * @throws IOException
     */
    private void reset(boolean skipBuffer) throws IOException {
        if (!skipBuffer) {
            out.write(buffer, 0, pos);
        }
        pos = 0;
        startOfValue = 0;
        options.clear();
        state = LOOKING_FOR_FIRST_ESC_CHAR;
    }

    public void install() throws IOException {
        if (installer != null) {
            installer.run();
        }
    }

    public void uninstall() throws IOException {
        if (resetAtUninstall && type != AnsiType.Redirected && type != AnsiType.Unsupported) {
            setMode(AnsiMode.Default);
            write(RESET_CODE);
            flush();
        }
        if (uninstaller != null) {
            uninstaller.run();
        }
    }

    @Override
    public void close() throws IOException {
        uninstall();
        super.close();
    }
}
