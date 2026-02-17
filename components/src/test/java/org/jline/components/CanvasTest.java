/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.List;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanvasTest {

    @Test
    void testCreateCanvas() {
        Canvas canvas = Canvas.create(10, 5);
        assertEquals(10, canvas.getWidth());
        assertEquals(5, canvas.getHeight());
    }

    @Test
    void testEmptyCanvasToLines() {
        Canvas canvas = Canvas.create(5, 3);
        List<AttributedString> lines = canvas.toLines();
        assertEquals(3, lines.size());
        for (AttributedString line : lines) {
            assertEquals("     ", line.toString());
        }
    }

    @Test
    void testPutChar() {
        Canvas canvas = Canvas.create(5, 3);
        canvas.put(1, 0, 'X', AttributedStyle.DEFAULT);
        List<AttributedString> lines = canvas.toLines();
        assertEquals(" X   ", lines.get(0).toString());
    }

    @Test
    void testTextRendering() {
        Canvas canvas = Canvas.create(10, 1);
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("Hello");
        canvas.text(2, 0, sb.toAttributedString());
        List<AttributedString> lines = canvas.toLines();
        assertEquals("  Hello   ", lines.get(0).toString());
    }

    @Test
    void testFill() {
        Canvas canvas = Canvas.create(5, 3);
        canvas.fill(1, 0, 3, 2, '#', AttributedStyle.DEFAULT);
        List<AttributedString> lines = canvas.toLines();
        assertEquals(" ### ", lines.get(0).toString());
        assertEquals(" ### ", lines.get(1).toString());
        assertEquals("     ", lines.get(2).toString());
    }

    @Test
    void testSubRegionClipping() {
        Canvas canvas = Canvas.create(10, 5);
        Canvas sub = canvas.subRegion(2, 1, 4, 3);
        assertEquals(4, sub.getWidth());
        assertEquals(3, sub.getHeight());

        sub.put(0, 0, 'A', AttributedStyle.DEFAULT);
        sub.put(3, 2, 'B', AttributedStyle.DEFAULT);

        List<AttributedString> lines = canvas.toLines();
        assertEquals('A', lines.get(1).charAt(2));
        assertEquals('B', lines.get(3).charAt(5));
    }

    @Test
    void testSubRegionOutOfBounds() {
        Canvas canvas = Canvas.create(10, 5);
        Canvas sub = canvas.subRegion(2, 1, 4, 3);
        // These should be silently clipped
        sub.put(-1, 0, 'X', AttributedStyle.DEFAULT);
        sub.put(4, 0, 'X', AttributedStyle.DEFAULT);
        sub.put(0, -1, 'X', AttributedStyle.DEFAULT);
        sub.put(0, 3, 'X', AttributedStyle.DEFAULT);

        // Original canvas should be unchanged where out-of-bounds
        List<AttributedString> lines = canvas.toLines();
        for (AttributedString line : lines) {
            assertEquals("          ", line.toString());
        }
    }

    @Test
    void testZeroSizeCanvas() {
        Canvas canvas = Canvas.create(0, 0);
        assertEquals(0, canvas.getWidth());
        assertEquals(0, canvas.getHeight());
        assertTrue(canvas.toLines().isEmpty());
    }

    @Test
    void testTextClipping() {
        Canvas canvas = Canvas.create(5, 1);
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("LongText");
        canvas.text(3, 0, sb.toAttributedString());
        List<AttributedString> lines = canvas.toLines();
        assertEquals("   Lo", lines.get(0).toString());
    }
}
