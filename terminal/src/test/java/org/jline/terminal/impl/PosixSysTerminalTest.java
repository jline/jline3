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

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.spi.Pty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            assertEquals(Signal.values().length, terminal.nativeHandlers.size());
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
            assertEquals(Signal.values().length, terminal.nativeHandlers.size());
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
            assertEquals(Signal.values().length, terminal.nativeHandlers.size());
            SignalHandler prev = terminal.handle(Signal.INT, s -> {});
            assertEquals(Signal.values().length, terminal.nativeHandlers.size());
            terminal.handle(Signal.INT, prev);
            assertEquals(Signal.values().length, terminal.nativeHandlers.size());
        }
    }

    @Test
    void testEnterRawModeBlocksUntilOneByte() throws Exception {
        // Regression: enterRawMode used to set VMIN=0/VTIME=1, which makes
        // the kernel return zero bytes after a 100 ms idle window. The JVM's
        // FileInputStream.read() turns that zero-byte read into -1 (EOF),
        // so any input pump reading from FileDescriptor.in saw a spurious
        // EOF on every empty tick. The POSIX cfmakeraw(3) defaults
        // (VMIN=1, VTIME=0) make read() block until at least one byte is
        // available, which is what every JLine caller of enterRawMode
        // actually wants — NonBlockingReader.read(timeoutMs) layers its
        // own polling on top.
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
}
