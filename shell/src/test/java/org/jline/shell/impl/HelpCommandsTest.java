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

import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HelpCommands}.
 */
public class HelpCommandsTest {

    private DefaultCommandDispatcher dispatcher;
    private HelpCommands commands;
    private CommandSession session;
    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void setUp() throws IOException {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        dispatcher.addGroup(new SimpleCommandGroup("demo", new TestEchoCmd(), new TestUpperCmd()));
        commands = new HelpCommands(dispatcher);
        dispatcher.addGroup(commands);
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        session = new CommandSession(null, System.in, new PrintStream(outCapture), new PrintStream(errCapture));
    }

    static class TestEchoCmd extends AbstractCommand {
        TestEchoCmd() {
            super("echo");
        }

        @Override
        public String description() {
            return "Echo arguments to output";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return String.join(" ", args);
        }
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
