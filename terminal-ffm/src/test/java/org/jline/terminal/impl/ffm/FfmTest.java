/*
 * Copyright (c) 2023, the original author(s).
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
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class FfmTest {

    @Test
    @DisabledOnOs(OS.WINDOWS) // non system terminals are not supported on windows
    public void testNewTerminalWithNull() throws IOException {
        Terminal terminal = new FfmTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        false,
                        null,
                        null);
        // terminal.close();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // non system terminals are not supported on windows
    public void testNewTerminalNoNull() throws IOException {
        Terminal terminal = new FfmTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        false,
                        new Attributes(),
                        new Size());
        Size size = terminal.getSize();
        // terminal.close();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void checkStructLayout() {
        try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
            new Kernel32.KEY_EVENT_RECORD(arena);
            new Kernel32.MOUSE_EVENT_RECORD(arena);
            new Kernel32.WINDOW_BUFFER_SIZE_RECORD(arena);
            new Kernel32.MENU_EVENT_RECORD(arena);
            new Kernel32.FOCUS_EVENT_RECORD(arena);
            new Kernel32.INPUT_RECORD(arena);
            new Kernel32.SMALL_RECT(arena);
        }
    }
}
