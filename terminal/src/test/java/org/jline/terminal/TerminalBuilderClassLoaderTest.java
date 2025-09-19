/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TerminalBuilder classLoader functionality.
 */
public class TerminalBuilderClassLoaderTest {

    @Test
    public void testClassLoaderMethodChaining() {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        TerminalBuilder builder = TerminalBuilder.builder();
        TerminalBuilder result = builder.classLoader(customClassLoader);

        // Should return the same builder instance for method chaining
        assertSame(builder, result, "classLoader() should return the same builder instance for method chaining");
    }

    @Test
    public void testClassLoaderWithNull() {
        TerminalBuilder builder = TerminalBuilder.builder();

        // Should not throw exception when setting null classloader
        assertDoesNotThrow(() -> builder.classLoader(null), "Setting null classloader should not throw exception");
    }

    @Test
    public void testClassLoaderIntegrationWithDumbTerminal() throws IOException {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        // Create a dumb terminal with custom classloader - should work without issues
        Terminal terminal = TerminalBuilder.builder()
                .classLoader(customClassLoader)
                .dumb(true)
                .build();

        assertNotNull(terminal, "Terminal should be created successfully with custom classloader");
        assertTrue(terminal.getClass().getSimpleName().contains("Dumb"), "Should create a dumb terminal");

        terminal.close();
    }

    @Test
    public void testClassLoaderBuilderPattern() throws IOException {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        // Test that classLoader can be used in builder pattern with other methods
        Terminal terminal = TerminalBuilder.builder()
                .name("TestTerminal")
                .classLoader(customClassLoader)
                .dumb(true)
                .build();

        assertNotNull(terminal, "Terminal should be created successfully");
        assertEquals("TestTerminal", terminal.getName(), "Terminal name should be set correctly");

        terminal.close();
    }

    @Test
    public void testClassLoaderWithSystemFalse() throws IOException {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        // Test with system(false) - should create virtual terminal
        // When using system(false), we need to provide explicit streams
        Terminal terminal = TerminalBuilder.builder()
                .classLoader(customClassLoader)
                .system(false)
                .streams(new java.io.ByteArrayInputStream(new byte[0]), new java.io.ByteArrayOutputStream())
                .build();

        assertNotNull(terminal, "Terminal should be created successfully");

        terminal.close();
    }

    @Test
    public void testClassLoaderWithMultipleBuilders() {
        ClassLoader customClassLoader1 = new URLClassLoader(new URL[0]);
        ClassLoader customClassLoader2 = new URLClassLoader(new URL[0]);

        // Test that different builders can have different classloaders
        TerminalBuilder builder1 = TerminalBuilder.builder().classLoader(customClassLoader1);
        TerminalBuilder builder2 = TerminalBuilder.builder().classLoader(customClassLoader2);

        // Builders should be independent
        assertNotSame(builder1, builder2, "Different builders should be different instances");
    }

    @Test
    public void testClassLoaderBuilderReuse() {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        TerminalBuilder builder = TerminalBuilder.builder();

        // Test that we can call classLoader multiple times
        TerminalBuilder result1 = builder.classLoader(customClassLoader);
        TerminalBuilder result2 = builder.classLoader(null);
        TerminalBuilder result3 = builder.classLoader(customClassLoader);

        // All should return the same builder instance
        assertSame(builder, result1, "First classLoader call should return same builder");
        assertSame(builder, result2, "Second classLoader call should return same builder");
        assertSame(builder, result3, "Third classLoader call should return same builder");
    }

    @Test
    public void testClassLoaderWithSystemProperty() throws IOException {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        // Test that classLoader works with system property override
        String originalProvider = System.getProperty("org.jline.terminal.provider");
        try {
            System.setProperty("org.jline.terminal.provider", "dumb");

            Terminal terminal =
                    TerminalBuilder.builder().classLoader(customClassLoader).build();

            assertNotNull(terminal, "Terminal should be created with system property override");
            terminal.close();
        } finally {
            if (originalProvider != null) {
                System.setProperty("org.jline.terminal.provider", originalProvider);
            } else {
                System.clearProperty("org.jline.terminal.provider");
            }
        }
    }
}
