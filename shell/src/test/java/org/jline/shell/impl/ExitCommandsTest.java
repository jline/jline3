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

import org.jline.shell.Command;
import org.jline.shell.ExitShellException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ExitCommands}.
 */
class ExitCommandsTest extends AbstractCommandsTest {

    private ExitCommands commands;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        commands = new ExitCommands();
        dispatcher.addGroup(commands);
    }

    /**
     * Test that the exit command throws the expected {@link ExitShellException}.
     */
    @Test
    void exitCommand() {
        Command cmd = commands.command("exit");
        assertNotNull(cmd);
        assertThrows(ExitShellException.class, () -> cmd.execute(session, new String[0]));
    }

    /**
     * Test that the exit command throws the expected {@link ExitShellException} when called from a dispatcher.
     */
    @Test
    void exitCommandFull() {
        assertThrows(ExitShellException.class, () -> dispatcher.execute("exit"));
    }

    /**
     * Test that the quit alias throws the expected {@link ExitShellException}.
     */
    @Test
    void quitAliasCommand() {
        Command cmd = commands.command("quit");
        assertNotNull(cmd);
        assertThrows(ExitShellException.class, () -> cmd.execute(session, new String[0]));
    }
}
