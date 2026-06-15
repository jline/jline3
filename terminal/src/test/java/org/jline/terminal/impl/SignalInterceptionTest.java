/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal.Signal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for software signal interception when ISIG is cleared (raw mode).
 *
 * <p>Since {@code AbstractUnixSysTerminal.SignalInterceptingInputStream} is tied
 * to {@code FileDescriptor.in} and cannot be instantiated in a unit test, we test
 * the equivalent behavior through {@link LineDisciplineTerminal} which has its own
 * signal translation in {@code doProcessInputByte()}.
 *
 * <p>These tests verify the signal-raising contract that both implementations share:
 * when ISIG is enabled, signal control characters trigger the corresponding signal.
 */
class SignalInterceptionTest {

    @Test
    void testVintrRaisesSignalInt() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "ansi", masterOutput, StandardCharsets.UTF_8)) {
            // ISIG is on by default — signal bytes should be intercepted
            Attributes attr = terminal.getAttributes();
            assertTrue(attr.getLocalFlag(LocalFlag.ISIG));
            int vintr = attr.getControlChar(ControlChar.VINTR);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            // Write VINTR byte to the terminal's master input
            terminal.processInputByte((byte) vintr);

            assertEquals(Signal.INT, received.get());
        }
    }

    @Test
    void testVquitRaisesSignalQuit() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "ansi", masterOutput, StandardCharsets.UTF_8)) {
            Attributes attr = terminal.getAttributes();
            int vquit = attr.getControlChar(ControlChar.VQUIT);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.QUIT, received::set);

            terminal.processInputByte((byte) vquit);

            assertEquals(Signal.QUIT, received.get());
        }
    }

    @Test
    void testVsuspRaisesSignalTstp() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "ansi", masterOutput, StandardCharsets.UTF_8)) {
            Attributes attr = terminal.getAttributes();
            int vsusp = attr.getControlChar(ControlChar.VSUSP);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.TSTP, received::set);

            terminal.processInputByte((byte) vsusp);

            assertEquals(Signal.TSTP, received.get());
        }
    }

    @Test
    void testNoSignalWhenIsigDisabled() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "ansi", masterOutput, StandardCharsets.UTF_8)) {
            // Disable ISIG — LineDisciplineTerminal should NOT intercept signal bytes
            Attributes attr = terminal.getAttributes();
            attr.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attr);

            int vintr = attr.getControlChar(ControlChar.VINTR);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            terminal.processInputByte((byte) vintr);

            // LineDisciplineTerminal does NOT raise when ISIG is off
            // (AbstractUnixSysTerminal.SignalInterceptingInputStream fills this gap
            // for system terminals)
            assertNull(received.get());
        }
    }

    @Test
    void testDisabledControlCharDoesNotRaise() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "ansi", masterOutput, StandardCharsets.UTF_8)) {
            // Disable VINTR by setting it to -1
            Attributes attr = terminal.getAttributes();
            attr.setControlChar(ControlChar.VINTR, -1);
            terminal.setAttributes(attr);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            // Send Ctrl+C (0x03) — should NOT raise because VINTR is disabled
            terminal.processInputByte((byte) 0x03);

            assertNull(received.get());
        }
    }
}
