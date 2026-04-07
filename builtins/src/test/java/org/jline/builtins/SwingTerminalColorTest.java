/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SwingTerminal.TerminalComponent#getAnsiColor(int)}.
 */
class SwingTerminalColorTest {

    @Test
    void testBlack() {
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0x000);
        assertEquals(0, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    void testWhite() {
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0xFFF);
        assertEquals(255, c.getRed());
        assertEquals(255, c.getGreen());
        assertEquals(255, c.getBlue());
    }

    @Test
    void testPureRed() {
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0xF00);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    void testPureGreen() {
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0x0F0);
        assertEquals(0, c.getRed());
        assertEquals(255, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    void testPureBlue() {
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0x00F);
        assertEquals(0, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(255, c.getBlue());
    }

    @Test
    void testMidGray() {
        // 0x888 -> each nibble 0x8 -> (0x8 << 4) | 0x8 = 0x88 = 136
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0x888);
        assertEquals(0x88, c.getRed());
        assertEquals(0x88, c.getGreen());
        assertEquals(0x88, c.getBlue());
    }

    @Test
    void testNibbleExpansion() {
        // Verify that each nibble value 0x0–0xF expands correctly to 0x00–0xFF
        for (int n = 0; n <= 0xF; n++) {
            int packed = (n << 8) | (n << 4) | n;
            Color c = SwingTerminal.TerminalComponent.getAnsiColor(packed);
            int expected = (n << 4) | n;
            assertEquals(expected, c.getRed(), "nibble " + n + " red");
            assertEquals(expected, c.getGreen(), "nibble " + n + " green");
            assertEquals(expected, c.getBlue(), "nibble " + n + " blue");
        }
    }

    @Test
    void testMixedColor() {
        // 0xA3C -> R=0xA, G=0x3, B=0xC
        Color c = SwingTerminal.TerminalComponent.getAnsiColor(0xA3C);
        assertEquals(0xAA, c.getRed());
        assertEquals(0x33, c.getGreen());
        assertEquals(0xCC, c.getBlue());
    }
}
