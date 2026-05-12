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
import org.jline.shell.CommandSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HelpCommands}.
 */
class HelpCommandsTest extends AbstractCommandsTest {

    private HelpCommands commands;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        dispatcher.addGroup(new SimpleCommandGroup("demo", new TestEchoCommand(), new TestUpperCmd()));
        commands = new HelpCommands(dispatcher);
        dispatcher.addGroup(commands);
    }

    static class TestUpperCmd extends AbstractCommand {
        TestUpperCmd() {
            super("upper");
        }

        @Override
        public String description() {
            return "Convert to upper case";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return String.join(" ", args).toUpperCase();
        }
    }

    @Test
    void groupName() {
        assertEquals("help", commands.name());
    }

    @Test
    void helpCommandExists() {
        Command cmd = commands.command("help");
        assertNotNull(cmd);
        assertNotNull(commands.command("?")); // alias
    }

    @Test
    void helpListAll() throws Exception {
        Command cmd = commands.command("help");
        cmd.execute(session, new String[0]);
        String output = outCapture.toString();
        assertTrue(output.contains("echo"));
        assertTrue(output.contains("upper"));
        assertTrue(output.contains("demo"));
    }

    @Test
    void helpSpecificCommand() throws Exception {
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"echo"});
        String output = outCapture.toString();
        assertTrue(output.contains("echo"));
        assertTrue(output.contains("Echo arguments"));
    }

    @Test
    void helpUnknownCommand() throws Exception {
        Command cmd = commands.command("help");
        cmd.execute(session, new String[] {"nonexistent"});
        String err = errCapture.toString();
        assertTrue(err.contains("unknown command"));
    }
}
