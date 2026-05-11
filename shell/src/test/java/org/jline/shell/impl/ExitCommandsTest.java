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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.reader.EndOfFileException;
import org.jline.shell.Command;
import org.jline.shell.Shell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ExitCommands}.
 */
public class ExitCommandsTest extends AbstractCommandsTest {

    private ExitCommands commands;

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        commands = new ExitCommands();
        dispatcher.addGroup(commands);
    }

    /**
     * Test that the exit command throws the expected {@link EndOfFileException}.
     */
    @Test
    void exitCommand() {
        Command cmd = commands.command("exit");
        assertNotNull(cmd);
        assertThrows(EndOfFileException.class, () -> cmd.execute(session, new String[0]));
    }

    /**
     * Test that the exit command throws the expected {@link EndOfFileException} when called from a dispatcher.
     */
    @Test
    void exitCommandFull() {
        assertThrows(EndOfFileException.class, () -> dispatcher.execute("exit"));
    }

    /**
     * Test that the quit alias throws the expected {@link EndOfFileException}.
     */
    @Test
    void quitAliasCommand() {
        Command cmd = commands.command("quit");
        assertNotNull(cmd);
        assertThrows(EndOfFileException.class, () -> cmd.execute(session, new String[0]));
    }

    /**
     * Test that the exit command exits a {@link Shell} with default settings.
     */
    @Test
    void exitShellCommand() throws IOException {
        terminalInput.write("exit\n".getBytes());
        try (Shell shell = Shell.builder().terminal(terminal).build()) {
            assertTimeoutPreemptively(Duration.of(1000, ChronoUnit.MILLIS), shell::run);
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
                            Thread.sleep(1000);
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
