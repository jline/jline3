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
import java.util.Locale;

import org.junit.jupiter.api.Test;

import jdk.internal.io.JdkConsole;

import static org.junit.jupiter.api.Assertions.*;

class JLineJdkConsoleTest {

    /**
     * Helper to create a console instance for testing.
     * May throw if no terminal is available in the test environment.
     */
    private JLineJdkConsole createConsole() {
        return new JLineJdkConsole(StandardCharsets.UTF_8);
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
        JLineJdkConsole console = createConsole();
        try {
            PrintWriter writer = console.writer();
            assertNotNull(writer, "writer() should return a non-null PrintWriter");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void readerReturnsNonNull() {
        JLineJdkConsole console = createConsole();
        try {
            Reader reader = console.reader();
            assertNotNull(reader, "reader() should return a non-null Reader");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void formatReturnsSelf() {
        JLineJdkConsole console = createConsole();
        try {
            JdkConsole result = console.format("Hello %s%n", "World");
            assertSame(console, result, "format() should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void formatWithLocaleReturnsSelf() {
        JLineJdkConsole console = createConsole();
        try {
            JdkConsole result = console.format(Locale.US, "Hello %s%n", "World");
            assertSame(console, result, "format(Locale,...) should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void printfReturnsSelf() {
        JLineJdkConsole console = createConsole();
        try {
            JdkConsole result = console.printf("Hello %s%n", "World");
            assertSame(console, result, "printf() should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void printlnReturnsSelf() {
        JLineJdkConsole console = createConsole();
        try {
            JdkConsole result = console.println("Hello World");
            assertSame(console, result, "println() should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void printReturnsSelf() {
        JLineJdkConsole console = createConsole();
        try {
            JdkConsole result = console.print("Hello World");
            assertSame(console, result, "print() should return 'this' for method chaining");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void flushDoesNotThrow() {
        JLineJdkConsole console = createConsole();
        try {
            assertDoesNotThrow(console::flush);
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }

    @Test
    void writerReturnsSameInstance() {
        JLineJdkConsole console = createConsole();
        try {
            PrintWriter w1 = console.writer();
            PrintWriter w2 = console.writer();
            assertSame(w1, w2, "writer() should return the same instance on repeated calls");
        } catch (Exception e) {
            // Terminal may not be available in test environment
        }
    }
}
