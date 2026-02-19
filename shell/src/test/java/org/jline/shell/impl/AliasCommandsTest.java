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
import java.io.PrintStream;

import org.jline.shell.AliasManager;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AliasCommands}.
 */
public class AliasCommandsTest {

    private AliasManager aliasManager;
    private AliasCommands commands;
    private CommandSession session;
    private ByteArrayOutputStream outCapture;
    private ByteArrayOutputStream errCapture;

    @BeforeEach
    void setUp() {
        aliasManager = new DefaultAliasManager();
        commands = new AliasCommands(aliasManager);
        outCapture = new ByteArrayOutputStream();
        errCapture = new ByteArrayOutputStream();
        session = new CommandSession(null, System.in, new PrintStream(outCapture), new PrintStream(errCapture));
    }

    @Test
    void aliasDefine() throws Exception {
        Command alias = commands.command("alias");
        assertNotNull(alias);
        alias.execute(session, new String[] {"ll=ls -la"});
        assertEquals("ls -la", aliasManager.getAlias("ll"));
    }

    @Test
    void aliasListAll() throws Exception {
        aliasManager.setAlias("ll", "ls -la");
        aliasManager.setAlias("gs", "git status");
        Command alias = commands.command("alias");
        alias.execute(session, new String[0]);
        String output = outCapture.toString();
        assertTrue(output.contains("ll"));
        assertTrue(output.contains("gs"));
    }

    @Test
    void aliasShowOne() throws Exception {
        aliasManager.setAlias("ll", "ls -la");
        Command alias = commands.command("alias");
        alias.execute(session, new String[] {"ll"});
        String output = outCapture.toString();
        assertTrue(output.contains("ll"));
        assertTrue(output.contains("ls -la"));
    }

    @Test
    void aliasShowNotFound() throws Exception {
        Command alias = commands.command("alias");
        alias.execute(session, new String[] {"nonexistent"});
        String err = errCapture.toString();
        assertTrue(err.contains("not found"));
    }

    @Test
    void unalias() throws Exception {
        aliasManager.setAlias("ll", "ls -la");
        Command unalias = commands.command("unalias");
        assertNotNull(unalias);
        unalias.execute(session, new String[] {"ll"});
        assertNull(aliasManager.getAlias("ll"));
    }

    @Test
    void unaliasNotFound() throws Exception {
        Command unalias = commands.command("unalias");
        unalias.execute(session, new String[] {"nonexistent"});
        String err = errCapture.toString();
        assertTrue(err.contains("not found"));
    }

    @Test
    void unaliasNoArgs() throws Exception {
        Command unalias = commands.command("unalias");
        unalias.execute(session, new String[0]);
        String err = errCapture.toString();
        assertTrue(err.contains("usage"));
    }

    @Test
    void groupName() {
        assertEquals("aliases", commands.name());
    }
}
