/*
 * Copyright (c) 2002-2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JniTerminalProviderTest {

    @Test
    void testIsSystemStream() {
        assertDoesNotThrow(() -> new JniTerminalProvider().isSystemStream(SystemStream.Output));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testNewTerminal() throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Terminal terminal = new JniTerminalProvider()
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
