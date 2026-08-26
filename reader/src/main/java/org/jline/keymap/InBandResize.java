/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.keymap;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

/**
 * Shared utility for handling DEC private mode 2048 (in-band window resize
 * notifications).
 *
 * <p>When mode 2048 is active, the terminal sends
 * {@code CSI 48 ; rows ; cols ; pixelHeight ; pixelWidth t} whenever the
 * window is resized.  The {@link KeyMap} binding consumes the
 * {@code CSI 48 ;} prefix ({@link #RESIZE_SEQ}); the methods in this class
 * handle the remainder of the sequence.</p>
 *
 * <p>This was originally implemented inside
 * {@code org.jline.reader.impl.LineReaderImpl} but is extracted here so that
 * full-screen builtins ({@code Nano}, {@code Less}, {@code Tmux}) — which
 * bypass {@code LineReader} and read input directly via
 * {@link BindingReader} — can also handle in-band resize reports.</p>
 *
 * @see Terminal#trackInBandResize(boolean)
 */
public final class InBandResize {

    /**
     * The CSI prefix that starts an in-band resize report.
     * Bind this in a {@link KeyMap} to trigger resize handling.
     */
    public static final String RESIZE_SEQ = "\033[48;";

    private InBandResize() {}

    /**
     * Reads resize parameters from the input until the final {@code t} byte.
     *
     * <p>The {@link KeyMap} match has already consumed the {@link #RESIZE_SEQ}
     * prefix.  This method reads the remaining characters — digits and
     * semicolons forming {@code rows;cols[;pixelHeight;pixelWidth]} — up to
     * the terminating {@code t}.</p>
     *
     * @param reader the binding reader to read characters from
     * @return the parameter string (e.g. {@code "24;80"}), or {@code null}
     *         if the sequence is malformed or too long
     */
    public static String readResizeParams(BindingReader reader) {
        StringBuilder sb = new StringBuilder();
        boolean discard = false;
        int c;
        while ((c = reader.readCharacter()) >= 0) {
            if (c == 't') {
                return discard ? null : sb.toString();
            }
            if (discard) {
                continue;
            }
            if (!isParamChar(c) || sb.length() > MAX_PARAM_LENGTH) {
                discard = true;
                continue;
            }
            sb.append((char) c);
        }
        return null;
    }

    private static final int MAX_PARAM_LENGTH = 50;

    private static boolean isParamChar(int c) {
        return (c >= '0' && c <= '9') || c == ';';
    }

    /**
     * Parses {@code rows;cols[;pixelHeight;pixelWidth]} and applies the new
     * terminal size, raising {@link Terminal.Signal#WINCH}.
     *
     * <p>The pixel dimensions (if present) are currently ignored — only the
     * character-cell dimensions are applied.</p>
     *
     * @param params the parameter string returned by {@link #readResizeParams}
     * @param terminal the terminal whose size should be updated
     */
    public static void applyResizeParams(String params, Terminal terminal) {
        String[] parts = params.split(";", -1);
        if (parts.length < 2) {
            return;
        }
        try {
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            if (rows > 0 && cols > 0) {
                terminal.setSize(Size.of(cols, rows));
                terminal.raise(Terminal.Signal.WINCH);
            }
        } catch (NumberFormatException e) {
            // Ignore malformed resize reports
        }
    }

    /**
     * Convenience method that reads and applies an in-band resize report
     * in a single call.
     *
     * @param reader the binding reader to read characters from
     * @param terminal the terminal whose size should be updated
     */
    public static void handleResize(BindingReader reader, Terminal terminal) {
        String params = readResizeParams(reader);
        if (params != null) {
            applyResizeParams(params, terminal);
        }
    }
}
