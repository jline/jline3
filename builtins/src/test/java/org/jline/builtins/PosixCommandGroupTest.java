/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PosixCommandGroup}.
 */
public class PosixCommandGroupTest {

    private PosixCommandGroup group;
    private Terminal terminal;

    @BeforeEach
    void setUp() throws Exception {
        group = new PosixCommandGroup();
        terminal = TerminalBuilder.builder().dumb(true).build();
    }

    @Test
    void commandsListNotEmpty() {
        assertFalse(group.commands().isEmpty());
    }

    @Test
    void hasEchoCommand() {
        Command echo = group.command("echo");
        assertNotNull(echo);
        assertEquals("echo", echo.name());
    }

    @Test
    void hasPwdCommand() {
        Command pwd = group.command("pwd");
        assertNotNull(pwd);
    }

    @Test
    void hasCdCommand() {
        Command cd = group.command("cd");
        assertNotNull(cd);
    }

    @Test
    void echoExecution() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        CommandSession session = new CommandSession(terminal, System.in, out, new PrintStream(terminal.output()));
        session.setWorkingDirectory(Path.of(System.getProperty("user.dir")));

        Command echo = group.command("echo");
        echo.execute(session, new String[] {"hello", "world"});

        out.flush();
        String output = baos.toString();
        assertTrue(output.contains("hello world"), "Expected 'hello world' in output: " + output);
    }

    @Test
    void pwdExecution(@TempDir Path tempDir) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        CommandSession session = new CommandSession(terminal, System.in, out, new PrintStream(terminal.output()));
        session.setWorkingDirectory(tempDir);

        Command pwd = group.command("pwd");
        pwd.execute(session, new String[0]);

        out.flush();
        String output = baos.toString();
        assertTrue(output.contains(tempDir.toString()), "Expected working directory in output: " + output);
    }

    @Test
    void cdUpdatesSessionWorkingDirectory(@TempDir Path tempDir) throws Exception {
        Path subDir = tempDir.resolve("sub");
        Files.createDirectories(subDir);

        CommandSession session = new CommandSession(terminal);
        session.setWorkingDirectory(tempDir);

        Command cd = group.command("cd");
        cd.execute(session, new String[] {subDir.toString()});

        assertEquals(subDir, session.workingDirectory());
    }

    @Test
    void ioStreamsPassedCorrectly() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBaos);
        CommandSession session = new CommandSession(terminal, System.in, out, err);
        session.setWorkingDirectory(Path.of(System.getProperty("user.dir")));

        Command echo = group.command("echo");
        echo.execute(session, new String[] {"test"});

        out.flush();
        assertTrue(baos.toString().contains("test"));
    }

    @Test
    void groupName() {
        assertEquals("POSIX", group.name());
    }
}
