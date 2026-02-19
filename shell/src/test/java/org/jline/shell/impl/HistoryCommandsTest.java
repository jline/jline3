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
import org.jline.reader.LineReaderBuilder;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HistoryCommands}.
 */
public class HistoryCommandsTest {

    private LineReader reader;
    private HistoryCommands commands;
    private CommandSession session;
    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void setUp() throws IOException {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        reader = LineReaderBuilder.builder().terminal(terminal).build();
        commands = new HistoryCommands(reader);
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        session = new CommandSession(null, System.in, new PrintStream(outCapture), new PrintStream(errCapture));
    }

    @Test
    void groupName() {
        assertEquals("history", commands.name());
    }

    @Test
    void historyCommandExists() {
        Command cmd = commands.command("history");
        assertNotNull(cmd);
        assertEquals("Display or manage command history", cmd.description());
    }

    @Test
    void historyListEmpty() throws Exception {
        Command cmd = commands.command("history");
        cmd.execute(session, new String[0]);
        // No entries, no output
    }

    @Test
    void historyWithEntries() throws Exception {
        reader.getHistory().add("echo hello");
        reader.getHistory().add("echo world");
        Command cmd = commands.command("history");
        cmd.execute(session, new String[0]);
        String output = outCapture.toString();
        assertTrue(output.contains("echo hello"));
        assertTrue(output.contains("echo world"));
    }

    @Test
    void historyLastN() throws Exception {
        reader.getHistory().add("cmd1");
        reader.getHistory().add("cmd2");
        reader.getHistory().add("cmd3");
        Command cmd = commands.command("history");
        cmd.execute(session, new String[] {"2"});
        String output = outCapture.toString();
        assertFalse(output.contains("cmd1"));
        assertTrue(output.contains("cmd2"));
        assertTrue(output.contains("cmd3"));
    }

    @Test
    void historyClear() throws Exception {
        reader.getHistory().add("cmd1");
        Command cmd = commands.command("history");
        cmd.execute(session, new String[] {"-c"});
        assertEquals(0, reader.getHistory().size());
        assertTrue(outCapture.toString().contains("cleared"));
    }

    @Test
    void historySearchPattern() throws Exception {
        reader.getHistory().add("echo hello");
        reader.getHistory().add("ls -la");
        reader.getHistory().add("echo world");
        Command cmd = commands.command("history");
        cmd.execute(session, new String[] {"/echo"});
        String output = outCapture.toString();
        assertTrue(output.contains("echo hello"));
        assertTrue(output.contains("echo world"));
        assertFalse(output.contains("ls -la"));
    }

    @Test
    void historyInvalidArg() throws Exception {
        Command cmd = commands.command("history");
        cmd.execute(session, new String[] {"abc"});
        String err = errCapture.toString();
        assertTrue(err.contains("invalid"));
    }
}
