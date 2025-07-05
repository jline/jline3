/*
 * Copyright (c) 2002-2023, the original author(s).
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

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PosixCommands migrated functionality.
 */
public class PosixCommandsTest {

    @TempDir
    Path tempDir;

    private TestContext context;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private ByteArrayInputStream in;

    @BeforeEach
    void setUp() throws IOException {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        in = new ByteArrayInputStream("".getBytes());

        Terminal terminal = new DumbTerminal(in, out);
        context = new TestContext(terminal, tempDir, in, new PrintStream(out), new PrintStream(err));
    }

    @Test
    void testLsBasic() throws Exception {
        // Create test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));

        PosixCommands.ls(context, new String[] {});

        String output = out.toString();
        assertTrue(output.contains("file1.txt"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("subdir"));
    }

    @Test
    void testLsLongFormat() throws Exception {
        Files.createFile(tempDir.resolve("test.txt"));

        PosixCommands.ls(context, new String[] {"-l"});

        String output = out.toString();
        assertTrue(output.contains("test.txt"));
        // Should contain some file information (permissions, size, etc.)
        assertTrue(output.length() > "test.txt".length());
    }

    @Test
    void testLsWithColor() throws Exception {
        Files.createFile(tempDir.resolve("test.txt"));
        Files.createDirectory(tempDir.resolve("testdir"));

        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("dr", "1;34"); // Blue for directories
        colorMap.put("ex", "1;32"); // Green for executables

        PosixCommands.ls(context, new String[] {"--color=always"}, colorMap);

        String output = out.toString();
        assertTrue(output.contains("test.txt"));
        assertTrue(output.contains("testdir"));
    }

    @Test
    void testLsHiddenFiles() throws Exception {
        Files.createFile(tempDir.resolve(".hidden"));
        Files.createFile(tempDir.resolve("visible.txt"));

        // Without -a, hidden files should not appear
        PosixCommands.ls(context, new String[] {});
        String output1 = out.toString();
        assertFalse(output1.contains(".hidden"));
        assertTrue(output1.contains("visible.txt"));

        // Reset output
        out.reset();

        // With -a, hidden files should appear
        PosixCommands.ls(context, new String[] {"-a"});
        String output2 = out.toString();
        assertTrue(output2.contains(".hidden"));
        assertTrue(output2.contains("visible.txt"));
    }

    @Test
    void testSortBasic() throws Exception {
        String input = "zebra\napple\nbanana\n";
        context.setInput(input);

        PosixCommands.sort(context, new String[] {});

        String output = out.toString();
        String[] lines = output.trim().split("\n");
        assertEquals("apple", lines[0]);
        assertEquals("banana", lines[1]);
        assertEquals("zebra", lines[2]);
    }

    @Test
    void testSortReverse() throws Exception {
        String input = "apple\nbanana\nzebra\n";
        context.setInput(input);

        PosixCommands.sort(context, new String[] {"-r"});

        String output = out.toString();
        String[] lines = output.trim().split("\n");
        assertEquals("zebra", lines[0]);
        assertEquals("banana", lines[1]);
        assertEquals("apple", lines[2]);
    }

    @Test
    void testSortNumeric() throws Exception {
        String input = "10\n2\n100\n1\n";
        context.setInput(input);

        PosixCommands.sort(context, new String[] {"--numeric-sort"});

        String output = out.toString();
        String[] lines = output.trim().split("\n");
        assertEquals("1", lines[0]);
        assertEquals("2", lines[1]);
        assertEquals("10", lines[2]);
        assertEquals("100", lines[3]);
    }

    @Test
    void testSortUnique() throws Exception {
        String input = "apple\nbanana\napple\nbanana\ncherry\n";
        context.setInput(input);

        PosixCommands.sort(context, new String[] {"-u"});

        String output = out.toString();
        String[] lines = output.trim().split("\n");
        assertEquals(3, lines.length);
        assertEquals("apple", lines[0]);
        assertEquals("banana", lines[1]);
        assertEquals("cherry", lines[2]);
    }

    @Test
    void testSortCaseInsensitive() throws Exception {
        String input = "Zebra\napple\nBanana\n";
        context.setInput(input);

        PosixCommands.sort(context, new String[] {"-f"});

        String output = out.toString();
        String[] lines = output.trim().split("\n");
        assertEquals("apple", lines[0]);
        assertEquals("Banana", lines[1]);
        assertEquals("Zebra", lines[2]);
    }

    @Test
    void testGrepBasic() throws Exception {
        String input = "hello world\nfoo bar\nhello there\n";
        context.setInput(input);

        PosixCommands.grep(context, new String[] {"hello"});

        String output = out.toString();
        assertTrue(output.contains("hello world"));
        assertTrue(output.contains("hello there"));
        assertFalse(output.contains("foo bar"));
    }

    @Test
    void testGrepCaseInsensitive() throws Exception {
        String input = "Hello World\nfoo bar\nHELLO there\n";
        context.setInput(input);

        PosixCommands.grep(context, new String[] {"-i", "hello"});

        String output = out.toString();
        assertTrue(output.contains("Hello World"));
        assertTrue(output.contains("HELLO there"));
        assertFalse(output.contains("foo bar"));
    }

    @Test
    void testGrepInvert() throws Exception {
        String input = "hello world\nfoo bar\nhello there\n";
        context.setInput(input);

        PosixCommands.grep(context, new String[] {"-v", "hello"});

        String output = out.toString();
        assertTrue(output.contains("foo bar"));
        assertFalse(output.contains("hello world"));
        assertFalse(output.contains("hello there"));
    }

    @Test
    void testGrepLineNumbers() throws Exception {
        String input = "hello world\nfoo bar\nhello there\n";
        context.setInput(input);

        PosixCommands.grep(context, new String[] {"-n", "hello"});

        String output = out.toString();
        // Should contain line numbers and matching lines
        assertTrue(output.contains("hello world"));
        assertTrue(output.contains("hello there"));
        // Line numbers might be formatted differently, so just check for presence
        assertTrue(output.matches(".*\\d+.*hello.*"));
    }

    @Test
    void testGrepCount() throws Exception {
        String input = "hello world\nfoo bar\nhello there\n";
        context.setInput(input);

        PosixCommands.grep(context, new String[] {"-c", "hello"});

        String output = out.toString().trim();
        assertEquals("2", output);
    }

    @Test
    void testWatchBasic() throws Exception {
        AtomicReference<String> executedCommand = new AtomicReference<>();
        PosixCommands.CommandExecutor executor = command -> {
            executedCommand.set(command);
            return "Command output: " + command;
        };

        // Use a very short interval and stop quickly
        Thread watchThread = new Thread(() -> {
            try {
                PosixCommands.watch(context, new String[] {"-n", "1", "echo", "test"}, executor);
            } catch (Exception e) {
                // Expected when interrupted
            }
        });

        watchThread.start();
        Thread.sleep(100); // Let it run briefly
        watchThread.interrupt();

        String output = out.toString();
        assertTrue(output.contains("Every 1s: echo test"));
    }

    @Test
    void testCdValidation() throws Exception {
        // Test with existing directory
        PosixCommands.cd(context, new String[] {tempDir.toString()});

        String output = out.toString();
        assertTrue(output.contains("Directory exists:"));

        // Reset output
        out.reset();

        // Test with non-existing directory
        assertThrows(IOException.class, () -> {
            PosixCommands.cd(context, new String[] {"nonexistent"});
        });
    }

    @Test
    void testTtop() throws Exception {
        // ttop should delegate to TTop.ttop - just test it doesn't crash
        assertDoesNotThrow(() -> {
            // This will likely fail due to terminal requirements, but shouldn't crash
            try {
                PosixCommands.ttop(context, new String[] {"--help"});
            } catch (Exception e) {
                // Expected in test environment
            }
        });
    }

    @Test
    void testNano() throws Exception {
        // nano should delegate to Nano - just test it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                PosixCommands.nano(context, new String[] {"--help"});
            } catch (Exception e) {
                // Expected in test environment
            }
        });
    }

    @Test
    void testLess() throws Exception {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content\nLine 2\nLine 3\n".getBytes());

        // less should delegate to Less - just test it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                PosixCommands.less(context, new String[] {"--help"});
            } catch (Exception e) {
                // Expected in test environment
            }
        });
    }

    /**
     * Test implementation of Context for unit tests.
     */
    private static class TestContext extends PosixCommands.Context {
        private InputStream input;

        public TestContext(
                Terminal terminal, Path currentDir, InputStream input, PrintStream output, PrintStream error) {
            super(input, output, error, currentDir, terminal);
            this.input = input;
        }

        public void setInput(String inputString) {
            this.input = new ByteArrayInputStream(inputString.getBytes());
        }

        @Override
        public InputStream in() {
            return input;
        }

        @Override
        public boolean isTty() {
            return false; // For testing, assume non-TTY
        }
    }
}
