/*
 * Copyright (c) 2002-2025, the original author(s).
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColorPaletteTest {

    /**
     * Custom DumbTerminal for testing that returns specific default colors
     */
    private static class TestDumbTerminal extends DumbTerminal {
        private final int defaultForeground;
        private final int defaultBackground;

        public TestDumbTerminal(
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
    public void testDefaultColors() throws IOException {
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
    public void testNoDefaultColors() throws IOException {
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

    @Test
    public void testTerminalConvenienceMethods() throws IOException {
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
}
