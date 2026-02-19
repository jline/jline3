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

import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VariableCommands} and bare variable assignment in
 * {@link DefaultCommandDispatcher}.
 */
public class VariableCommandsTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal, null, null, null, new DefaultLineExpander(), null);
        dispatcher.addGroup(new VariableCommands());
        // Add echo for verification
        dispatcher.addGroup(new SimpleCommandGroup("test", new AbstractCommand("echo") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                String msg = String.join(" ", args);
                session.out().println(msg);
                return msg;
            }
        }));
    }

    @Test
    void bareAssignment() throws Exception {
        dispatcher.execute("FOO=bar");
        assertEquals("bar", dispatcher.session().get("FOO"));
    }

    @Test
    void bareAssignmentWithValue() throws Exception {
        dispatcher.execute("MY_VAR=hello world");
        // Bare assignment captures everything after '='
        assertEquals("hello world", dispatcher.session().get("MY_VAR"));
    }

    @Test
    void setCommand() throws Exception {
        dispatcher.execute("set NAME=world");
        assertEquals("world", dispatcher.session().get("NAME"));
    }

    @Test
    void setCommandSpaceForm() throws Exception {
        dispatcher.execute("set GREETING hello");
        assertEquals("hello", dispatcher.session().get("GREETING"));
    }

    @Test
    void unsetCommand() throws Exception {
        dispatcher.session().put("TEMP", "value");
        dispatcher.execute("unset TEMP");
        assertNull(dispatcher.session().get("TEMP"));
    }

    @Test
    void exportCommand() throws Exception {
        dispatcher.execute("export COLOR=blue");
        assertEquals("blue", dispatcher.session().get("COLOR"));
    }

    @Test
    void variableUsedInExpansion() throws Exception {
        dispatcher.execute("FOO=bar");
        Object result = dispatcher.execute("echo $FOO");
        assertEquals("bar", result);
    }

    @Test
    void setAndExpandAdvanced() throws Exception {
        dispatcher.execute("set NAME=world");
        Object result = dispatcher.execute("echo ${NAME:-default}");
        assertEquals("world", result);
    }

    @Test
    void unsetAndExpandDefault() throws Exception {
        dispatcher.execute("set NAME=world");
        dispatcher.execute("unset NAME");
        Object result = dispatcher.execute("echo ${NAME:-default}");
        assertEquals("default", result);
    }
}
