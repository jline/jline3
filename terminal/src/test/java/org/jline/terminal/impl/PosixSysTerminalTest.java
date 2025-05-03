/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.easymock.EasyMock;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.spi.Pty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PosixSysTerminalTest {

    @Test
    public void testNativeSignalsDefault() throws Exception {
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
    public void testNativeSignalsIgnore() throws Exception {
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
    public void testNativeSignalsRegister() throws Exception {
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
}
