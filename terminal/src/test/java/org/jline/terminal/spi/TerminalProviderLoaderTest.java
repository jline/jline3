/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TerminalProvider#load(String)} and
 * {@link TerminalProvider#load(String, ClassLoader)} classloader resolution.
 */
@SuppressWarnings("missing-explicit-ctor")
class TerminalProviderLoaderTest {

    /**
     * A classloader that hides all {@code META-INF/jline/providers/} resources
     * and delegates class loading to its parent. This simulates the thread
     * context classloader in a plugin system that cannot see JLine resources.
     */
    private static class ResourceHidingClassLoader extends ClassLoader {
        ResourceHidingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name != null && name.startsWith("META-INF/jline/providers/")) {
                return null;
            }
            return super.getResourceAsStream(name);
        }

        @Override
        public URL getResource(String name) {
            if (name != null && name.startsWith("META-INF/jline/providers/")) {
                return null;
            }
            return super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name != null && name.startsWith("META-INF/jline/providers/")) {
                return Collections.emptyEnumeration();
            }
            return super.getResources(name);
        }
    }

    /**
     * A classloader that provides a synthetic provider resource pointing to a
     * given class name, without delegating resource lookups to its parent.
     */
    private static class SyntheticProviderClassLoader extends ClassLoader {
        private final String providerName;
        private final String className;

        SyntheticProviderClassLoader(ClassLoader parent, String providerName, String className) {
            super(parent);
            this.providerName = providerName;
            this.className = className;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (("META-INF/jline/providers/" + providerName).equals(name)) {
                return new ByteArrayInputStream(className.getBytes(StandardCharsets.UTF_8));
            }
            return super.getResourceAsStream(name);
        }
    }

    // ------------------------------------------------------------------
    // Basic load(name) tests
    // ------------------------------------------------------------------

    @Test
    void loadExecProvider() throws IOException {
        // "exec" provider lives in the terminal module itself, always available
        TerminalProvider provider = TerminalProvider.load("exec");
        assertNotNull(provider);
        assertEquals("exec", provider.name());
    }

    @Test
    void loadDumbProvider() throws IOException {
        TerminalProvider provider = TerminalProvider.load("dumb");
        assertNotNull(provider);
        assertEquals("dumb", provider.name());
    }

    @Test
    void loadNonExistentProviderThrows() {
        IOException ex = assertThrows(IOException.class, () -> TerminalProvider.load("nonexistent"));
        assertTrue(ex.getMessage().contains("nonexistent"), "Message should name the provider");
        assertTrue(
                ex.getMessage().contains("META-INF/jline/providers/"),
                "Message should mention the resource path being searched");
    }

    // ------------------------------------------------------------------
    // Classloader fallback chain tests
    // ------------------------------------------------------------------

    @Test
    void fallbackToJLineClassLoaderWhenContextHidesResources() throws Exception {
        // Simulate the issue #2110 scenario: thread context classloader
        // cannot see META-INF/jline/providers/* but JLine's own classloader can.
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader hiding = new ResourceHidingClassLoader(original);
        Thread.currentThread().setContextClassLoader(hiding);
        try {
            // load(name) should still succeed by falling back to TerminalProvider's classloader
            TerminalProvider provider = TerminalProvider.load("exec");
            assertNotNull(provider);
            assertEquals("exec", provider.name());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void explicitClassLoaderTakesPriority() throws Exception {
        // Even when the context classloader can find "exec", an explicit
        // classloader pointing to "dumb" should win (it is tried first).
        ClassLoader explicitCl = new SyntheticProviderClassLoader(
                getClass().getClassLoader(), "exec", "org.jline.terminal.impl.DumbTerminalProvider");

        TerminalProvider provider = TerminalProvider.load("exec", explicitCl);
        assertNotNull(provider);
        // The explicit classloader's resource mapped "exec" -> DumbTerminalProvider
        assertEquals("dumb", provider.name());
    }

    @Test
    void nullExplicitClassLoaderFallsBackNormally() throws IOException {
        // Passing null as the explicit classloader should behave the same as load(name)
        TerminalProvider provider = TerminalProvider.load("exec", null);
        assertNotNull(provider);
        assertEquals("exec", provider.name());
    }

    @Test
    void errorMessageMentionsClassLoaderHint() {
        IOException ex = assertThrows(IOException.class, () -> TerminalProvider.load("nonexistent", null));
        assertTrue(
                ex.getMessage().contains("TerminalBuilder.classLoader()"),
                "Error should suggest TerminalBuilder.classLoader() as a fix");
    }

    // ------------------------------------------------------------------
    // Combined context-hiding + explicit classloader
    // ------------------------------------------------------------------

    @Test
    void explicitClassLoaderWorksWhenContextHidesResources() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader hiding = new ResourceHidingClassLoader(original);
        Thread.currentThread().setContextClassLoader(hiding);
        try {
            // Context classloader hides resources, but an explicit classloader
            // that can see them should succeed.
            TerminalProvider provider = TerminalProvider.load("exec", original);
            assertNotNull(provider);
            assertEquals("exec", provider.name());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void allClassLoadersFailGivesActionableError() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader hiding = new ResourceHidingClassLoader(original);
        Thread.currentThread().setContextClassLoader(hiding);
        try {
            // Explicit classloader also hides resources, and JLine's own classloader
            // cannot find "nonexistent" — all three candidates fail.
            IOException ex = assertThrows(IOException.class, () -> TerminalProvider.load("nonexistent", hiding));
            assertTrue(ex.getMessage().contains("nonexistent"));
            assertTrue(ex.getMessage().contains("META-INF/jline/providers/"));
            assertTrue(ex.getMessage().contains("TerminalBuilder.classLoader()"));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
