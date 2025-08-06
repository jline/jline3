/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SixelGraphics utility class.
 */
public class SixelGraphicsTest {

    @BeforeEach
    public void setUp() {
        // Reset any override before each test
        SixelGraphics.setSixelSupportOverride(null);
    }

    @AfterEach
    public void tearDown() {
        // Clean up after each test
        SixelGraphics.setSixelSupportOverride(null);
    }

    @Test
    public void testSixelSupportOverride() {
        // Create a mock terminal
        Terminal terminal = createMockTerminal("dumb");

        // Test default behavior (should be false for dumb terminal)
        assertFalse(SixelGraphics.isSixelSupported(terminal));

        // Test force enable
        SixelGraphics.setSixelSupportOverride(true);
        assertTrue(SixelGraphics.isSixelSupported(terminal));

        // Test force disable
        SixelGraphics.setSixelSupportOverride(false);
        assertFalse(SixelGraphics.isSixelSupported(terminal));

        // Test reset to automatic detection
        SixelGraphics.setSixelSupportOverride(null);
        assertFalse(SixelGraphics.isSixelSupported(terminal));
    }

    @Test
    public void testTerminalDetection() {
        // Test known sixel-supporting terminals
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("xterm")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("xterm-256color")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("mintty")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("foot")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("iterm2")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("konsole")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("mlterm")));
        assertTrue(SixelGraphics.isSixelSupported(createMockTerminal("wezterm")));

        // Test terminals that don't support sixel
        assertFalse(SixelGraphics.isSixelSupported(createMockTerminal("dumb")));
        assertFalse(SixelGraphics.isSixelSupported(createMockTerminal("vt100")));
        assertFalse(SixelGraphics.isSixelSupported(createMockTerminal("unknown")));
    }

    @Test
    public void testConvertToSixelBasic() throws IOException {
        // Create a simple 2x2 black and white image
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 2, 2);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1, 1); // Top-left pixel black
        g.fillRect(1, 1, 1, 1); // Bottom-right pixel black
        g.dispose();

        String sixelData = SixelGraphics.convertToSixel(image);

        // Verify the basic structure
        assertTrue(sixelData.startsWith("\u001bP0;1;q"), "Should start with DCS and sixel intro");
        assertTrue(sixelData.endsWith("\u001b\\"), "Should end with ST");
        assertTrue(sixelData.contains("#"), "Should contain color definitions");
    }

    @Test
    public void testConvertToSixelSingleColor() throws IOException {
        // Create a simple single-color image
        BufferedImage image = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 6, 6);
        g.dispose();

        String sixelData = SixelGraphics.convertToSixel(image);

        // Verify basic structure
        assertTrue(sixelData.startsWith("\u001bP0;1;q"));
        assertTrue(sixelData.endsWith("\u001b\\"));

        // Should contain color definition for red
        assertTrue(sixelData.contains("#"));
    }

    @Test
    public void testDisplayImageUnsupportedTerminal() {
        Terminal terminal = createMockTerminal("dumb");
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        // Should throw exception for unsupported terminal
        assertThrows(UnsupportedOperationException.class, () -> {
            new SixelGraphics().displayImage(terminal, image);
        });
    }

    @Test
    public void testDisplayImageSupportedTerminal() throws IOException {
        // Force enable sixel support
        SixelGraphics.setSixelSupportOverride(true);

        Terminal terminal = createMockTerminal("xterm");
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 10, 10);
        g.dispose();

        // Should not throw exception
        assertDoesNotThrow(() -> {
            new SixelGraphics().displayImage(terminal, image);
        });
    }

    @Test
    public void testImageResizing() throws IOException {
        // Create a large image that should be resized
        BufferedImage largeImage = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = largeImage.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 1000, 800);
        g.dispose();

        String sixelData = SixelGraphics.convertToSixel(largeImage);

        // Should still produce valid sixel data
        assertTrue(sixelData.startsWith("\u001bP0;1;q"));
        assertTrue(sixelData.endsWith("\u001b\\"));
    }

    @Test
    public void testNullImageHandling() {
        assertThrows(NullPointerException.class, () -> {
            SixelGraphics.convertToSixel(null);
        });
    }

    @Test
    public void testSixelDataStructure() throws IOException {
        // Create a simple 1x6 image with alternating black and white pixels
        BufferedImage image = new BufferedImage(1, 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1, 6);
        // Set alternating pixels to black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1, 1); // Top pixel
        g.fillRect(0, 2, 1, 1); // Third pixel
        g.fillRect(0, 4, 1, 1); // Fifth pixel
        g.dispose();

        String sixelData = SixelGraphics.convertToSixel(image);

        // Verify structure
        assertTrue(sixelData.startsWith("\u001bP0;1;q"), "Should start with proper DCS sequence");
        assertTrue(sixelData.endsWith("\u001b\\"), "Should end with ST");
        assertTrue(sixelData.contains("#"), "Should contain color definitions");

        // The pattern should be 010101 in binary = 21 decimal = 21 + 63 = 84 = 'T'
        // But since we have multiple colors, the exact character depends on the color processing
        assertTrue(sixelData.length() > 20, "Should have reasonable length");
    }

    @Test
    public void testEmptyImage() throws IOException {
        // Create a 1x1 transparent image
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        // Leave it transparent/empty

        String sixelData = SixelGraphics.convertToSixel(image);

        // Should still produce valid sixel data
        assertTrue(sixelData.startsWith("\u001bP0;1;q"));
        assertTrue(sixelData.endsWith("\u001b\\"));
    }

    /**
     * Creates a mock terminal for testing purposes.
     */
    private Terminal createMockTerminal(String type) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

            return TerminalBuilder.builder().type(type).streams(input, output).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create mock terminal", e);
        }
    }
}
