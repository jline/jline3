/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.TermiosMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmTest {

    @Test
    @DisabledOnOs(OS.WINDOWS) // non system terminals are not supported on windows
    void testNewTerminalWithNull() throws IOException {
        try (Terminal terminal = new FfmTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        false,
                        null,
                        null)) {
            assertNotNull(terminal);
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // non system terminals are not supported on windows
    void testNewTerminalNoNull() throws IOException {
        try (Terminal terminal = new FfmTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        false,
                        new Attributes(),
                        Size.of(0, 0))) {
            assertNotNull(terminal);
            assertNotNull(terminal.getSize());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testWinsizeConstructorArgumentOrder() {
        try (Arena arena = Arena.ofConfined()) {
            short cols = 120;
            short rows = 40;
            CLibrary.winsize ws = new CLibrary.winsize(arena, cols, rows);
            assertEquals(cols, ws.ws_col());
            assertEquals(rows, ws.ws_row());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void applyAttributesPreservesBaudAndSpeed() {
        try (Arena arena = Arena.ofConfined()) {
            CLibrary.termios t = new CLibrary.termios(arena);
            Attributes seed = new Attributes();
            seed.setControlFlag(ControlFlag.CS8, true);
            seed.setControlFlag(ControlFlag.CREAD, true);
            seed.setLocalFlag(LocalFlag.ECHO, true);
            t.apply(seed);

            TermiosMapping mapping = TermiosMapping.forCurrentPlatform();
            Attributes allControl = new Attributes();
            for (ControlFlag flag : ControlFlag.values()) {
                allControl.setControlFlag(flag, true);
            }
            long mappedCflag = mapping.toTermios(allControl).cflag();
            long baudBits = 0x000DL & ~mappedCflag;
            if (baudBits == 0) {
                baudBits = Long.lowestOneBit(~mappedCflag);
            }
            t.c_cflag(t.c_cflag() | baudBits);
            t.c_ispeed(9600);
            t.c_ospeed(9600);

            Attributes attr = t.asAttributes();
            attr.setLocalFlag(LocalFlag.ECHO, false);
            t.apply(attr);

            assertEquals(baudBits, t.c_cflag() & baudBits);
            assertEquals(9600, t.c_ispeed());
            assertEquals(9600, t.c_ospeed());
            Attributes applied = t.asAttributes();
            assertFalse(applied.getLocalFlag(LocalFlag.ECHO));
            assertTrue(applied.getControlFlag(ControlFlag.CS8));
            assertTrue(applied.getControlFlag(ControlFlag.CREAD));
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void checkStructLayout() {
        try (Arena arena = Arena.ofConfined()) {
            assertNotNull(new Kernel32.KEY_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.MOUSE_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.WINDOW_BUFFER_SIZE_RECORD(arena));
            assertNotNull(new Kernel32.MENU_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.FOCUS_EVENT_RECORD(arena));
            assertNotNull(new Kernel32.INPUT_RECORD(arena));
            assertNotNull(new Kernel32.SMALL_RECT(arena));
        }
    }
}
