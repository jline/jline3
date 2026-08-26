/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.provider;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;

import jdk.internal.io.JdkConsole;

import static org.junit.jupiter.api.Assertions.*;

class JLineJdkConsoleTest {

    private JLineJdkConsole createConsole() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = new LineDisciplineTerminal("test", "dumb", out, StandardCharsets.UTF_8);
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        return new JLineJdkConsole(StandardCharsets.UTF_8, terminal, reader);
    }

    @Test
    void charsetReturnsConfiguredCharset() {
        JLineJdkConsole console = new JLineJdkConsole(StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, console.charset());

        JLineJdkConsole latin1Console = new JLineJdkConsole(StandardCharsets.ISO_8859_1);
        assertEquals(StandardCharsets.ISO_8859_1, latin1Console.charset());
    }

    @Test
    void writerReturnsNonNull() throws Exception {
        JLineJdkConsole console = createConsole();
        PrintWriter writer = console.writer();
        assertNotNull(writer, "writer() should return a non-null PrintWriter");
    }

    @Test
    void readerReturnsNonNull() throws Exception {
        JLineJdkConsole console = createConsole();
        Reader reader = console.reader();
        assertNotNull(reader, "reader() should return a non-null Reader");
    }

    @Test
    void formatReturnsSelf() throws Exception {
        JLineJdkConsole console = createConsole();
        JdkConsole result = console.format("Hello %s%n", "World");
        assertSame(console, result, "format() should return 'this' for method chaining");
    }

    @Test
    void formatWithLocaleReturnsSelf() throws Exception {
        JLineJdkConsole console = createConsole();
        JdkConsole result = console.format(Locale.US, "Hello %s%n", "World");
        assertSame(console, result, "format(Locale,...) should return 'this' for method chaining");
    }

    @Test
    void printfReturnsSelf() throws Exception {
        JLineJdkConsole console = createConsole();
        JdkConsole result = console.printf("Hello %s%n", "World");
        assertSame(console, result, "printf() should return 'this' for method chaining");
    }

    @Test
    void printlnReturnsSelf() throws Exception {
        JLineJdkConsole console = createConsole();
        JdkConsole result = console.println("Hello World");
        assertSame(console, result, "println() should return 'this' for method chaining");
    }

    @Test
    void printReturnsSelf() throws Exception {
        JLineJdkConsole console = createConsole();
        JdkConsole result = console.print("Hello World");
        assertSame(console, result, "print() should return 'this' for method chaining");
    }

    @Test
    void flushDoesNotThrow() throws Exception {
        JLineJdkConsole console = createConsole();
        assertDoesNotThrow(console::flush);
    }

    @Test
    void writerReturnsSameInstance() throws Exception {
        JLineJdkConsole console = createConsole();
        PrintWriter w1 = console.writer();
        PrintWriter w2 = console.writer();
        assertSame(w1, w2, "writer() should return the same instance on repeated calls");
    }
}
