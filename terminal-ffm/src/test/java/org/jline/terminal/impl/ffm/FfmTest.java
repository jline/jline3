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
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.OSUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void applyAttributesPreservesFlagsAndSpeed() {
        try (Arena arena = Arena.ofConfined()) {
            // Set up Attributes with recognisable flags
            Attributes attr = new Attributes();
            attr.setInputFlag(InputFlag.ICRNL, true);
            attr.setLocalFlag(LocalFlag.ECHO, true);
            attr.setLocalFlag(LocalFlag.ICANON, true);

            // Round-trip: Attributes -> native termios -> Attributes
            CLibrary.termios t = new CLibrary.termios(arena, attr);
            Attributes roundTripped = t.asAttributes();

            assertEquals(
                    attr.getInputFlag(InputFlag.ICRNL),
                    roundTripped.getInputFlag(InputFlag.ICRNL),
                    "ICRNL should survive the round-trip");
            assertEquals(
                    attr.getLocalFlag(LocalFlag.ECHO),
                    roundTripped.getLocalFlag(LocalFlag.ECHO),
                    "ECHO should survive the round-trip");
            assertEquals(
                    attr.getLocalFlag(LocalFlag.ICANON),
                    roundTripped.getLocalFlag(LocalFlag.ICANON),
                    "ICANON should survive the round-trip");

            // Speed fields exist only on non-AIX platforms (macOS, Linux).
            // On AIX the c_ispeed/c_ospeed VarHandles are null and the
            // getters return 0 regardless of what is written.
            if (!OSUtils.IS_AIX) {
                t.c_ispeed(9600L);
                t.c_ospeed(9600L);
                assertEquals(9600L, t.c_ispeed(), "c_ispeed should be stored");
                assertEquals(9600L, t.c_ospeed(), "c_ospeed should be stored");
            }
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
