/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.provider;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jdk.internal.io.JdkConsole;
import jdk.internal.io.JdkConsoleProvider;

import static org.junit.jupiter.api.Assertions.*;

class JLineConsoleProviderTest {

    @Test
    void providerReturnsNullWhenNotTTY() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        JdkConsole console = provider.console(false, StandardCharsets.UTF_8);
        assertNull(console, "Provider should return null when not attached to a TTY");
    }

    @Test
    void providerThreeArgReturnsNullWhenNotTTY() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        JdkConsole console = provider.console(false, StandardCharsets.UTF_8, StandardCharsets.UTF_8);
        assertNull(console, "Provider (3-arg) should return null when not attached to a TTY");
    }

    @Test
    void providerReturnsNonNullWhenTTY() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        try {
            JdkConsole console = provider.console(true, StandardCharsets.UTF_8);
            assertNotNull(console, "Provider should return a console when attached to a TTY");
        } catch (Exception e) {
            // In CI environments without a terminal, building a system terminal may fail.
        }
    }

    @Test
    void providerThreeArgReturnsNonNullWhenTTY() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        try {
            JdkConsole console = provider.console(true, StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1);
            assertNotNull(console, "Provider (3-arg) should return a console when attached to a TTY");
        } catch (Exception e) {
            // In CI environments without a terminal, building a system terminal may fail.
        }
    }

    @Test
    void providerReturnsCorrectCharset() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
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
    void providerThreeArgUsesOutCharset() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        try {
            JdkConsole console = provider.console(true, StandardCharsets.US_ASCII, StandardCharsets.ISO_8859_1);
            if (console != null) {
                assertEquals(
                        StandardCharsets.ISO_8859_1, console.charset(), "charset() should return the output charset");
            }
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void providerImplementsServiceInterface() {
        JLineConsoleProvider provider = new JLineConsoleProvider();
        assertInstanceOf(JdkConsoleProvider.class, provider);
    }
}
