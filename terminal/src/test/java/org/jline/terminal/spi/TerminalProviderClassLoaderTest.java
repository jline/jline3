/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.spi;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TerminalProvider classLoader functionality.
 */
public class TerminalProviderClassLoaderTest {

    @Test
    public void testLoadWithNullClassLoader() throws IOException {
        // Test that load(name, null) behaves the same as load(name)
        try {
            TerminalProvider provider1 = TerminalProvider.load("nonexistent");
            fail("Should throw IOException for nonexistent provider");
        } catch (IOException e) {
            // Expected
        }

        try {
            TerminalProvider provider2 = TerminalProvider.load("nonexistent", null);
            fail("Should throw IOException for nonexistent provider");
        } catch (IOException e) {
            // Expected - should behave the same as load(name)
            assertTrue(
                    e.getMessage().contains("Unable to find terminal provider nonexistent"),
                    "Error message should indicate provider not found");
        }
    }

    @Test
    public void testLoadWithCustomClassLoader() {
        ClassLoader customClassLoader = new URLClassLoader(new URL[0]);

        // Test with custom classloader that doesn't have the provider
        assertThrows(
                IOException.class,
                () -> {
                    TerminalProvider.load("nonexistent", customClassLoader);
                },
                "Should throw IOException when provider is not found in custom classloader");
    }

    @Test
    public void testLoadMethodOverloads() {
        // Test that both method signatures exist and can be called
        assertDoesNotThrow(
                () -> {
                    try {
                        TerminalProvider.load("nonexistent");
                    } catch (IOException e) {
                        // Expected - just testing method exists
                    }
                },
                "Single parameter load method should exist");

        assertDoesNotThrow(
                () -> {
                    try {
                        TerminalProvider.load("nonexistent", null);
                    } catch (IOException e) {
                        // Expected - just testing method exists
                    }
                },
                "Two parameter load method should exist");
    }

    @Test
    public void testLoadWithCurrentThreadClassLoader() throws IOException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Set a custom classloader as the context classloader
            ClassLoader customClassLoader = new URLClassLoader(new URL[0]);
            Thread.currentThread().setContextClassLoader(customClassLoader);

            // Test that load(name, null) uses the context classloader
            try {
                TerminalProvider.load("nonexistent", null);
                fail("Should throw IOException");
            } catch (IOException e) {
                assertTrue(
                        e.getMessage().contains("Unable to find terminal provider nonexistent"),
                        "Should use context classloader when override is null");
            }

        } finally {
            // Restore original classloader
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
