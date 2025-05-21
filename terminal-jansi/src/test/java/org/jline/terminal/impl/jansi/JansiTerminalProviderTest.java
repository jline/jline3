/*
 * Copyright (c) 2002-2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Terminal;
import org.jline.terminal.spi.SystemStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JansiTerminalProviderTest {

    @Test
    public void testJansiVersion() {
        assertEquals(2, JansiTerminalProvider.JANSI_MAJOR_VERSION);
        assertEquals(4, JansiTerminalProvider.JANSI_MINOR_VERSION);
    }

    @Test
    void testIsSystemStream() {
        assertDoesNotThrow(() -> new JansiTerminalProvider().isSystemStream(SystemStream.Output));
    }

    @Test
    @Disabled
    void testNewTerminal() throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Terminal terminal = new JansiTerminalProvider()
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
