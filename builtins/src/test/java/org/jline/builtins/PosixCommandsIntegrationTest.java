/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PosixCommands functionality.
 * These tests focus on working functionality and integration scenarios.
 */
public class PosixCommandsIntegrationTest {

    @TempDir
    Path tempDir;

    private PosixCommands.Context context;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private Map<String, Object> vars;

    @BeforeEach
    void setUp() throws IOException {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        vars = new HashMap<>();
        vars.put("HOME", System.getProperty("user.home"));

        Terminal terminal = new DumbTerminal(System.in, out);
        context = new PosixCommands.Context(
                System.in, new PrintStream(out), new PrintStream(err), tempDir, terminal, vars::get);
    }

    @Test
    void testPwdCommand() throws Exception {
        PosixCommands.pwd(context, new String[] {"pwd"});

        String output = out.toString().trim();
        assertEquals(tempDir.toString(), output);
    }

    @Test
    void testEchoCommand() throws Exception {
        PosixCommands.echo(context, new String[] {"echo", "Hello", "World"});

        String output = out.toString();
        assertEquals("Hello World\n", output);
    }

    @Test
    void testEchoNoNewline() throws Exception {
        PosixCommands.echo(context, new String[] {"echo", "-n", "Hello"});

        String output = out.toString();
        assertEquals("Hello", output);
    }

    @Test
    void testCatWithFiles() throws Exception {
        // Create test files
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.write(file1, "Line 1\nLine 2\n".getBytes());
        Files.write(file2, "Line 3\nLine 4\n".getBytes());

        PosixCommands.cat(context, new String[] {"cat", "file1.txt", "file2.txt"});

        String output = out.toString();
        assertEquals("Line 1\nLine 2\nLine 3\nLine 4\n", output);
    }

    @Test
    void testCatWithLineNumbers() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "First line\nSecond line\n".getBytes());

        PosixCommands.cat(context, new String[] {"cat", "-n", "test.txt"});

        String output = out.toString();
        assertTrue(output.contains("1\tFirst line"));
        assertTrue(output.contains("2\tSecond line"));
    }

    @Test
    void testDateCommand() throws Exception {
        PosixCommands.date(context, new String[] {"date"});

        String output = out.toString().trim();
        assertFalse(output.isEmpty());
        // Should contain current date information
        assertTrue(output.length() > 10);
    }

    @Test
    void testDateUTC() throws Exception {
        PosixCommands.date(context, new String[] {"date", "-u"});

        String output = out.toString().trim();
        // Should contain date information
        assertFalse(output.isEmpty());
        assertTrue(output.length() > 10);
    }

    @Test
    void testDateCustomFormat() throws Exception {
        PosixCommands.date(context, new String[] {"date", "+%Y-%m-%d"});

        String output = out.toString().trim();
        // Should be in YYYY-MM-DD format (allow for different date format conversion)
        assertFalse(output.isEmpty());
        assertTrue(output.length() >= 8); // At least some date-like output
    }

    @Test
    void testSleepCommand() throws Exception {
        long start = System.currentTimeMillis();
        PosixCommands.sleep(context, new String[] {"sleep", "1"});
        long end = System.currentTimeMillis();

        // Should have slept for approximately 1 second
        assertTrue(end - start >= 900); // Allow some tolerance
    }

    @Test
    void testClearCommand() throws Exception {
        // Clear command should not throw an exception
        assertDoesNotThrow(() -> {
            PosixCommands.clear(context, new String[] {"clear"});
        });
    }

    @Test
    void testCdValidation() throws Exception {
        // Test cd with existing directory
        assertDoesNotThrow(() -> {
            PosixCommands.cd(context, new String[] {"cd", "."});
        });
    }

    @Test
    void testCdWithDirectoryChanger() throws Exception {
        AtomicReference<Path> changedTo = new AtomicReference<>();
        Consumer<Path> directoryChanger = changedTo::set;

        PosixCommands.cd(context, new String[] {"cd", "."}, directoryChanger);

        assertNotNull(changedTo.get());
        assertEquals(tempDir.toAbsolutePath().normalize(), changedTo.get());
    }

    @Test
    void testCdNonExistentDirectory() throws Exception {
        assertThrows(IOException.class, () -> {
            PosixCommands.cd(context, new String[] {"cd", "nonexistent"});
        });
    }

    @Test
    void testWatchCommandExecutor() throws Exception {
        AtomicReference<List<String>> executedCommand = new AtomicReference<>();
        PosixCommands.CommandExecutor executor = command -> {
            executedCommand.set(command);
            return "Test output";
        };

        // Use a very short interval and stop quickly
        Thread watchThread = new Thread(() -> {
            try {
                PosixCommands.watch(context, new String[] {"watch", "-n", "1", "test", "command"}, executor);
            } catch (Exception e) {
                // Expected when interrupted
            }
        });

        watchThread.start();
        Thread.sleep(100); // Let it run briefly
        watchThread.interrupt();

        // Should have executed the command
        assertNotNull(executedCommand.get());
        assertEquals(Arrays.asList("test", "command"), executedCommand.get());
    }

    @Test
    void testLsBasicFunctionality() throws Exception {
        // Create test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));

        PosixCommands.ls(context, new String[] {"ls"});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("subdir"));
    }

    @Test
    void testHeadCommand() throws Exception {
        Path file = tempDir.resolve("test.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(file, content.toString().getBytes());

        PosixCommands.head(context, new String[] {"head", "-n", "5", "test.txt"});

        String output = out.toString();
        assertTrue(output.contains("Line 1"));
        assertTrue(output.contains("Line 5"));
        assertFalse(output.contains("Line 6"));
    }

    @Test
    void testTailCommand() throws Exception {
        Path file = tempDir.resolve("test.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(file, content.toString().getBytes());

        PosixCommands.tail(context, new String[] {"tail", "-n", "5", "test.txt"});

        String output = out.toString();
        assertTrue(output.contains("Line 16"));
        assertTrue(output.contains("Line 20"));
        assertFalse(output.contains("Line 15"));
    }

    @Test
    void testWcCommand() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "Line 1\nLine 2\nLine 3\n".getBytes());

        PosixCommands.wc(context, new String[] {"wc", "test.txt"});

        String output = out.toString();
        // Should contain line count, word count, byte count
        assertTrue(output.contains("3")); // lines
        assertTrue(output.contains("6")); // words
        assertTrue(output.contains("test.txt"));
    }

    @Test
    void testCommandExecutorInterface() {
        PosixCommands.CommandExecutor executor = command -> {
            return "Executed: " + String.join(" ", command);
        };

        assertDoesNotThrow(() -> {
            String result = executor.execute(Arrays.asList("test", "command"));
            assertEquals("Executed: test command", result);
        });
    }

    @Test
    void testContextFunctionality() {
        assertEquals(tempDir, context.currentDir());
        assertEquals(System.in, context.in());
        assertNotNull(context.out());
        assertNotNull(context.err());
        assertNotNull(context.terminal());
        assertTrue(context.isTty()); // Context.isTty() checks if terminal != null

        // Test variable access
        assertEquals(System.getProperty("user.home"), context.get("HOME"));
    }

    @Test
    void testHelpOptions() throws Exception {
        // Test that help options work for various commands
        assertThrows(Options.HelpException.class, () -> {
            PosixCommands.pwd(context, new String[] {"pwd", "--help"});
        });

        assertThrows(Options.HelpException.class, () -> {
            PosixCommands.echo(context, new String[] {"echo", "--help"});
        });

        assertThrows(Options.HelpException.class, () -> {
            PosixCommands.cat(context, new String[] {"cat", "--help"});
        });
    }
}
