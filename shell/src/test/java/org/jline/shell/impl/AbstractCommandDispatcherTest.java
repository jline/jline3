/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base-class for tests that use a {@link DefaultCommandDispatcher}.
 * <p>
 * This base-class also deals with cleanup of the {@link Terminal} and {@link DefaultCommandDispatcher}.
 */
public class AbstractCommandDispatcherTest {

    protected Terminal terminal;
    protected DefaultCommandDispatcher dispatcher;
    private final Function<Terminal, DefaultCommandDispatcher> factory;

    /**
     * Default constructor, creates a simple {@link DefaultCommandDispatcher}.
     */
    protected AbstractCommandDispatcherTest() {
        this(DefaultCommandDispatcher::new);
    }

    /**
     * Constructor allowing for customization of the new {@link DefaultCommandDispatcher}.
     *
     * @param factory The factory for instantiating custom {@link DefaultCommandDispatcher} instances.
     */
    protected AbstractCommandDispatcherTest(Function<Terminal, DefaultCommandDispatcher> factory) {
        this.factory = factory;
    }

    @BeforeEach
    protected void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = Objects.requireNonNull(factory.apply(terminal));
    }

    @AfterEach
    protected void tearDown() throws IOException {
        try {
            if (terminal != null) {
                terminal.close();
            }
        } finally {
            if (dispatcher != null) {
                dispatcher.close();
            }
        }
    }
}
