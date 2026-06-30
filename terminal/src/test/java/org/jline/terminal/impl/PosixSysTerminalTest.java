/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.spi.Pty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosixSysTerminalTest {

    @Test
    void testNativeSignalsDefault() throws Exception {
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        EasyMock.replay(pty);
        try (PosixSysTerminal terminal = new PosixSysTerminal("name", "ansi", pty, null, true, SignalHandler.SIG_DFL)) {
            assertFalse(terminal.nativeHandlers.isEmpty());
        }
    }

    @Test
    void testNativeSignalsIgnore() throws Exception {
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        EasyMock.replay(pty);
        try (PosixSysTerminal terminal = new PosixSysTerminal("name", "ansi", pty, null, true, SignalHandler.SIG_IGN)) {
            assertFalse(terminal.nativeHandlers.isEmpty());
        }
    }

    @Test
    void testNativeSignalsRegister() throws Exception {
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        EasyMock.replay(pty);
        try (PosixSysTerminal terminal = new PosixSysTerminal("name", "ansi", pty, null, true, SignalHandler.SIG_DFL)) {
            int initialSize = terminal.nativeHandlers.size();
            assertFalse(terminal.nativeHandlers.isEmpty());
            SignalHandler prev = terminal.handle(Signal.INT, s -> {});
            assertEquals(initialSize, terminal.nativeHandlers.size());
            terminal.handle(Signal.INT, prev);
            assertEquals(initialSize, terminal.nativeHandlers.size());
        }
    }

    @Test
    void testEnterRawModeBlocksUntilOneByte() throws Exception {
        // Regression: VMIN=0/VTIME=1 made FileInputStream.read() return -1 (EOF) on every 100 ms idle tick.
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        Capture<Attributes> applied = EasyMock.newCapture();
        pty.setAttr(EasyMock.capture(applied));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(pty);
        try (PosixSysTerminal terminal =
                new PosixSysTerminal("name", "ansi", pty, null, false, SignalHandler.SIG_DFL)) {
            terminal.enterRawMode();
            Attributes raw = applied.getValue();
            assertEquals(1, raw.getControlChar(ControlChar.VMIN));
            assertEquals(0, raw.getControlChar(ControlChar.VTIME));
        }
    }

    @Test
    void testNoNativeSignalsWhenDisabled() throws Exception {
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        EasyMock.replay(pty);
        try (PosixSysTerminal terminal =
                new PosixSysTerminal("name", "ansi", pty, null, false, SignalHandler.SIG_DFL)) {
            assertTrue(terminal.nativeHandlers.isEmpty());
        }
    }

    @Test
    void testSignalInterceptionWhenIsigCleared() throws Exception {
        String prev = System.getProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS);
        try {
            System.setProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS, "true");

            Attributes attr = new Attributes();
            attr.setControlChar(ControlChar.VINTR, 3);
            attr.setControlChar(ControlChar.VQUIT, 28);
            attr.setControlChar(ControlChar.VSUSP, 26);

            Pty pty = EasyMock.createNiceMock(Pty.class);
            EasyMock.expect(pty.getAttr()).andReturn(attr).anyTimes();
            EasyMock.expect(pty.getSlaveInput())
                    .andReturn(new ByteArrayInputStream(new byte[] {0x03}))
                    .anyTimes();
            EasyMock.expect(pty.getSlaveOutput())
                    .andReturn(new ByteArrayOutputStream())
                    .anyTimes();
            EasyMock.replay(pty);
            try (PosixSysTerminal terminal =
                    new PosixSysTerminal("name", "ansi", pty, null, false, SignalHandler.SIG_DFL)) {
                // ISIG is off by default in a new Attributes — signal interception should be active
                AtomicReference<Signal> received = new AtomicReference<>();
                terminal.handle(Signal.INT, received::set);

                int b = terminal.input().read();
                assertEquals(0x03, b);
                assertEquals(Signal.INT, received.get());
            }
        } finally {
            if (prev == null) {
                System.clearProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS);
            } else {
                System.setProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS, prev);
            }
        }
    }

    @Test
    void testNoSignalInterceptionWhenIsigSet() throws Exception {
        String prev = System.getProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS);
        try {
            System.setProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS, "true");

            Attributes attr = new Attributes();
            attr.setControlChar(ControlChar.VINTR, 3);
            attr.setLocalFlag(LocalFlag.ISIG, true);

            Pty pty = EasyMock.createNiceMock(Pty.class);
            EasyMock.expect(pty.getAttr()).andReturn(attr).anyTimes();
            EasyMock.expect(pty.getSlaveInput())
                    .andReturn(new ByteArrayInputStream(new byte[] {0x03}))
                    .anyTimes();
            EasyMock.expect(pty.getSlaveOutput())
                    .andReturn(new ByteArrayOutputStream())
                    .anyTimes();
            EasyMock.replay(pty);
            try (PosixSysTerminal terminal =
                    new PosixSysTerminal("name", "ansi", pty, null, false, SignalHandler.SIG_DFL)) {
                // Set ISIG so the software interceptor should NOT fire
                terminal.setAttributes(attr);

                AtomicReference<Signal> received = new AtomicReference<>();
                terminal.handle(Signal.INT, received::set);

                int b = terminal.input().read();
                assertEquals(0x03, b);
                assertNull(received.get());
            }
        } finally {
            if (prev == null) {
                System.clearProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS);
            } else {
                System.setProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS, prev);
            }
        }
    }

    @Test
    void testNoSignalInterceptionWhenSoftwareSignalsDisabled() throws Exception {
        String prev = System.getProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS);
        try {
            System.setProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS, "false");

            Attributes attr = new Attributes();
            attr.setControlChar(ControlChar.VINTR, 3);

            Pty pty = EasyMock.createNiceMock(Pty.class);
            EasyMock.expect(pty.getAttr()).andReturn(attr).anyTimes();
            EasyMock.expect(pty.getSlaveInput())
                    .andReturn(new ByteArrayInputStream(new byte[] {0x03}))
                    .anyTimes();
            EasyMock.expect(pty.getSlaveOutput())
                    .andReturn(new ByteArrayOutputStream())
                    .anyTimes();
            EasyMock.replay(pty);
            try (PosixSysTerminal terminal =
                    new PosixSysTerminal("name", "ansi", pty, null, false, SignalHandler.SIG_DFL)) {
                AtomicReference<Signal> received = new AtomicReference<>();
                terminal.handle(Signal.INT, received::set);

                int b = terminal.input().read();
                assertEquals(0x03, b);
                assertNull(received.get());
            }
        } finally {
            if (prev == null) {
                System.clearProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS);
            } else {
                System.setProperty(TerminalBuilder.PROP_SOFTWARE_SIGNALS, prev);
            }
        }
    }

    @Test
    void testHandleDoesNotRegisterWhenNativeSignalsDisabled() throws Exception {
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        EasyMock.replay(pty);
        try (PosixSysTerminal terminal =
                new PosixSysTerminal("name", "ansi", pty, null, false, SignalHandler.SIG_DFL)) {
            assertTrue(terminal.nativeHandlers.isEmpty());
            terminal.handle(Signal.INT, s -> {});
            assertTrue(terminal.nativeHandlers.isEmpty());
        }
    }
}
