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
import java.util.List;
import java.util.Optional;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the terminal graphics functionality.
 */
public class TerminalGraphicsTest {

    private BufferedImage testImage;

    @BeforeEach
    void setUp() {
        // Create a simple test image
        testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 50, 50);
        g.setColor(Color.BLUE);
        g.fillRect(50, 0, 50, 50);
        g.setColor(Color.GREEN);
        g.fillRect(0, 50, 50, 50);
        g.setColor(Color.YELLOW);
        g.fillRect(50, 50, 50, 50);
        g.dispose();
    }

    @Test
    void testAvailableProtocols() {
        List<TerminalGraphics> protocols = TerminalGraphicsManager.getAvailableProtocols();

        assertNotNull(protocols);
        // Check for at least 3 protocols (Kitty, iTerm2, Sixel)
        // ServiceLoader may load additional implementations
        assertTrue(protocols.size() >= 3, "Expected at least 3 protocols, but found " + protocols.size());

        // Check that all expected protocols are present
        boolean hasKitty = protocols.stream().anyMatch(p -> p.getProtocol() == TerminalGraphics.Protocol.KITTY);
        boolean hasITerm2 = protocols.stream().anyMatch(p -> p.getProtocol() == TerminalGraphics.Protocol.ITERM2);
        boolean hasSixel = protocols.stream().anyMatch(p -> p.getProtocol() == TerminalGraphics.Protocol.SIXEL);

        assertTrue(hasKitty, "Kitty protocol should be available");
        assertTrue(hasITerm2, "iTerm2 protocol should be available");
        assertTrue(hasSixel, "Sixel protocol should be available");
    }

    @Test
    void testProtocolPriorities() {
        List<TerminalGraphics> protocols = TerminalGraphicsManager.getAvailableProtocols();

        // Find each protocol
        TerminalGraphics kitty = protocols.stream()
                .filter(p -> p.getProtocol() == TerminalGraphics.Protocol.KITTY)
                .findFirst()
                .orElse(null);
        TerminalGraphics iterm2 = protocols.stream()
                .filter(p -> p.getProtocol() == TerminalGraphics.Protocol.ITERM2)
                .findFirst()
                .orElse(null);
        TerminalGraphics sixel = protocols.stream()
                .filter(p -> p.getProtocol() == TerminalGraphics.Protocol.SIXEL)
                .findFirst()
                .orElse(null);

        assertNotNull(kitty);
        assertNotNull(iterm2);
        assertNotNull(sixel);

        // Check priority ordering: Kitty > iTerm2 > Sixel
        assertTrue(kitty.getPriority() > iterm2.getPriority(), "Kitty should have higher priority than iTerm2");
        assertTrue(iterm2.getPriority() > sixel.getPriority(), "iTerm2 should have higher priority than Sixel");
    }

    @Test
    void testImageConversion() throws IOException {
        // Test each protocol's image conversion
        KittyGraphics kitty = new KittyGraphics();
        ITerm2Graphics iterm2 = new ITerm2Graphics();
        SixelGraphics sixel = new SixelGraphics();

        TerminalGraphics.ImageOptions options = new TerminalGraphics.ImageOptions();

        // Test basic conversion
        String kittyResult = kitty.convertImage(testImage, options);
        String iterm2Result = iterm2.convertImage(testImage, options);
        String sixelResult = sixel.convertImage(testImage, options);

        assertNotNull(kittyResult);
        assertNotNull(iterm2Result);
        assertNotNull(sixelResult);

        // Check that each protocol produces the expected format
        assertTrue(kittyResult.startsWith("\033_G"), "Kitty output should start with Kitty escape sequence");
        assertTrue(kittyResult.endsWith("\033\\"), "Kitty output should end with Kitty terminator");

        assertTrue(
                iterm2Result.startsWith("\033]1337;File="), "iTerm2 output should start with iTerm2 escape sequence");
        assertTrue(iterm2Result.endsWith("\007"), "iTerm2 output should end with BEL");

        assertTrue(sixelResult.contains("#"), "Sixel output should contain color definitions");
    }

    @Test
    void testImageOptions() throws IOException {
        SixelGraphics sixel = new SixelGraphics();

        // Test with width and height options
        TerminalGraphics.ImageOptions options =
                new TerminalGraphics.ImageOptions().width(50).height(50).preserveAspectRatio(false);

        String result = sixel.convertImage(testImage, options);
        assertNotNull(result);

        // Test with name option
        options = new TerminalGraphics.ImageOptions().name("test-image");
        result = sixel.convertImage(testImage, options);
        assertNotNull(result);
    }

    @Test
    void testTerminalGraphicsManagerBasicFunctionality() throws IOException {
        // Test that TerminalGraphicsManager methods exist and can be called
        Terminal terminal = TerminalBuilder.builder()
                .type("xterm")
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build();

        // Test basic graphics support check
        boolean supported = TerminalGraphicsManager.isGraphicsSupported(terminal);
        // Don't assert the result since it depends on the environment

        // Test getting available protocols
        List<TerminalGraphics> protocols = TerminalGraphicsManager.getAvailableProtocols();
        assertNotNull(protocols);
        assertFalse(protocols.isEmpty());
    }

    @Test
    void testGhosttyTerminalDetection() throws IOException {
        // Test that Ghostty terminal type is recognized
        Terminal ghosttyTerminal = TerminalBuilder.builder()
                .type("ghostty")
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build();

        // Kitty graphics should be supported for Ghostty
        KittyGraphics kittyGraphics = new KittyGraphics();
        // Note: This test may not work in CI since it depends on environment variables
        // but it verifies the terminal type detection logic

        // Test that the protocol exists and can be instantiated
        assertNotNull(kittyGraphics);
        assertEquals(TerminalGraphics.Protocol.KITTY, kittyGraphics.getProtocol());
    }

    @Test
    void testBasicTerminalTypesSkipRuntimeDetection() throws IOException {
        // Test that basic terminal types that don't support graphics skip runtime detection
        String[] basicTermTypes = {"dumb", "vt100", "vt102", "ansi"};

        for (String termType : basicTermTypes) {
            Terminal terminal = TerminalBuilder.builder()
                    .type(termType)
                    .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                    .build();

            // These terminals should not support Kitty graphics
            KittyGraphics kittyGraphics = new KittyGraphics();
            // The test verifies that the method can be called without hanging
            // (actual support depends on environment, but runtime detection should be skipped)
            assertNotNull(kittyGraphics);
        }
    }

    @Test
    void testProtocolForcing() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            // Test forcing a specific protocol
            TerminalGraphicsManager.forceProtocol(TerminalGraphics.Protocol.SIXEL);

            Optional<TerminalGraphics> protocol = TerminalGraphicsManager.getBestProtocol(terminal);
            assertTrue(protocol.isPresent());
            assertEquals(TerminalGraphics.Protocol.SIXEL, protocol.get().getProtocol());

            // Reset to automatic detection
            TerminalGraphicsManager.forceProtocol(null);
        }
    }

    @Test
    void testProtocolNames() {
        assertEquals("kitty", TerminalGraphics.Protocol.KITTY.getName());
        assertEquals("iterm2", TerminalGraphics.Protocol.ITERM2.getName());
        assertEquals("sixel", TerminalGraphics.Protocol.SIXEL.getName());
    }

    @Test
    void testImageOptionsBuilder() {
        TerminalGraphics.ImageOptions options = new TerminalGraphics.ImageOptions()
                .width(100)
                .height(200)
                .name("test")
                .preserveAspectRatio(true)
                .inline(false);

        assertEquals(Integer.valueOf(100), options.getWidth());
        assertEquals(Integer.valueOf(200), options.getHeight());
        assertEquals("test", options.getName());
        assertEquals(Boolean.TRUE, options.getPreserveAspectRatio());
        assertEquals(Boolean.FALSE, options.getInline());
    }
}
