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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.shell.CommandSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VariableCommands} and bare variable assignment in
 * {@link DefaultCommandDispatcher}.
 */
class VariableCommandsTest extends AbstractCommandsTest {

    private final AtomicBoolean marked = new AtomicBoolean();

    @TempDir
    Path tempDir;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
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
        dispatcher.addGroup(new SimpleCommandGroup("marker", new AbstractCommand("mark") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                marked.set(true);
                return null;
            }
        }));
    }

    @Override
    protected DefaultCommandDispatcher createDispatcher() {
        return new DefaultCommandDispatcher(terminal, null, null, null, new DefaultLineExpander(), null);
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

    @Test
    void expandedValueDoesNotStartAnotherCommand() throws Exception {
        dispatcher.session().put("MSG", "hello; mark");
        Object result = dispatcher.execute("echo $MSG");
        assertFalse(marked.get(), "operator inside a variable value must not run a second command");
        assertEquals("hello; mark", result);
    }

    @Test
    void expandedValueDoesNotAddRedirection() throws Exception {
        Path target = tempDir.resolve("out.txt");
        dispatcher.session().put("MSG", "hello > " + target);
        dispatcher.execute("echo $MSG");
        assertFalse(Files.exists(target), "operator inside a variable value must not redirect output");
    }

    @Test
    void redirectTargetFromVariable() throws Exception {
        Path target = tempDir.resolve("target.txt");
        dispatcher.session().put("OUT", target.toString());
        dispatcher.execute("echo hello > $OUT");
        assertEquals("hello", Files.readString(target).trim());
    }

    @Test
    void redirectTargetWithTilde() throws Exception {
        String home = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        try {
            dispatcher.execute("echo hello > ~/tilde.txt");
        } finally {
            System.setProperty("user.home", home);
        }
        assertEquals("hello", Files.readString(tempDir.resolve("tilde.txt")).trim());
    }
}
