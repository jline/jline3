/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.List;

import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleCommandGroupTest {

    @Test
    void groupContainsCommands() {
        Command echo = new AbstractCommand("echo", "e") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                return String.join(" ", args);
            }
        };

        SimpleCommandGroup group = new SimpleCommandGroup("test", echo);
        assertEquals("test", group.name());
        assertEquals(1, group.commands().size());
    }

    @Test
    void findCommandByName() {
        Command cmd = new AbstractCommand("hello") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                return "hello";
            }
        };

        SimpleCommandGroup group = new SimpleCommandGroup("grp", cmd);
        assertSame(cmd, group.command("hello"));
        assertNull(group.command("nonexistent"));
    }

    @Test
    void findCommandByAlias() {
        Command cmd = new AbstractCommand("hello", "hi", "hey") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                return "hello";
            }
        };

        SimpleCommandGroup group = new SimpleCommandGroup("grp", cmd);
        assertSame(cmd, group.command("hi"));
        assertSame(cmd, group.command("hey"));
        assertTrue(group.hasCommand("hi"));
    }

    @Test
    void abstractCommandProperties() {
        Command cmd = new AbstractCommand("test", "t") {
            @Override
            public String description() {
                return "a test command";
            }

            @Override
            public Object execute(CommandSession session, String[] args) {
                return null;
            }
        };

        assertEquals("test", cmd.name());
        assertEquals(List.of("t"), cmd.aliases());
        assertEquals("a test command", cmd.description());
    }
}
