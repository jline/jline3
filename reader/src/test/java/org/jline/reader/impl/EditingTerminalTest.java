/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.EditingTerminal;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link EditingTerminal} seam interfaces and the decoupled
 * {@link LineReaderImpl} constructor.
 */
class EditingTerminalTest {

    /** Verify that Terminal implements EditingTerminal and exposes all provider methods. */
    @Test
    void terminalIsEditingTerminal() throws IOException {
        Terminal terminal = new DumbTerminal(
                "test",
                Terminal.TYPE_DUMB,
                InputStream.nullInputStream(),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);
        terminal.setSize(Size.of(120, 40));

        // Terminal IS-A EditingTerminal, no adapter needed
        assertTrue(terminal instanceof EditingTerminal);
        EditingTerminal et = terminal;

        assertSame(terminal.reader(), et.reader());
        assertSame(terminal.writer(), et.writer());
        assertEquals(terminal.getSize(), et.getSize());
        assertEquals(terminal.getBufferSize(), et.getBufferSize());
        assertEquals(terminal.getName(), et.getName());
        assertEquals(terminal.getType(), et.getType());
        assertEquals(
                terminal.getBooleanCapability(Capability.auto_right_margin),
                et.getBooleanCapability(Capability.auto_right_margin));
        assertEquals(
                terminal.getStringCapability(Capability.cursor_address),
                et.getStringCapability(Capability.cursor_address));
        assertEquals(terminal.getGraphemeClusterMode(), et.getGraphemeClusterMode());

        terminal.close();
    }

    /** Verify that constructing LineReaderImpl from a Terminal uses the terminal as provider. */
    @Test
    void lineReaderFromTerminalUsesTerminalAsProvider() throws IOException {
        byte[] input = "hello\n".getBytes(StandardCharsets.UTF_8);
        Terminal terminal = new DumbTerminal(
                "test",
                Terminal.TYPE_DUMB,
                new ByteArrayInputStream(input),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));

        LineReaderImpl reader = new LineReaderImpl(terminal, "test", null);

        assertSame(terminal, reader.getTerminal());
        assertNotNull(reader.getEditingTerminal());
        // Since Terminal extends EditingTerminal, getEditingTerminal() returns the terminal itself
        assertSame(terminal, reader.getEditingTerminal());
    }

    /** Verify that LineReaderBuilder.editingTerminal() creates a reader with the custom provider. */
    @Test
    void lineReaderBuilderWithEditingTerminal() throws IOException {
        byte[] input = "test\n".getBytes(StandardCharsets.UTF_8);
        Terminal terminal = new DumbTerminal(
                "delegate",
                Terminal.TYPE_DUMB,
                new ByteArrayInputStream(input),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));

        // Pass Terminal as EditingTerminal via builder
        LineReader reader = LineReaderBuilder.builder()
                .editingTerminal(terminal)
                .appName("test-app")
                .build();

        assertNotNull(reader.getEditingTerminal());
        assertEquals("test-app", reader.getAppName());
    }

    /** Verify that a custom EditingTerminal implementation can construct a LineReaderImpl. */
    @Test
    void customEditingTerminalConstruction() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] input = "line\n".getBytes(StandardCharsets.UTF_8);
        DumbTerminal dumb = new DumbTerminal(
                "src", Terminal.TYPE_DUMB, new ByteArrayInputStream(input), out, StandardCharsets.UTF_8);
        dumb.setSize(Size.of(80, 24));

        // Create a custom provider that wraps a terminal's reader/writer but is NOT a Terminal
        EditingTerminal custom = new EditingTerminal() {
            @Override
            public NonBlockingReader reader() {
                return dumb.reader();
            }

            @Override
            public PrintWriter writer() {
                return dumb.writer();
            }

            @Override
            public void flush() {
                dumb.flush();
            }

            @Override
            public boolean puts(Capability capability, Object... params) {
                return false;
            }

            @Override
            public Size getSize() {
                return Size.of(80, 24);
            }

            @Override
            public Size getBufferSize() {
                return Size.of(80, 24);
            }

            @Override
            public String getStringCapability(Capability capability) {
                return null;
            }

            @Override
            public boolean getBooleanCapability(Capability capability) {
                return false;
            }

            @Override
            public SignalHandler handle(Signal signal, SignalHandler handler) {
                return null;
            }
        };

        LineReaderImpl reader = new LineReaderImpl(custom, "embedded-app", null);

        assertNotNull(reader.getEditingTerminal());
        assertSame(custom, reader.getEditingTerminal());
        // Terminal should be a dumb terminal created internally for Display
        assertNotNull(reader.getTerminal());
        assertEquals("embedded-app", reader.getAppName());
    }

    /** Verify that EditingTerminal default methods return sensible defaults. */
    @Test
    void defaultMethodsReturnSensibleDefaults() {
        // A minimal implementation that only provides the required abstract methods
        EditingTerminal minimal = new EditingTerminal() {
            @Override
            public NonBlockingReader reader() {
                return null;
            }

            @Override
            public PrintWriter writer() {
                return null;
            }

            @Override
            public void flush() {}

            @Override
            public boolean puts(Capability capability, Object... params) {
                return false;
            }

            @Override
            public Size getSize() {
                return Size.of(80, 24);
            }

            @Override
            public Size getBufferSize() {
                return Size.of(80, 24);
            }

            @Override
            public String getStringCapability(Capability capability) {
                return null;
            }

            @Override
            public boolean getBooleanCapability(Capability capability) {
                return false;
            }

            @Override
            public SignalHandler handle(Signal signal, SignalHandler handler) {
                return null;
            }
        };

        // Test default methods from InputProvider
        assertEquals(null, minimal.readMouseEvent(() -> -1, ""));

        // Test default methods from OutputProvider
        assertEquals(false, minimal.trackMouse(Terminal.MouseTracking.Normal));

        // Test default methods from CapabilityProvider
        assertEquals(false, minimal.getGraphemeClusterMode());
        assertEquals(null, minimal.getCursorPosition(c -> {}));
        assertEquals("embedded", minimal.getName());
        assertEquals(Terminal.TYPE_DUMB, minimal.getType());

        // Test default methods from SignalProvider
        Attributes attr = minimal.enterRawMode();
        assertNotNull(attr);
        minimal.setAttributes(attr); // should not throw
        assertNotNull(minimal.getAttributes());
    }

    /** Verify that readLine works when a Terminal is used as EditingTerminal. */
    @Test
    void readLineThroughTerminal() throws IOException {
        byte[] input = "hello world\n".getBytes(StandardCharsets.UTF_8);
        Terminal terminal = new DumbTerminal(
                "test",
                Terminal.TYPE_DUMB,
                new ByteArrayInputStream(input),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));

        // Construct via the EditingTerminal constructor
        LineReaderImpl reader = new LineReaderImpl((EditingTerminal) terminal, "test", null);

        String line = reader.readLine();
        assertEquals("hello world", line);

        terminal.close();
    }

    /** Verify that EOF throws EndOfFileException. */
    @Test
    void eofThrowsEndOfFileException() throws IOException {
        Terminal terminal = new DumbTerminal(
                "test",
                Terminal.TYPE_DUMB,
                InputStream.nullInputStream(),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));

        LineReaderImpl reader = new LineReaderImpl((EditingTerminal) terminal, "test", null);

        assertThrows(EndOfFileException.class, reader::readLine);

        terminal.close();
    }
}
