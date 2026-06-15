/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base-class for tests that use a {@link Terminal}.
 * <p>
 * This base-class also deals with cleanup of the {@link Terminal}.
 */
public abstract class AbstractTerminalTest {

    protected PipedOutputStream terminalInput;
    private PipedInputStream terminalStream;
    protected Terminal terminal;
    protected ByteArrayOutputStream terminalOutput;

    @BeforeEach
    protected void setUp() throws IOException {
        terminalInput = new PipedOutputStream();
        terminalStream = new PipedInputStream(terminalInput);
        terminalOutput = new ByteArrayOutputStream();
        terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(terminalStream, terminalOutput)
                .build();
    }

    @AfterEach
    protected void tearDown() throws IOException {
        try {
            if (terminal != null) {
                terminal.close();
            }
        } finally {
            try {
                if (terminalInput != null) {
                    terminalInput.close();
                }
            } finally {
                if (terminalStream != null) {
                    terminalStream.close();
                }
            }
        }
    }
}
