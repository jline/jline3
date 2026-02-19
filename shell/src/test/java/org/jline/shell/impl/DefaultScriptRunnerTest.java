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
import java.util.ArrayList;
import java.util.List;

import org.jline.shell.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultScriptRunner}.
 */
public class DefaultScriptRunnerTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;
    private DefaultScriptRunner runner;
    private List<String> executedCommands;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        runner = new DefaultScriptRunner();
        executedCommands = new ArrayList<>();

        // Track which commands are executed
        dispatcher.addGroup(new SimpleCommandGroup("test", new AbstractCommand("track") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                String line = String.join(" ", args);
                executedCommands.add(line);
                return line;
            }
        }));
    }

    @Test
    void multiLineScript(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("test.sh");
        Files.writeString(script, "track line1\ntrack line2\ntrack line3\n");

        runner.execute(script, dispatcher.session(), dispatcher);

        assertEquals(3, executedCommands.size());
        assertEquals("line1", executedCommands.get(0));
        assertEquals("line2", executedCommands.get(1));
        assertEquals("line3", executedCommands.get(2));
    }

    @Test
    void commentsSkipped(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("test.sh");
        Files.writeString(script, "# This is a comment\ntrack hello\n# Another comment\ntrack world\n");

        runner.execute(script, dispatcher.session(), dispatcher);

        assertEquals(2, executedCommands.size());
        assertEquals("hello", executedCommands.get(0));
        assertEquals("world", executedCommands.get(1));
    }

    @Test
    void blankLinesSkipped(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("test.sh");
        Files.writeString(script, "\ntrack hello\n\n\ntrack world\n\n");

        runner.execute(script, dispatcher.session(), dispatcher);

        assertEquals(2, executedCommands.size());
    }

    @Test
    void lineContinuation(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("test.sh");
        Files.writeString(script, "track hello\\\n world\n");

        runner.execute(script, dispatcher.session(), dispatcher);

        assertEquals(1, executedCommands.size());
        assertEquals("hello world", executedCommands.get(0));
    }

    @Test
    void missingFileThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> runner.execute(Path.of("/nonexistent/script.sh"), null, dispatcher));
    }

    @Test
    void nullFileThrows() {
        assertThrows(IllegalArgumentException.class, () -> runner.execute(null, null, dispatcher));
    }

    @Test
    void sourceCommand(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("test.sh");
        Files.writeString(script, "track sourced\n");

        ScriptCommands cmds = new ScriptCommands(runner, dispatcher);
        dispatcher.addGroup(cmds);

        CommandSession session = dispatcher.session();
        session.setWorkingDirectory(tempDir);

        dispatcher.execute("source " + script);

        assertEquals(1, executedCommands.size());
        assertEquals("sourced", executedCommands.get(0));
    }

    @Test
    void emptyScript(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("empty.sh");
        Files.writeString(script, "");

        runner.execute(script, dispatcher.session(), dispatcher);

        assertTrue(executedCommands.isEmpty());
    }

    @Test
    void commentOnlyScript(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("comments.sh");
        Files.writeString(script, "# comment 1\n# comment 2\n");

        runner.execute(script, dispatcher.session(), dispatcher);

        assertTrue(executedCommands.isEmpty());
    }
}
