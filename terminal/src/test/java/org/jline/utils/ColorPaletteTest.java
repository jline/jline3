/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.InfoCmp.Capability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ColorPaletteTest {

    /**
     * Custom DumbTerminal for testing that returns specific default colors
     */
    private static class TestDumbTerminal extends DumbTerminal {
        private final int defaultForeground;
        private final int defaultBackground;

        TestDumbTerminal(
                String name,
                String type,
                InputStream in,
                OutputStream out,
                Charset encoding,
                int defaultForeground,
                int defaultBackground)
                throws IOException {
            super(name, type, in, out, encoding);
            this.defaultForeground = defaultForeground;
            this.defaultBackground = defaultBackground;
        }

        @Override
        public int getDefaultForegroundColor() {
            return defaultForeground;
        }

        @Override
        public int getDefaultBackgroundColor() {
            return defaultBackground;
        }
    }

    @Test
    void testDefaultColors() throws IOException {
        // Create a mock terminal that returns specific default colors
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        Terminal terminal =
                new TestDumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8, 0xFF0000, 0x0000FF);

        ColorPalette palette = new ColorPalette(terminal);

        // Test default foreground color
        int foreground = palette.getDefaultForeground();
        assertEquals(0xFF0000, foreground, "Default foreground color should be red");

        // Test default background color
        int background = palette.getDefaultBackground();
        assertEquals(0x0000FF, background, "Default background color should be blue");
    }

    @Test
    void testNoDefaultColors() throws IOException {
        // Create a mock terminal that doesn't return default colors
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        Terminal terminal = new TestDumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8, -1, -1);

        ColorPalette palette = new ColorPalette(terminal);

        // Test default foreground color
        int foreground = palette.getDefaultForeground();
        assertEquals(-1, foreground, "Default foreground color should be -1 when not available");

        // Test default background color
        int background = palette.getDefaultBackground();
        assertEquals(-1, background, "Default background color should be -1 when not available");
    }

    /**
     * When the terminal never answers OSC 4 (e.g. no tty), {@code loadPalette()} must return
     * {@code false} and the palette must fall back to the standard 256-colour default — not a
     * single-entry all-black palette that would map every colour to black.
     * <p>
     * Regression test for https://github.com/jline/jline3/issues/2257
     */
    @Test
    void testLoadPaletteWithSilentTerminalFallsBackToDefaultPalette() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // Empty input → terminal never answers OSC 4
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        Terminal terminal = new TestDumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8, -1, -1);

        ColorPalette palette = new ColorPalette(terminal);
        boolean loaded = palette.loadPalette();

        assertFalse(loaded, "loadPalette() must return false when the terminal does not answer OSC 4");
        assertFalse(palette.isReal(), "isReal() must be false when no OSC 4 response was received");
        assertTrue(
                palette.getLength() > 1,
                "Palette must fall back to the default 256-colour table, not a single black entry; got length="
                        + palette.getLength());
        // Sanity-check: orange (255,128,0) should NOT round to colour index 0 (black)
        int rounded = palette.round(255, 128, 0);
        assertNotEquals(0, rounded, "Orange must not round to black in the fallback palette");
    }

    /**
     * When the terminal's reader has been explicitly closed, {@code loadPalette()} must not throw a
     * {@code ClosedException} — it must gracefully fall back to the default palette.
     * <p>
     * Regression test for https://github.com/jline/jline3/issues/2257 (ClosedException case)
     */
    @Test
    void testLoadPaletteWithClosedReaderDoesNotThrow() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        Terminal terminal = new TestDumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8, -1, -1);
        // Explicitly close the reader so that peek() throws ClosedException
        terminal.reader().close();

        ColorPalette palette = new ColorPalette(terminal);
        // Must not throw ClosedException
        assertDoesNotThrow(() -> palette.loadPalette(), "loadPalette() must not throw when the reader is closed");
        assertFalse(palette.isReal(), "isReal() must be false when the reader is closed");
    }

    @Test
    void testTerminalConvenienceMethods() throws IOException {
        // Create a mock terminal that returns specific default colors
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        Terminal terminal =
                new TestDumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8, 0xFF0000, 0x0000FF);

        // Test convenience methods
        int foreground = terminal.getDefaultForegroundColor();
        assertEquals(0xFF0000, foreground, "Default foreground color should be red");

        int background = terminal.getDefaultBackgroundColor();
        assertEquals(0x0000FF, background, "Default background color should be blue");
    }

    /**
     * Regression test for https://github.com/jline/jline3/issues/2256.
     *
     * <p>The palette was sized from an empty capability map during the AbstractTerminal
     * constructor (before parseInfoCmp() ran), so it always had 256 entries regardless
     * of the terminal's actual max_colors. The fix reloads the palette at the end of
     * parseInfoCmp() and setEnv() once max_colors is known.
     *
     * <p>Only terminal types with built-in JLine capability data are used here
     * (xterm=8, xterm-256color=256), as types without built-in data fall back to
     * the "ansi" defaults and produce different max_colors values.
     */
    @ParameterizedTest
    @CsvSource({"xterm, 8", "xterm-256color, 256"})
    void testPaletteSizedFromMaxColors(String termType, int expectedLength) throws IOException {
        // Keep the write end open so the read end never sees EOF (required by TerminalBuilder).
        PipedOutputStream writer = new PipedOutputStream();
        try (Terminal t = TerminalBuilder.builder()
                .system(false)
                .type(termType)
                .streams(new PipedInputStream(writer), new ByteArrayOutputStream())
                .build()) {
            int maxColors = t.getNumericCapability(Capability.max_colors);
            int paletteLength = t.getPalette().getLength();
            assertEquals(expectedLength, maxColors, "max_colors for " + termType + " should be " + expectedLength);
            assertEquals(
                    expectedLength,
                    paletteLength,
                    "palette length for " + termType
                            + " should match max_colors; was palette sized before parseInfoCmp()?");
        }
    }
}
