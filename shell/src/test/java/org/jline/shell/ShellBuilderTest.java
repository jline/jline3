/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.reader.LineReader;
import org.jline.shell.impl.AbstractCommand;
import org.jline.shell.impl.SimpleCommandGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ShellBuilder} and {@link Shell}.
 */
class ShellBuilderTest extends AbstractTerminalTest {

    @Test
    void builderCreatesShellWithDefaults() throws Exception {
        try (Shell shell = Shell.builder().terminal(terminal).build()) {
            assertNotNull(shell);
            assertSame(terminal, shell.terminal());
            assertNotNull(shell.reader());
            assertNotNull(shell.dispatcher());
        }
    }

    @Test
    void builderAcceptsCustomPrompt() throws Exception {
        try (Shell shell = Shell.builder().terminal(terminal).prompt("test> ").build()) {
            assertNotNull(shell);
        }
    }

    @Test
    void builderAcceptsPromptSupplier() throws Exception {
        try (Shell shell =
                Shell.builder().terminal(terminal).prompt(() -> "dynamic> ").build()) {
            assertNotNull(shell);
        }
    }

    @Test
    void builderAcceptsVariablesAndOptions() throws Exception {
        try (Shell shell = Shell.builder()
                .terminal(terminal)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M> ")
                .variable(LineReader.INDENTATION, 4)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build()) {
            assertNotNull(shell);
            assertEquals(4, shell.reader().getVariable(LineReader.INDENTATION));
            assertTrue(shell.reader().isSet(LineReader.Option.INSERT_BRACKET));
        }
    }

    @Test
    void builderAcceptsHistoryFile() throws Exception {
        Path historyFile = Path.of(System.getProperty("java.io.tmpdir"), "test-history");
        try (Shell shell =
                Shell.builder().terminal(terminal).historyFile(historyFile).build()) {
            assertNotNull(shell);
        }
    }

    @Test
    void builderAcceptsCommandGroups() throws Exception {
        Command echo = new AbstractCommand("echo") {
            @Override
            public Object execute(CommandSession session, String[] args) {
                return String.join(" ", args);
            }
        };
        CommandGroup group = new SimpleCommandGroup("test", echo);
        try (Shell shell = Shell.builder().terminal(terminal).groups(group).build()) {
            assertNotNull(shell);
            assertNotNull(shell.dispatcher().findCommand("echo"));
        }
    }

    @Test
    void builderAcceptsOnReaderReady() throws Exception {
        boolean[] called = {false};
        try (Shell shell = Shell.builder()
                .terminal(terminal)
                .onReaderReady(reader -> called[0] = true)
                .build()) {
            assertNotNull(shell);
            assertTrue(called[0]);
        }
    }

    /**
     * Test that the exit command exits a {@link Shell} with default settings.
     */
    @Test
    void exitShellCommand() throws IOException {
        terminalInput.write("exit\n".getBytes());
        try (Shell shell = Shell.builder().terminal(terminal).build()) {
            assertTimeoutPreemptively(Duration.of(2000, ChronoUnit.MILLIS), shell::run);
        }
    }

    /**
     * Test that the exit command in a {@link Shell} can be disabled.
     */
    @Test
    void noExitShellCommand() throws Exception {
        AtomicBoolean stopped = new AtomicBoolean(false);
        try (Shell shell =
                Shell.builder().terminal(terminal).exitCommands(false).build()) {
            terminalInput.write("exit\n".getBytes(terminal.encoding()));
            terminalInput.flush();
            Thread shellThread = Thread.currentThread();
            Thread thread = new Thread(
                    () -> {
                        try {
                            Thread.sleep(2000);
                            stopped.set(true);
                            shell.stop();
                            shellThread.interrupt();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            // Ignore
                        }
                    },
                    "shell-stop-test-thread");
            thread.start();
            shell.run();
            if (thread.isAlive()) {
                thread.interrupt();
                thread.join();
            }
            assertTrue(stopped.get(), "Shell should ignore the 'exit' command.");
        }
    }
}
