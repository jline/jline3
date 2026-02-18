/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;
import java.util.Map;

import org.jline.shell.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for subcommand support in {@link DefaultCommandDispatcher}.
 */
public class SubcommandTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        dispatcher.addGroup(new SimpleCommandGroup("test", new GitCommand()));
    }

    /**
     * A command with subcommands, simulating "git".
     */
    static class GitCommand extends AbstractCommand {
        private final Map<String, Command> subcommands;

        GitCommand() {
            super("git");
            subcommands = Map.of(
                    "commit", new CommitSubcommand(),
                    "status", new StatusSubcommand());
        }

        @Override
        public String description() {
            return "Version control";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            session.out().println("git: no subcommand specified");
            return "git-main";
        }

        @Override
        public Map<String, Command> subcommands() {
            return subcommands;
        }
    }

    static class CommitSubcommand extends AbstractCommand {
        CommitSubcommand() {
            super("commit");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String msg = String.join(" ", args);
            session.out().println("committed: " + msg);
            return "commit:" + msg;
        }
    }

    static class StatusSubcommand extends AbstractCommand {
        StatusSubcommand() {
            super("status");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            session.out().println("on branch main");
            return "status";
        }
    }

    @Test
    void subcommandRouting() throws Exception {
        Object result = dispatcher.execute("git commit -m msg");
        assertEquals("commit:-m msg", result);
    }

    @Test
    void parentCommandWithoutSubcommand() throws Exception {
        Object result = dispatcher.execute("git");
        assertEquals("git-main", result);
    }

    @Test
    void unknownSubcommandRunsParent() throws Exception {
        // "push" is not a registered subcommand, so it falls through to parent
        // The parent receives ["push"] as args
        Object result = dispatcher.execute("git push");
        assertEquals("git-main", result);
    }

    @Test
    void subcommandWithMultipleArgs() throws Exception {
        Object result = dispatcher.execute("git commit -m hello world");
        assertEquals("commit:-m hello world", result);
    }

    @Test
    void statusSubcommand() throws Exception {
        Object result = dispatcher.execute("git status");
        assertEquals("status", result);
    }

    @Test
    void completerNotNull() {
        assertNotNull(dispatcher.completer());
    }
}
