/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

import org.jline.console.impl.DefaultJobManager;
import org.jline.reader.LineReader;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ShellBuilder} and {@link Shell}.
 */
public class ShellBuilderTest {

    @Test
    void builderCreatesShellWithDefaults() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Shell shell = Shell.builder().terminal(terminal).build();
        assertNotNull(shell);
        assertSame(terminal, shell.terminal());
        assertNotNull(shell.reader());
        assertNotNull(shell.systemRegistry());
        shell.close();
    }

    @Test
    void builderAcceptsCustomPrompt() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Shell shell = Shell.builder().terminal(terminal).prompt("test> ").build();
        assertNotNull(shell);
        shell.close();
    }

    @Test
    void builderAcceptsPromptSupplier() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Shell shell =
                Shell.builder().terminal(terminal).prompt(() -> "dynamic> ").build();
        assertNotNull(shell);
        shell.close();
    }

    @Test
    void builderAcceptsVariablesAndOptions() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Shell shell = Shell.builder()
                .terminal(terminal)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M> ")
                .variable(LineReader.INDENTATION, 4)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build();
        assertNotNull(shell);
        assertEquals(4, shell.reader().getVariable(LineReader.INDENTATION));
        assertTrue(shell.reader().isSet(LineReader.Option.INSERT_BRACKET));
        shell.close();
    }

    @Test
    void builderAcceptsHistoryFile() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Path historyFile = Paths.get(System.getProperty("java.io.tmpdir"), "test-history");
        Shell shell =
                Shell.builder().terminal(terminal).historyFile(historyFile).build();
        assertNotNull(shell);
        shell.close();
    }

    @Test
    void builderAcceptsWorkDir() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Supplier<Path> workDir = () -> Paths.get("/tmp");
        Shell shell = Shell.builder().terminal(terminal).workDir(workDir).build();
        assertNotNull(shell);
        shell.close();
    }

    @Test
    void builderAcceptsCommandRegistries() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        CommandRegistry registry = new SimpleTestRegistry();
        Shell shell = Shell.builder().terminal(terminal).commands(registry).build();
        assertNotNull(shell);
        assertTrue(shell.systemRegistry().hasCommand("test-cmd"));
        shell.close();
    }

    @Test
    void builderAcceptsJobManager() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        DefaultJobManager jobManager = new DefaultJobManager();
        Shell shell = Shell.builder().terminal(terminal).jobManager(jobManager).build();
        assertNotNull(shell);
        shell.close();
    }

    @Test
    void builderDisableTailTipWidgets() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        Shell shell = Shell.builder().terminal(terminal).tailTipWidgets(false).build();
        assertNotNull(shell);
        shell.close();
    }

    /**
     * Simple command registry for testing.
     */
    private static class SimpleTestRegistry implements CommandRegistry {
        @Override
        public Set<String> commandNames() {
            return Collections.singleton("test-cmd");
        }

        @Override
        public Map<String, String> commandAliases() {
            return Collections.emptyMap();
        }

        @Override
        public List<String> commandInfo(String command) {
            return Collections.singletonList("A test command");
        }

        @Override
        public boolean hasCommand(String command) {
            return "test-cmd".equals(command);
        }

        @Override
        public SystemCompleter compileCompleters() {
            return new SystemCompleter();
        }

        @Override
        public CmdDesc commandDescription(List<String> args) {
            return null;
        }
    }
}
