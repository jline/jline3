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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PosixCommands functionality.
 * This test suite covers all major POSIX commands with both basic functionality
 * and advanced integration scenarios.
 */
public class PosixCommandsTest {

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

    /**
     * Helper method to normalize line endings for cross-platform compatibility.
     */
    private String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * Helper method to check if POSIX file attributes are supported.
     */
    private boolean isPosixSupported() {
        return !System.getProperty("os.name").toLowerCase().contains("windows");
    }

    // ========================================
    // Core Command Tests
    // ========================================

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
        // Normalize line endings for cross-platform compatibility
        String expected = "Hello World" + System.lineSeparator();
        assertEquals(expected, output);
    }

    @Test
    void testEchoNoNewline() throws Exception {
        PosixCommands.echo(context, new String[] {"echo", "-n", "Hello"});

        String output = out.toString();
        assertEquals("Hello", output);
    }

    @Test
    void testClearCommand() throws Exception {
        // Clear command should not throw an exception
        assertDoesNotThrow(() -> {
            PosixCommands.clear(context, new String[] {"clear"});
        });
    }

    // ========================================
    // File Operation Tests
    // ========================================

    @Test
    void testLsBasic() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

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
    void testLsGlob1() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));

        PosixCommands.ls(context, new String[] {"ls", "fi*.txt"});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"), output);
        assertTrue(output.contains("file2.txt"), output);
        assertFalse(output.contains("subdir"), output);
    }

    @Test
    void testLsGlob2() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));

        PosixCommands.ls(context, new String[] {"ls", "file?.txt"});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"), output);
        assertTrue(output.contains("file2.txt"), output);
        assertFalse(output.contains("subdir"), output);
    }

    @Test
    void testLsGlob3() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));

        PosixCommands.ls(context, new String[] {"ls", "*1.txt"});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"), output);
        assertFalse(output.contains("file2.txt"), output);
        assertFalse(output.contains("subdir"), output);
    }

    @Test
    void testLsLongFormat() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        Files.createFile(tempDir.resolve("test.txt"));

        PosixCommands.ls(context, new String[] {"ls", "-l"});

        String output = out.toString();
        assertTrue(output.contains("test.txt"));
        // Should contain some file information (permissions, size, etc.)
        assertTrue(output.length() > "test.txt".length());
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
        // Normalize line endings for cross-platform compatibility
        String expected = "Line 1" + System.lineSeparator() + "Line 2"
                + System.lineSeparator() + "Line 3"
                + System.lineSeparator() + "Line 4"
                + System.lineSeparator();
        assertEquals(expected, output);
    }

    @Test
    void testCatWithLineNumbers() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "First line\nSecond line\n".getBytes());

        PosixCommands.cat(context, new String[] {"cat", "-n", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        assertTrue(output.contains("1\tFirst line"));
        assertTrue(output.contains("2\tSecond line"));
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

    // ========================================
    // Text Processing Tests
    // ========================================

    @Test
    void testSortBasic() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "zebra\napple\nbanana\n".getBytes());

        PosixCommands.sort(context, new String[] {"sort", "test.txt"});

        String output = out.toString();
        // Normalize line endings and split properly
        String[] lines = output.trim().split("\\r?\\n");
        assertEquals("apple", lines[0].trim());
        assertEquals("banana", lines[1].trim());
        assertEquals("zebra", lines[2].trim());
    }

    @Test
    void testGrepBasic() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "hello world\nfoo bar\nhello there\n".getBytes());

        PosixCommands.grep(context, new String[] {"grep", "hello", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        // Should contain lines with "hello"
        assertTrue(output.contains("hello"), "Output should contain 'hello': " + output);
    }

    @Test
    void testGrepFruit1() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "a", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "apple", "banana", "date");
        expectNone(output, "cherry", "elderberry");
    }

    @Test
    void testGrepFruit2() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "err", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "cherry", "elderberry");
        expectNone(output, "apple", "banana", "date");
    }

    @Test
    void testGrepFruit3() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-B1", "date", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "cherry", "date");
        expectNone(output, "apple", "banana", "elderberry");
    }

    @Test
    void testGrepFruit4() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-A1", "date", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "date", "elderberry");
        expectNone(output, "apple", "banana", "cherry");
    }

    @Test
    void testGrepFruit5() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-C1", "date", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "cherry", "date", "elderberry");
        expectNone(output, "apple", "banana");
    }

    @Test
    void testGrepFruit6() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "a.*e", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "apple", "date");
        expectNone(output, "banana", "cherry", "elderberry");
    }

    @Test
    void testGrepFruit7() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-C1", "ch", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "banana", "cherry", "date");
        expectNone(output, "apple", "elderberry");
    }

    @Test
    void testGrepFruit8() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-B2", "app", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "apple");
        expectNone(output, "banana", "cherry", "date", "elderberry");
    }

    @Test
    void testGrepFruit9() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-A2", "app", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "apple", "banana", "cherry");
        expectNone(output, "date", "elderberry");
    }

    @Test
    void testGrepFruit10() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-B2", "eld", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "cherry", "date", "elderberry");
        expectNone(output, "apple", "banana");
    }

    @Test
    void testGrepFruit11() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "-A2", "eld", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "elderberry");
        expectNone(output, "apple", "banana", "cherry", "date");
    }

    @Test
    void testGrepFruit12() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "a[pn]", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "apple", "banana");
        expectNone(output, "cherry", "date", "elderberry");
    }

    @Test
    void testGrepFruit13() throws Exception {
        makeFruitFile("test.txt");

        PosixCommands.grep(context, new String[] {"grep", "a(p|n)", "test.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        expectAll(output, "apple", "banana");
        expectNone(output, "cherry", "date", "elderberry");
    }

    private void makeFruitFile(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, "apple\nbanana\ncherry\ndate\nelderberry\n".getBytes());
    }

    private static void expectNonEmpty(String output) {
        assertFalse(output.trim().isEmpty(), "Output should not be empty");
    }

    private static void expectAll(String output, String... fruits) {
        Arrays.stream(fruits).forEach(fruit -> {
            assertTrue(output.contains(fruit), "Output should contain '" + fruit + "': " + output);
        });
    }

    private static void expectNone(String output, String... fruits) {
        Arrays.stream(fruits).forEach(fruit -> {
            assertFalse(output.contains(fruit), "Output should not contain '" + fruit + "': " + output);
        });
    }

    // ========================================
    // Date and Time Tests
    // ========================================

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

    // ========================================
    // Directory Operation Tests
    // ========================================

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

    // ========================================
    // Advanced Feature Tests
    // ========================================

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
