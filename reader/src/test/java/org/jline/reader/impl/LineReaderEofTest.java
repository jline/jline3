/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LineReaderEofTest {

    @Test
    @Timeout(10)
    void eofWithExecProvider() throws Exception {
        checkEof(TerminalBuilder.PROP_PROVIDER_EXEC);
    }

    @Test
    @Timeout(10)
    void eofWithDefaultProvider() throws Exception {
        checkEof(null);
    }

    private void checkEof(String providerType) throws Exception {
        String savedProviders = System.getProperty(TerminalBuilder.PROP_PROVIDERS);
        try {
            if (providerType != null) {
                System.setProperty(TerminalBuilder.PROP_PROVIDERS, providerType);
            }

            PipedInputStream in = new PipedInputStream();
            PipedOutputStream outIn = new PipedOutputStream(in);
            outIn.write("hello\n".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

            Terminal terminal;
            try {
                terminal = TerminalBuilder.builder().streams(in, out).build();
            } catch (Exception e) {
                assumeTrue(false, "Provider '" + providerType + "' not available: " + e.getMessage());
                return;
            }

            try (terminal) {
                LineReader lr = LineReaderBuilder.builder().terminal(terminal).build();
                assertEquals("hello", lr.readLine());
                outIn.close();
                Thread.sleep(200);
                assertThrows(EndOfFileException.class, () -> lr.readLine());
            }
        } finally {
            if (savedProviders != null) {
                System.setProperty(TerminalBuilder.PROP_PROVIDERS, savedProviders);
            } else {
                System.clearProperty(TerminalBuilder.PROP_PROVIDERS);
            }
        }
    }
}
