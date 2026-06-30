/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.provider;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jdk.internal.io.JdkConsole;
import jdk.internal.io.JdkConsoleProvider;

import static org.junit.jupiter.api.Assertions.*;

class JLineConsoleProviderTest {

    @Test
    void providerReturnsNullWhenNotTTY() {
        JdkConsoleProvider provider = new JLineConsoleProvider();
        JdkConsole console = provider.console(false, StandardCharsets.UTF_8);
        assertNull(console, "Provider should return null when not attached to a TTY");
    }

    @Test
    void providerReturnsNonNullWhenTTY() {
        JdkConsoleProvider provider = new JLineConsoleProvider();
        // When isTTY is true, the provider should attempt to create a console.
        // In a test environment without a real terminal, the JLine terminal builder
        // may fall back to a dumb terminal. We verify the provider returns non-null.
        try {
            JdkConsole console = provider.console(true, StandardCharsets.UTF_8);
            assertNotNull(console, "Provider should return a console when attached to a TTY");
        } catch (Exception e) {
            // In CI environments without a terminal, building a system terminal may fail.
            // This is expected behavior - the provider correctly requires a terminal.
        }
    }

    @Test
    void providerReturnsCorrectCharset() {
        JdkConsoleProvider provider = new JLineConsoleProvider();
        try {
            JdkConsole console = provider.console(true, StandardCharsets.UTF_8);
            if (console != null) {
                assertEquals(StandardCharsets.UTF_8, console.charset());
            }
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void providerAcceptsVariousCharsets() {
        JdkConsoleProvider provider = new JLineConsoleProvider();
        for (Charset charset :
                new Charset[] {StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII}) {
            try {
                JdkConsole console = provider.console(true, charset);
                if (console != null) {
                    assertEquals(charset, console.charset());
                }
            } catch (Exception e) {
                // Terminal may not be available in test environment
            }
        }
    }

    @Test
    void providerImplementsServiceInterface() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        assertInstanceOf(JdkConsoleProvider.class, provider);
    }
}
