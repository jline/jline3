/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base-class for tests that want to test {@link org.jline.shell.CommandGroup}s.
 * <p>
 * Provides the conveniences of {@link AbstractCommandDispatcherTest}
 * and includes a {@link LineReader} and captured {@link CommandSession}.
 */
public class AbstractCommandsTest extends AbstractCommandDispatcherTest {

    protected LineReader reader;
    protected CommandSession session;
    protected ByteArrayOutputStream outCapture;
    protected ByteArrayOutputStream errCapture;

    /**
     * Default constructor, creates a simple {@link DefaultCommandDispatcher}.
     */
    protected AbstractCommandsTest() {
        super();
    }

    /**
     * Constructor allowing for customization of the new {@link DefaultCommandDispatcher}.
     *
     * @param factory The factory for instantiating custom {@link DefaultCommandDispatcher} instances.
     */
    protected AbstractCommandsTest(Function<Terminal, DefaultCommandDispatcher> factory) {
        super(factory);
    }

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        reader = LineReaderBuilder.builder().terminal(terminal).build();
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        session = new CommandSession(null, System.in, new PrintStream(outCapture), new PrintStream(errCapture));
    }
}
