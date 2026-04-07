/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.awt.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SwingTerminal.TerminalComponent} rendering helper methods:
 * color resolution, dim color computation, and font style resolution.
 */
class SwingTerminalRenderingTest {

    // Attribute bit masks (matching ScreenTerminal/SwingTerminal encoding)
    private static final long UNDERLINE = 0x01000000L;
    private static final long INVERSE = 0x02000000L;
    private static final long CONCEAL = 0x04000000L;
    private static final long BOLD = 0x08000000L;
    private static final long FG_SET = 0x10000000L;
    private static final long BG_SET = 0x20000000L;
    private static final long DIM = 0x40000000L;
    private static final long ITALIC = 0x80000000L;

    /**
     * Encodes foreground and background 12-bit colors with attribute flags into
     * the 32-bit attribute format used after {@code cell >>> 32}.
     */
    private static long attr(int fg, int bg, long flags) {
        // Lower 12 bits: bg, next 12 bits: fg, upper bits: flags
        return (long) bg | ((long) fg << 12) | flags;
    }

    // -----------------------------------------------------------------------
    // dimColor tests
    // -----------------------------------------------------------------------

    @Test
    void testDimColorHalvesEachChannel() {
        // White (0xfff): each 4-bit channel 0xf → 0x7
        assertEquals(0x777, SwingTerminal.TerminalComponent.dimColor(0xfff));
    }

    @Test
    void testDimColorBlackStaysBlack() {
        assertEquals(0x000, SwingTerminal.TerminalComponent.dimColor(0x000));
    }

    @Test
    void testDimColorSingleChannel() {
        // Red 0xf00 → 0x700
        assertEquals(0x700, SwingTerminal.TerminalComponent.dimColor(0xf00));
        // Green 0x0f0 → 0x070
        assertEquals(0x070, SwingTerminal.TerminalComponent.dimColor(0x0f0));
        // Blue 0x00f → 0x007
        assertEquals(0x007, SwingTerminal.TerminalComponent.dimColor(0x00f));
    }

    // -----------------------------------------------------------------------
    // resolveColors tests
    // -----------------------------------------------------------------------

    @Test
    void testResolveColorsDefaults() {
        // No flags set → default fg=0xfff (white), bg=0x000 (black)
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(0L, false, false);
        assertEquals(0x0fff, colors[0], "Default foreground should be white");
        assertEquals(0x0000, colors[1], "Default background should be black");
    }

    @Test
    void testResolveColorsExplicitFgBg() {
        // fg=0x800 (red-ish), bg=0x008 (blue-ish), both set
        long a = attr(0x800, 0x008, FG_SET | BG_SET);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, false, false);
        assertEquals(0x800, colors[0], "Foreground");
        assertEquals(0x008, colors[1], "Background");
    }

    @Test
    void testResolveColorsInverse() {
        long a = attr(0xfff, 0x000, FG_SET | BG_SET | INVERSE);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, false, false);
        assertEquals(0x000, colors[0], "Inverse: fg should be original bg");
        assertEquals(0xfff, colors[1], "Inverse: bg should be original fg");
    }

    @Test
    void testResolveColorsConcealHidesForeground() {
        long a = attr(0xfff, 0x000, FG_SET | BG_SET | CONCEAL);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, false, false);
        assertEquals(0x000, colors[0], "Concealed: fg should equal bg");
        assertEquals(0x000, colors[1]);
    }

    @Test
    void testResolveColorsDimReducesForeground() {
        // Default fg is white (0xfff), dimmed → 0x777
        long a = attr(0, 0, DIM);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, false, false);
        assertEquals(0x777, colors[0], "Dim: fg should be halved");
        assertEquals(0x000, colors[1], "Dim: bg unchanged");
    }

    @Test
    void testResolveColorsConcealPlusDim() {
        // When both conceal and dim are active, conceal wins — fg must equal bg
        long a = attr(0xfff, 0x000, FG_SET | BG_SET | CONCEAL | DIM);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, false, false);
        assertEquals(colors[1], colors[0], "Conceal+dim: fg must equal bg (conceal wins)");
        assertEquals(0x000, colors[0], "Conceal+dim: fg should be bg color, not dimmed");
    }

    @Test
    void testResolveColorsCursorOverridesAll() {
        long a = attr(0x800, 0x008, FG_SET | BG_SET | DIM | CONCEAL);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, true, true);
        assertEquals(0x000, colors[0], "Cursor: fg should be black");
        assertEquals(0x0fff, colors[1], "Cursor: bg should be white");
    }

    @Test
    void testResolveColorsCursorNotVisibleNoOverride() {
        long a = attr(0x800, 0x008, FG_SET | BG_SET);
        int[] colors = SwingTerminal.TerminalComponent.resolveColors(a, true, false);
        assertEquals(0x800, colors[0], "Invisible cursor: fg unchanged");
        assertEquals(0x008, colors[1], "Invisible cursor: bg unchanged");
    }

    // -----------------------------------------------------------------------
    // resolveFontStyle tests
    // -----------------------------------------------------------------------

    @Test
    void testResolveFontStylePlain() {
        assertEquals(Font.PLAIN, SwingTerminal.TerminalComponent.resolveFontStyle(0L));
    }

    @Test
    void testResolveFontStyleBold() {
        assertEquals(Font.BOLD, SwingTerminal.TerminalComponent.resolveFontStyle(BOLD));
    }

    @Test
    void testResolveFontStyleItalic() {
        assertEquals(Font.ITALIC, SwingTerminal.TerminalComponent.resolveFontStyle(ITALIC));
    }

    @Test
    void testResolveFontStyleBoldItalic() {
        assertEquals(Font.BOLD | Font.ITALIC, SwingTerminal.TerminalComponent.resolveFontStyle(BOLD | ITALIC));
    }

    @Test
    void testResolveFontStyleIgnoresOtherFlags() {
        // Underline, inverse, etc. should not affect font style
        assertEquals(Font.PLAIN, SwingTerminal.TerminalComponent.resolveFontStyle(UNDERLINE | INVERSE | CONCEAL));
    }
}
