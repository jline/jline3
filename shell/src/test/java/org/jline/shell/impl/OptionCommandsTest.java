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

import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OptionCommands}.
 */
public class OptionCommandsTest {

    private LineReader reader;
    private OptionCommands commands;
    private CommandSession session;
    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void setUp() throws IOException {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        reader = LineReaderBuilder.builder().terminal(terminal).build();
        commands = new OptionCommands(reader);
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        session = new CommandSession(null, System.in, new PrintStream(outCapture), new PrintStream(errCapture));
    }

    @Test
    void groupName() {
        assertEquals("options", commands.name());
    }

    @Test
    void setOptCommand() throws Exception {
        Command cmd = commands.command("setopt");
        assertNotNull(cmd);
        cmd.execute(session, new String[] {"INSERT_BRACKET"});
        assertTrue(reader.isSet(Option.INSERT_BRACKET));
    }

    @Test
    void unsetOptCommand() throws Exception {
        reader.setOpt(Option.INSERT_BRACKET);
        Command cmd = commands.command("unsetopt");
        assertNotNull(cmd);
        cmd.execute(session, new String[] {"INSERT_BRACKET"});
        assertFalse(reader.isSet(Option.INSERT_BRACKET));
    }

    @Test
    void setOptListEnabled() throws Exception {
        reader.setOpt(Option.INSERT_BRACKET);
        Command cmd = commands.command("setopt");
        cmd.execute(session, new String[0]);
        String output = outCapture.toString();
        assertTrue(output.contains("INSERT_BRACKET"));
    }

    @Test
    void setOptUnknownOption() throws Exception {
        Command cmd = commands.command("setopt");
        cmd.execute(session, new String[] {"NONEXISTENT_OPTION"});
        String err = errCapture.toString();
        assertTrue(err.contains("unknown option"));
    }

    @Test
    void setVarCommand() throws Exception {
        Command cmd = commands.command("setvar");
        assertNotNull(cmd);
        cmd.execute(session, new String[] {"MY_VAR", "hello"});
        assertEquals("hello", reader.getVariable("MY_VAR"));
    }

    @Test
    void setVarInteger() throws Exception {
        Command cmd = commands.command("setvar");
        cmd.execute(session, new String[] {"MY_INT", "42"});
        assertEquals(42, reader.getVariable("MY_INT"));
    }

    @Test
    void setVarBoolean() throws Exception {
        Command cmd = commands.command("setvar");
        cmd.execute(session, new String[] {"MY_BOOL", "true"});
        assertEquals(true, reader.getVariable("MY_BOOL"));
    }

    @Test
    void setVarListAll() throws Exception {
        reader.setVariable("TEST_VAR", "test_value");
        Command cmd = commands.command("setvar");
        cmd.execute(session, new String[0]);
        String output = outCapture.toString();
        assertTrue(output.contains("TEST_VAR"));
    }

    @Test
    void setVarShowSingle() throws Exception {
        reader.setVariable("SHOW_ME", "visible");
        Command cmd = commands.command("setvar");
        cmd.execute(session, new String[] {"SHOW_ME"});
        String output = outCapture.toString();
        assertTrue(output.contains("SHOW_ME"));
        assertTrue(output.contains("visible"));
    }
}
