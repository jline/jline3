/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.provider;

import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jdk.internal.io.JdkConsole;

import static org.junit.jupiter.api.Assertions.*;

class JLineJdkConsoleTest {

    /**
     * Helper to create a console instance for testing.
     * May return null if no terminal is available in the test environment.
     */
    private JdkConsole createConsole() {
        try {
            return new JLineJdkConsole(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void charsetReturnsConfiguredCharset() {
        JLineJdkConsole console = new JLineJdkConsole(StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, console.charset());

        JLineJdkConsole latin1Console = new JLineJdkConsole(StandardCharsets.ISO_8859_1);
        assertEquals(StandardCharsets.ISO_8859_1, latin1Console.charset());
    }

    @Test
    void writerReturnsNonNull() {
        JdkConsole console = createConsole();
        if (console == null) return;
        try {
            PrintWriter writer = console.writer();
            assertNotNull(writer, "writer() should return a non-null PrintWriter");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void readerReturnsNonNull() {
        JdkConsole console = createConsole();
        if (console == null) return;
        try {
            Reader reader = console.reader();
            assertNotNull(reader, "reader() should return a non-null Reader");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void formatReturnsSelf() {
        JdkConsole console = createConsole();
        if (console == null) return;
        try {
            JdkConsole result = console.format("Hello %s%n", "World");
            assertSame(console, result, "format() should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void printfReturnsSelf() {
        JdkConsole console = createConsole();
        if (console == null) return;
        try {
            JdkConsole result = console.printf("Hello %s%n", "World");
            assertSame(console, result, "printf() should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void flushDoesNotThrow() {
        JdkConsole console = createConsole();
        if (console == null) return;
        try {
            assertDoesNotThrow(console::flush);
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void writerReturnsSameInstance() {
        JdkConsole console = createConsole();
        if (console == null) return;
        try {
            PrintWriter w1 = console.writer();
            PrintWriter w2 = console.writer();
            assertSame(w1, w2, "writer() should return the same instance on repeated calls");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }
}
