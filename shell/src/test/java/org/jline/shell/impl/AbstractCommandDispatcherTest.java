/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.*;
import java.util.Objects;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base-class for tests that use a {@link DefaultCommandDispatcher}.
 * <p>
 * This base-class also deals with cleanup of the {@link Terminal} and {@link DefaultCommandDispatcher}.
 */
public abstract class AbstractCommandDispatcherTest {

    protected PipedOutputStream terminalInput;
    private PipedInputStream terminalStream;
    protected Terminal terminal;
    protected DefaultCommandDispatcher dispatcher;
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
        dispatcher = Objects.requireNonNull(createDispatcher());
    }

    /**
     * The factory for instantiating custom {@link DefaultCommandDispatcher} instances.
     */
    protected DefaultCommandDispatcher createDispatcher() {
        return new DefaultCommandDispatcher(terminal);
    }

    @AfterEach
    protected void tearDown() throws IOException {
        try {
            try {
                if (terminal != null) {
                    terminal.close();
                }
            } finally {
                if (dispatcher != null) {
                    dispatcher.close();
                }
            }
        } finally {
            try {
                terminalInput.close();
            } finally {
                if (terminalStream != null) {
                    terminalStream.close();
                }
            }
        }
    }
}
