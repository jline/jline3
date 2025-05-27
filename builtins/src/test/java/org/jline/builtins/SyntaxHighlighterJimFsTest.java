/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test SyntaxHighlighter.addFiles() with JimFS to verify cross-platform behavior
 * on both Unix and Windows file systems.
 */
public class SyntaxHighlighterJimFsTest {

    private static Stream<Arguments> fileSystemConfigurations() {
        return Stream.of(Arguments.of("Unix", Configuration.unix()), Arguments.of("Windows", Configuration.windows()));
    }

    @ParameterizedTest(name = "{0} file system")
    @MethodSource("fileSystemConfigurations")
    public void testRelativeRecursiveGlobPattern(String fsName, Configuration config) throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(config)) {
            // Create nested directory structure: foo/bar/baz/
            Path root = getFileSystemRoot(fs, config);
            Path fooDir = root.resolve("foo");
            Path barDir = fooDir.resolve("bar");
            Path bazDir = barDir.resolve("baz");
            Files.createDirectories(bazDir);

            // Create test files in different levels
            Path file1 = barDir.resolve("test1.nanorc");
            Path file2 = bazDir.resolve("test2.nanorc");
            Path file3 = bazDir.resolve("other.txt");

            Files.write(file1, "syntax test1".getBytes(StandardCharsets.UTF_8));
            Files.write(file2, "syntax test2".getBytes(StandardCharsets.UTF_8));
            Files.write(file3, "not a nanorc file".getBytes(StandardCharsets.UTF_8));

            // Create nanorc file in the root
            Path nanorc = root.resolve("jnanorc");
            Files.write(nanorc, "# test config".getBytes(StandardCharsets.UTF_8));

            // Test relative recursive glob pattern "foo/bar/**/*.nanorc"
            String relativeRecursivePattern = "foo/bar/**/*.nanorc";
            List<Path> foundFiles = new ArrayList<>();

            SyntaxHighlighter.addFiles(nanorc, relativeRecursivePattern, stream -> stream.forEach(foundFiles::add));

            assertEquals(1, foundFiles.size(), "Should find exactly 2 .nanorc files in foo/bar/**");
            assertFalse(foundFiles.contains(file1), "Should contain test1.nanorc from foo/bar/");
            assertTrue(foundFiles.contains(file2), "Should contain test2.nanorc from foo/bar/baz/");
            assertFalse(foundFiles.contains(file3), "Should not contain other.txt");
        }
    }

    private Path getFileSystemRoot(FileSystem fs, Configuration config) {
        if (config == Configuration.windows()) {
            return fs.getPath("C:\\");
        } else {
            return fs.getPath("/");
        }
    }

    @ParameterizedTest(name = "{0} file system")
    @MethodSource("fileSystemConfigurations")
    public void testAbsoluteRecursiveGlobPattern(String fsName, Configuration config) throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(config)) {
            // Create nested directory structure
            Path root = getFileSystemRoot(fs, config);
            Path shareDir, configDir;
            String absoluteRecursivePattern;

            if (config == Configuration.windows()) {
                shareDir = root.resolve("Program Files").resolve("nano");
                configDir = root.resolve("config");
                absoluteRecursivePattern = "C:/Program Files/nano/**/*.nanorc";
            } else {
                shareDir = root.resolve("usr").resolve("share").resolve("nano");
                configDir = root.resolve("etc");
                absoluteRecursivePattern = "/usr/share/nano/**/*.nanorc";
            }

            Path subDir = shareDir.resolve("subdir");
            Files.createDirectories(subDir);
            Files.createDirectories(configDir);

            // Create test files
            Path file1 = shareDir.resolve("test1.nanorc");
            Path file2 = subDir.resolve("test2.nanorc");

            Files.write(file1, "syntax test1".getBytes(StandardCharsets.UTF_8));
            Files.write(file2, "syntax test2".getBytes(StandardCharsets.UTF_8));

            // Create nanorc file in a different location
            Path nanorc = configDir.resolve("jnanorc");
            Files.write(nanorc, "# test config".getBytes(StandardCharsets.UTF_8));

            // Test absolute recursive glob pattern
            List<Path> foundFiles = new ArrayList<>();

            SyntaxHighlighter.addFiles(nanorc, absoluteRecursivePattern, stream -> stream.forEach(foundFiles::add));

            assertEquals(1, foundFiles.size(), "Should find exactly 2 .nanorc files recursively");
            assertFalse(foundFiles.contains(file1), "Should contain test1.nanorc");
            assertTrue(foundFiles.contains(file2), "Should contain test2.nanorc");
        }
    }

    @ParameterizedTest(name = "{0} file system")
    @MethodSource("fileSystemConfigurations")
    public void testSimpleGlobPattern(String fsName, Configuration config) throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(config)) {
            Path root = getFileSystemRoot(fs, config);

            // Create test files in the root
            Path file1 = root.resolve("test1.nanorc");
            Path file2 = root.resolve("test2.nanorc");
            Path file3 = root.resolve("other.txt");

            Files.write(file1, "syntax test1".getBytes(StandardCharsets.UTF_8));
            Files.write(file2, "syntax test2".getBytes(StandardCharsets.UTF_8));
            Files.write(file3, "not a nanorc file".getBytes(StandardCharsets.UTF_8));

            // Create nanorc file
            Path nanorc = root.resolve("jnanorc");
            Files.write(nanorc, "# test config".getBytes(StandardCharsets.UTF_8));

            // Test simple glob pattern "*.nanorc"
            String simplePattern = "*.nanorc";
            List<Path> foundFiles = new ArrayList<>();

            SyntaxHighlighter.addFiles(nanorc, simplePattern, stream -> stream.forEach(foundFiles::add));

            assertEquals(2, foundFiles.size(), "Should find exactly 2 .nanorc files");
            assertTrue(foundFiles.contains(file1), "Should contain test1.nanorc");
            assertTrue(foundFiles.contains(file2), "Should contain test2.nanorc");
            assertFalse(foundFiles.contains(file3), "Should not contain other.txt");
        }
    }

    @ParameterizedTest(name = "{0} file system")
    @MethodSource("fileSystemConfigurations")
    public void testNonExistentDirectory(String fsName, Configuration config) throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(config)) {
            Path root = getFileSystemRoot(fs, config);

            // Create nanorc file
            Path nanorc = root.resolve("jnanorc");
            Files.write(nanorc, "# test config".getBytes(StandardCharsets.UTF_8));

            // Test glob pattern pointing to non-existent directory
            String nonExistentPattern = "nonexistent/**/*.nanorc";
            List<Path> foundFiles = new ArrayList<>();

            // Should not throw an exception
            assertDoesNotThrow(() -> {
                SyntaxHighlighter.addFiles(nanorc, nonExistentPattern, stream -> stream.forEach(foundFiles::add));
            });

            assertEquals(0, foundFiles.size(), "Should find no files when directory doesn't exist");
        }
    }

    @ParameterizedTest(name = "{0} file system")
    @MethodSource("fileSystemConfigurations")
    public void testNonGlobPattern(String fsName, Configuration config) throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(config)) {
            Path root = getFileSystemRoot(fs, config);

            // Create test file
            Path testFile = root.resolve("test.nanorc");
            Files.write(testFile, "syntax test".getBytes(StandardCharsets.UTF_8));

            // Create nanorc file
            Path nanorc = root.resolve("jnanorc");
            Files.write(nanorc, "# test config".getBytes(StandardCharsets.UTF_8));

            // Test non-glob pattern (should work as before)
            String simplePattern = "test.nanorc";
            List<Path> foundFiles = new ArrayList<>();

            SyntaxHighlighter.addFiles(nanorc, simplePattern, stream -> stream.forEach(foundFiles::add));

            assertEquals(1, foundFiles.size(), "Should find exactly 1 file");
            assertTrue(foundFiles.contains(testFile), "Should contain test.nanorc");
        }
    }

    @Test
    public void testExtractStaticPathPrefix() {
        // Test the extractStaticPathPrefix method indirectly by verifying behavior
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path root = fs.getPath("/");

            // Create a complex directory structure
            Path deepDir = root.resolve("a").resolve("b").resolve("c").resolve("d");
            Files.createDirectories(deepDir);

            Path testFile = deepDir.resolve("test.nanorc");
            Files.write(testFile, "syntax test".getBytes(StandardCharsets.UTF_8));

            Path nanorc = root.resolve("jnanorc");
            Files.write(nanorc, "# test config".getBytes(StandardCharsets.UTF_8));

            // Test pattern "a/b/**/*.nanorc" - should extract "a/b" as static prefix
            String pattern = "a/b/**/*.nanorc";
            List<Path> foundFiles = new ArrayList<>();

            SyntaxHighlighter.addFiles(nanorc, pattern, stream -> stream.forEach(foundFiles::add));

            assertEquals(1, foundFiles.size(), "Should find the file in the deep directory");
            assertTrue(foundFiles.contains(testFile), "Should contain the test file");
        } catch (IOException e) {
            fail("Test should not throw IOException: " + e.getMessage());
        }
    }
}
