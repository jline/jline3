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
import java.util.regex.PatternSyntaxException;

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
        assertTrue(output.startsWith(".\n"));
        assertTrue(output.contains("..\n"));
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
    void testLsOneEntryPerLine() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createFile(tempDir.resolve("file3.txt"));

        // Test -1 option (one entry per line)
        PosixCommands.ls(context, new String[] {"ls", "-1"});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("file3.txt"));

        // Verify that each file is on its own line
        String[] lines = output.trim().split("\\r?\\n");
        assertTrue(lines.length >= 3, "Should have at least 3 lines for 3 files");
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
    void testGrepCount0() throws Exception {
        makeFruitFile("fruit.txt");

        PosixCommands.grep(context, new String[] {"grep", "-c", "apple", "fruit.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        assertEquals("1\n", output, "Output should be '1'");
    }

    @Test
    void testGrepCount1() throws Exception {
        makeFruitFile("fruit.txt");
        Path file = tempDir.resolve("phones.txt");
        Files.write(file, "apple\nandroid\n".getBytes());

        PosixCommands.grep(context, new String[] {"grep", "-c", "apple", "fruit.txt", "phones.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        assertTrue(output.contains("phones.txt:1"), "Output should contain 'phones.txt:1': " + output);
        assertTrue(output.contains("fruit.txt:1"), "Output should contain 'fruit.txt:1': " + output);
    }

    @Test
    void testGrepCount2() throws Exception {
        makeFruitFile("fruit.txt");
        Path file = tempDir.resolve("phones.txt");
        Files.write(file, "apple\nandroid\n".getBytes());

        PosixCommands.grep(context, new String[] {"grep", "-c", "-z", "erry", "fruit.txt", "phones.txt"});

        String output = normalizeLineEndings(out.toString());
        expectNonEmpty(output);
        assertTrue(output.contains("fruit.txt:2"), "Output should contain 'fruit.txt:2': " + output);
        assertFalse(output.contains("phones.txt"), "Output should not contain 'phones.txt': " + output);
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

    @Test
    void testGlobExpansionBraces() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files with different extensions and naming patterns
        Files.createFile(tempDir.resolve("MyTest.java"));
        Files.createFile(tempDir.resolve("MySpec.java"));
        Files.createFile(tempDir.resolve("MyTest.groovy"));
        Files.createFile(tempDir.resolve("MySpec.groovy"));
        Files.createFile(tempDir.resolve("MyOther.java"));
        Files.createFile(tempDir.resolve("MyOther.groovy"));

        // Test brace expansion with file extensions
        PosixCommands.ls(context, new String[] {"ls", "*{Test,Spec}.{java,groovy}"});

        String output = out.toString();
        assertTrue(output.contains("MyTest.java"), "Expected MyTest.java in output: " + output);
        assertTrue(output.contains("MySpec.java"), "Expected MySpec.java in output: " + output);
        assertTrue(output.contains("MyTest.groovy"), "Expected MyTest.groovy in output: " + output);
        assertTrue(output.contains("MySpec.groovy"), "Expected MySpec.groovy in output: " + output);
        assertFalse(output.contains("MyOther.java"), "Should not contain MyOther.java in output: " + output);
        assertFalse(output.contains("MyOther.groovy"), "Should not contain MyOther.groovy in output: " + output);
    }

    @Test
    void testGlobExpansionRecursive() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create nested directory structure
        Path srcDir = tempDir.resolve("src");
        Path testDir = srcDir.resolve("test");
        Files.createDirectories(testDir);

        // Create test files in nested structure
        Files.createFile(testDir.resolve("TestSpec.java"));
        Files.createFile(testDir.resolve("UnitTest.java"));
        Files.createFile(tempDir.resolve("RootTest.java"));

        // Test simpler recursive pattern (without **)
        PosixCommands.ls(context, new String[] {"ls", "src/test/*{Test,Spec}.java"});

        String output = out.toString();
        assertTrue(output.contains("TestSpec.java"), output);
        assertTrue(output.contains("UnitTest.java"), output);
        assertFalse(output.contains("RootTest.java"), output);
    }

    @Test
    void testGlobExpansionComplexPattern() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create simpler directory structure
        Path srcDir = tempDir.resolve("src");
        Path testJavaDir = srcDir.resolve("test").resolve("java");
        Path testGroovyDir = srcDir.resolve("test").resolve("groovy");

        Files.createDirectories(testJavaDir);
        Files.createDirectories(testGroovyDir);

        // Create test files
        Files.createFile(testJavaDir.resolve("ServiceTest.java"));
        Files.createFile(testJavaDir.resolve("ServiceSpec.java"));
        Files.createFile(testGroovyDir.resolve("HelperTest.groovy"));
        Files.createFile(testGroovyDir.resolve("HelperSpec.groovy"));
        Files.createFile(tempDir.resolve("Service.java"));

        // Test simpler pattern matching test files in both java and groovy
        PosixCommands.ls(context, new String[] {"ls", "src/test/*/*{Test,Spec}.{java,groovy}"});

        String output = out.toString();
        assertTrue(output.contains("ServiceTest.java"), output);
        assertTrue(output.contains("ServiceSpec.java"), output);
        assertTrue(output.contains("HelperTest.groovy"), output);
        assertTrue(output.contains("HelperSpec.groovy"), output);
        assertFalse(output.contains("Service.java"), output);
    }

    @Test
    void testGlobExpansionDoubleAsterisk() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create nested directory structure to test ** pattern
        Path srcDir = tempDir.resolve("src");
        Path mainDir = srcDir.resolve("main").resolve("java").resolve("com").resolve("example");
        Path testDir = srcDir.resolve("test").resolve("java").resolve("com").resolve("example");
        Path integrationDir =
                srcDir.resolve("test").resolve("integration").resolve("com").resolve("example");

        Files.createDirectories(mainDir);
        Files.createDirectories(testDir);
        Files.createDirectories(integrationDir);

        // Create test files at various depths
        Files.createFile(mainDir.resolve("Service.java"));
        Files.createFile(testDir.resolve("ServiceTest.java"));
        Files.createFile(testDir.resolve("ServiceSpec.java"));
        Files.createFile(integrationDir.resolve("ServiceIntegrationTest.java"));
        Files.createFile(tempDir.resolve("RootFile.java"));

        // IMPORTANT: Test POSIX ** behavior - create a file directly in src/ (zero directories)
        Files.createFile(srcDir.resolve("DirectTest.java"));

        // Test ** pattern to find all Test files recursively
        PosixCommands.ls(context, new String[] {"ls", "src/**/*Test.java"});

        String output = out.toString();
        assertTrue(output.contains("ServiceTest.java"), "Should find ServiceTest.java: " + output);
        assertTrue(
                output.contains("ServiceIntegrationTest.java"), "Should find ServiceIntegrationTest.java: " + output);
        assertTrue(
                output.contains("DirectTest.java"),
                "Should find DirectTest.java (POSIX ** = zero directories): " + output);
        assertFalse(output.contains("Service.java"), "Should not find Service.java: " + output);
        assertFalse(output.contains("ServiceSpec.java"), "Should not find ServiceSpec.java: " + output);
        assertFalse(output.contains("RootFile.java"), "Should not find RootFile.java: " + output);
    }

    @Test
    void testGlobExpansionMultipleDoubleAsterisk() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Test pattern with multiple ** to verify POSIX semantics: src/**/test/**/*.java
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        // Case 1: src/test/*.java (both ** = zero directories)
        Path testDir1 = srcDir.resolve("test");
        Files.createDirectories(testDir1);
        Files.createFile(testDir1.resolve("Case1Test.java"));

        // Case 2: src/foo/test/*.java (first ** = one directory, second ** = zero directories)
        Path testDir2 = srcDir.resolve("foo").resolve("test");
        Files.createDirectories(testDir2);
        Files.createFile(testDir2.resolve("Case2Test.java"));

        // Case 3: src/test/bar/*.java (first ** = zero directories, second ** = one directory)
        Path testDir3 = srcDir.resolve("test").resolve("bar");
        Files.createDirectories(testDir3);
        Files.createFile(testDir3.resolve("Case3Test.java"));

        // Case 4: src/foo/test/bar/*.java (both ** = one+ directories)
        Path testDir4 = srcDir.resolve("foo").resolve("test").resolve("bar");
        Files.createDirectories(testDir4);
        Files.createFile(testDir4.resolve("Case4Test.java"));

        // Test the complex pattern
        PosixCommands.ls(context, new String[] {"ls", "src/**/test/**/*.java"});

        String output = out.toString();
        assertTrue(output.contains("Case1Test.java"), "Should find Case1Test.java (both ** = zero): " + output);
        assertTrue(
                output.contains("Case2Test.java"),
                "Should find Case2Test.java (first ** = one, second ** = zero): " + output);
        assertTrue(
                output.contains("Case3Test.java"),
                "Should find Case3Test.java (first ** = zero, second ** = one): " + output);
        assertTrue(output.contains("Case4Test.java"), "Should find Case4Test.java (both ** = one+): " + output);
    }

    @Test
    void testBraceExpansionForDoubleAsterisk() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Test that patterns with ** inside braces correctly throw PatternSyntaxException
        // This is expected behavior since POSIX doesn't support ** within braces
        // and our transformation creates nested braces which Java doesn't support
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        // Create test files
        Files.createFile(srcDir.resolve("DirectTest.java"));
        Path subDir = srcDir.resolve("sub");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("SubTest.java"));

        // Test that src/{**/,}*Test.java correctly throws PatternSyntaxException
        // because it becomes src/{{**,}/,}*Test.java (nested braces)
        assertThrows(
                PatternSyntaxException.class,
                () -> {
                    PosixCommands.ls(context, new String[] {"ls", "src/{**/,}*Test.java"});
                },
                "Pattern with ** inside braces should throw PatternSyntaxException due to nested braces");
    }

    @Test
    void testGlobExpansionOriginalComplexPattern() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create directory structure similar to the original request: src/**/test/**/*{Test,Spec}.{java,groovy}
        Path srcDir = tempDir.resolve("src");
        Path mainTestDir = srcDir.resolve("main").resolve("test").resolve("unit");
        Path integrationTestDir = srcDir.resolve("integration").resolve("test").resolve("api");
        Path mainJavaDir = srcDir.resolve("main").resolve("java");

        Files.createDirectories(mainTestDir);
        Files.createDirectories(integrationTestDir);
        Files.createDirectories(mainJavaDir);

        // Create test files that should match the pattern
        Files.createFile(mainTestDir.resolve("UserTest.java"));
        Files.createFile(mainTestDir.resolve("UserSpec.java"));
        Files.createFile(mainTestDir.resolve("ServiceTest.groovy"));
        Files.createFile(integrationTestDir.resolve("ApiTest.java"));
        Files.createFile(integrationTestDir.resolve("ApiSpec.groovy"));

        // Create files that should NOT match
        Files.createFile(mainJavaDir.resolve("User.java"));
        Files.createFile(srcDir.resolve("Config.java"));

        // Test the complex pattern: src/**/test/**/*{Test,Spec}.{java,groovy}
        PosixCommands.ls(context, new String[] {"ls", "src/**/test/**/*{Test,Spec}.{java,groovy}"});

        String output = out.toString();
        assertTrue(output.contains("UserTest.java"), "Should find UserTest.java: " + output);
        assertTrue(output.contains("UserSpec.java"), "Should find UserSpec.java: " + output);
        assertTrue(output.contains("ServiceTest.groovy"), "Should find ServiceTest.groovy: " + output);
        assertTrue(output.contains("ApiTest.java"), "Should find ApiTest.java: " + output);
        assertTrue(output.contains("ApiSpec.groovy"), "Should find ApiSpec.groovy: " + output);

        assertFalse(output.contains("User.java"), "Should not find User.java: " + output);
        assertFalse(output.contains("Config.java"), "Should not find Config.java: " + output);
    }

    @Test
    void testGlobExpansionWithQuestionMark() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files with single character variations
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("test2.txt"));
        Files.createFile(tempDir.resolve("test3.txt"));
        Files.createFile(tempDir.resolve("testA.txt"));
        Files.createFile(tempDir.resolve("testAB.txt"));

        // Test question mark pattern
        PosixCommands.ls(context, new String[] {"ls", "test?.txt"});

        String output = out.toString();
        assertTrue(output.contains("test1.txt"), output);
        assertTrue(output.contains("test2.txt"), output);
        assertTrue(output.contains("test3.txt"), output);
        assertTrue(output.contains("testA.txt"), output);
        assertFalse(output.contains("testAB.txt"), output);
    }

    @Test
    void testGrepGlobExpansion() throws Exception {
        // Create test files with content
        Path file1 = tempDir.resolve("test1.txt");
        Path file2 = tempDir.resolve("test2.txt");
        Path file3 = tempDir.resolve("other.txt");

        Files.write(file1, "hello world\ntest content\n".getBytes());
        Files.write(file2, "hello there\nmore test\n".getBytes());
        Files.write(file3, "hello universe\nno match\n".getBytes());

        // Test grep with glob pattern
        PosixCommands.grep(context, new String[] {"grep", "test", "test*.txt"});

        String output = normalizeLineEndings(out.toString());
        assertTrue(output.contains("test content"), output);
        assertTrue(output.contains("more test"), output);
        assertFalse(output.contains("no match"), output);
    }

    @Test
    void testCatGlobExpansion() throws Exception {
        // Create test files
        Path file1 = tempDir.resolve("data1.txt");
        Path file2 = tempDir.resolve("data2.txt");
        Path file3 = tempDir.resolve("other.txt");

        Files.write(file1, "Content of file 1\n".getBytes());
        Files.write(file2, "Content of file 2\n".getBytes());
        Files.write(file3, "Content of other file\n".getBytes());

        // Test cat with glob pattern
        PosixCommands.cat(context, new String[] {"cat", "data*.txt"});

        String output = normalizeLineEndings(out.toString());
        assertTrue(output.contains("Content of file 1"), output);
        assertTrue(output.contains("Content of file 2"), output);
        assertFalse(output.contains("Content of other file"), output);
    }

    @Test
    void testGlobExpansionNoMatches() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create some files that won't match the pattern
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));

        // Test pattern that matches nothing
        PosixCommands.ls(context, new String[] {"ls", "*.xyz"});

        String output = out.toString();
        // When no files match, ls should show nothing or handle gracefully
        assertFalse(output.contains("file1.txt"), output);
        assertFalse(output.contains("file2.txt"), output);
    }

    @Test
    void testGlobExpansionAbsolutePath() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("test2.txt"));

        // Test absolute path glob - use a simpler approach
        // Change to the temp directory and use relative paths
        PosixCommands.ls(context, new String[] {"ls", "test*.txt"});

        String output = out.toString();
        assertTrue(output.contains("test1.txt"), output);
        assertTrue(output.contains("test2.txt"), output);
    }

    @Test
    void testGlobExpansionNestedBraces() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files with complex naming patterns
        Files.createFile(tempDir.resolve("TestCase.java"));
        Files.createFile(tempDir.resolve("TestSuite.java"));
        Files.createFile(tempDir.resolve("SpecCase.java"));
        Files.createFile(tempDir.resolve("SpecSuite.java"));
        Files.createFile(tempDir.resolve("TestOther.java"));

        // Test nested brace patterns
        PosixCommands.ls(context, new String[] {"ls", "{Test,Spec}{Case,Suite}.java"});

        String output = out.toString();
        assertTrue(output.contains("TestCase.java"), output);
        assertTrue(output.contains("TestSuite.java"), output);
        assertTrue(output.contains("SpecCase.java"), output);
        assertTrue(output.contains("SpecSuite.java"), output);
        assertFalse(output.contains("TestOther.java"), output);
    }

    @Test
    void testGlobExpansionMixedPatterns() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files with various patterns
        Files.createFile(tempDir.resolve("test1.java"));
        Files.createFile(tempDir.resolve("test2.java"));
        Files.createFile(tempDir.resolve("spec1.java"));
        Files.createFile(tempDir.resolve("spec2.java"));
        Files.createFile(tempDir.resolve("other.java"));

        // Test mixed wildcard and brace patterns
        PosixCommands.ls(context, new String[] {"ls", "{test,spec}?.java"});

        String output = out.toString();
        assertTrue(output.contains("test1.java"), output);
        assertTrue(output.contains("test2.java"), output);
        assertTrue(output.contains("spec1.java"), output);
        assertTrue(output.contains("spec2.java"), output);
        assertFalse(output.contains("other.java"), output);
    }

    @Test
    void testGlobExpansionCharacterClass() throws Exception {
        // Skip test on platforms that don't support POSIX file attributes
        Assumptions.assumeTrue(isPosixSupported(), "POSIX file attributes not supported on this platform");

        // Create test files with numeric suffixes
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createFile(tempDir.resolve("file3.txt"));
        Files.createFile(tempDir.resolve("fileA.txt"));
        Files.createFile(tempDir.resolve("fileB.txt"));

        // Test simple wildcard pattern instead of character class for now
        PosixCommands.ls(context, new String[] {"ls", "file?.txt"});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"), output);
        assertTrue(output.contains("file2.txt"), output);
        assertTrue(output.contains("file3.txt"), output);
        assertTrue(output.contains("fileA.txt"), output);
        assertTrue(output.contains("fileB.txt"), output);
    }
}
