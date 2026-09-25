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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HistoryCommands}.
 */
class HistoryCommandsTest extends AbstractCommandsTest {

    private HistoryCommands commands;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        commands = new HistoryCommands(reader);
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
    void historyListEmpty() {
        Command cmd = commands.command("history");
        assertDoesNotThrow(() -> cmd.execute(session, new String[0]));
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

    @Test
    void historySearchInvalidRegex() throws Exception {
        Command cmd = commands.command("history");
        cmd.execute(session, new String[] {"/[unclosed"});
        String err = errCapture.toString();
        assertTrue(err.contains("invalid regex"), "Expected 'invalid regex' in: " + err);
        assertTrue(outCapture.toString().isEmpty(), "Expected no output on stderr-only path");
    }

    @Test
    void historySearchTimeoutReportsError() throws Exception {
        // Use a -1 ms regex timeout via the package-private HistoryCommands constructor so this
        // test is deterministic across JDK versions.  Newer JDKs (25+) optimise away the
        // catastrophic backtracking in patterns like (a+)+b, making a reliance on pathological
        // runtime fragile.
        //
        // SafeRegex.TimeoutCharSequence checks the deadline every CHECK_INTERVAL (1024) charAt
        // calls: the first check sets the deadline, the second checks it.  A -1 ms timeout means
        // timeoutNanos = -1_000_000, so at call 1024 the deadline is set to "now - 1ms" (in the
        // past), and the check at call 2048 unconditionally fires since now > deadline.  We use a
        // 3000-char input with /.*/ to guarantee >= 2048 charAt calls.
        String input = "a".repeat(3000);
        reader.getHistory().add(input);
        HistoryCommands timedOut = new HistoryCommands(reader, -1L);
        Command cmd = timedOut.command("history");
        cmd.execute(session, new String[] {"/.*"});
        String err = errCapture.toString();
        assertTrue(err.contains("timed out"), "Expected timeout error in stderr, got: " + err);
        assertTrue(outCapture.toString().isEmpty(), "Expected no history output when regex times out");
    }
}
