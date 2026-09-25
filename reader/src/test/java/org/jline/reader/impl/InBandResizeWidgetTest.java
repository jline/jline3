/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.terminal.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the {@code terminal-resize} widget that parses
 * in-band resize reports ({@code CSI 48 ; rows ; cols [; pixelH ; pixelW] t})
 * and updates the terminal size.
 */
class InBandResizeWidgetTest extends ReaderTestSupport {

    @Test
    void resizeSequenceUpdatesSizeAndCompletesLine() {
        // CSI 48;30;100;0;0t followed by "ok" + Enter
        assertLine("ok", new TestBuffer("\033[48;30;100;0;0tok\n"));
        assertEquals(Size.of(100, 30), terminal.getSize());
    }

    @Test
    void resizeSequenceWithRowsAndColsOnly() {
        assertLine("hi", new TestBuffer("\033[48;50;120thi\n"));
        assertEquals(Size.of(120, 50), terminal.getSize());
    }

    @Test
    void malformedResizeSequenceDoesNotCrash() {
        // Invalid character 'x' in the sequence — should be drained to 't' and discarded
        assertLine("ab", new TestBuffer("\033[48;10x30;80tab\n"));
        // Size should remain unchanged (160x80 from setUp)
        assertEquals(Size.of(160, 80), terminal.getSize());
    }

    @Test
    void tooLongResizeSequenceIsDrained() {
        // Build a sequence with > 50 chars of parameters
        StringBuilder sb = new StringBuilder("\033[48;");
        for (int i = 0; i < 60; i++) {
            sb.append('1');
        }
        sb.append("tcd\n");
        assertLine("cd", new TestBuffer(sb.toString()));
        // Size should remain unchanged
        assertEquals(Size.of(160, 80), terminal.getSize());
    }

    @Test
    void multipleResizeSequencesApplyLast() {
        // Two resize sequences: first 30x100, then 40x200
        assertLine("z", new TestBuffer("\033[48;30;100;0;0t\033[48;40;200;0;0tz\n"));
        assertEquals(Size.of(200, 40), terminal.getSize());
    }
}
