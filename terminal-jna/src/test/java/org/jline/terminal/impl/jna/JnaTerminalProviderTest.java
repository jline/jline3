/*
 * Copyright (c) 2002-2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jna;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Terminal;
import org.jline.terminal.spi.SystemStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Platform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class JnaTerminalProviderTest {

    @Test
    void testIsSystemStream() {
        assumeFalse(Platform.isMac() && Platform.is64Bit() && Platform.isARM());

        assertDoesNotThrow(() -> new JnaTerminalProvider().isSystemStream(SystemStream.Output));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testNewTerminal() throws IOException {
        assumeFalse(Platform.isMac() && Platform.is64Bit() && Platform.isARM());

        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Terminal terminal = new JnaTerminalProvider()
                .newTerminal(
                        "name",
                        "xterm",
                        pis,
                        baos,
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Charset.defaultCharset(),
                        Terminal.SignalHandler.SIG_DFL,
                        true,
                        null,
                        null);
        assertNotNull(terminal);
    }
}
