/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.util.Collections;

import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The line-editor display renders control characters as caret notation via the
 * highlighter, but that path is skipped for buffers at or above
 * {@code FEATURES_MAX_BUFFER_SIZE}. This checks that the oversized-buffer
 * fallback still neutralizes control bytes so a large pasted or replayed line
 * cannot emit escape sequences to the terminal.
 */
class HighlightedBufferEscapeTest extends ReaderTestSupport {

    private static final char ESC = 0x1b;
    private static final char BEL = 0x07;

    @Test
    void oversizedBufferDoesNotEmitRawEscapes() {
        // OSC "set window title" then enough text to exceed FEATURES_MAX_BUFFER_SIZE.
        StringBuilder payload =
                new StringBuilder().append(ESC).append("]0;pwned").append(BEL);
        while (payload.length() < 2000) {
            payload.append('a');
        }
        reader.getBuffer().write(payload.toString());

        AttributedString displayed = reader.getDisplayedBufferWithPrompts(Collections.emptyList());
        String plain = displayed.toString();

        assertFalse(plain.indexOf(ESC) >= 0, "raw ESC reached the display");
        assertFalse(plain.indexOf(BEL) >= 0, "raw BEL reached the display");
        assertTrue(plain.contains("^["), "ESC should render as caret notation");
        assertTrue(plain.contains("aaaa"), "printable text should be preserved");
    }

    @Test
    void oversizedBufferKeepsPrintableUnicode() {
        char eacute = 0x00e9;
        StringBuilder payload = new StringBuilder();
        while (payload.length() < 1500) {
            payload.append(eacute);
        }
        reader.getBuffer().write(payload.toString());

        AttributedString displayed = reader.getDisplayedBufferWithPrompts(Collections.emptyList());
        String threeEacute = new String(new char[] {eacute, eacute, eacute});
        assertTrue(displayed.toString().contains(threeEacute), "printable non-ASCII should survive");
    }
}
