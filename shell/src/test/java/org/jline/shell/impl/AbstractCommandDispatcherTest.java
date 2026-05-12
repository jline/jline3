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

import org.jline.shell.AbstractTerminalTest;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base-class for tests that use a {@link DefaultCommandDispatcher}.
 * <p>
 * This base-class also deals with cleanup of the {@link Terminal} and {@link DefaultCommandDispatcher}.
 */
public abstract class AbstractCommandDispatcherTest extends AbstractTerminalTest {

    protected DefaultCommandDispatcher dispatcher;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        dispatcher = Objects.requireNonNull(createDispatcher());
    }

    /**
     * The factory for instantiating custom {@link DefaultCommandDispatcher} instances.
     */
    protected DefaultCommandDispatcher createDispatcher() {
        return new DefaultCommandDispatcher(terminal);
    }

    @Override
    @AfterEach
    protected void tearDown() throws IOException {
        try {
            dispatcher.close();
        } finally {
            super.tearDown();
        }
    }
}
