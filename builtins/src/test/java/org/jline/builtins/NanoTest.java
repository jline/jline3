/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NanoTest {

    @Test
    @Timeout(1)
    void nanoBufferLineOverflow() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8)) {
            terminal.setSize(Size.of(80, 25));
            for (int i = 0; i < 100; i++) {
                terminal.processInputByte(' ');
            }
            terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
            terminal.processInputByte('n');
            String[] argv = {"--ignorercfiles"};
            Nano nano = new Nano(
                    terminal,
                    Path.of("target/test.txt"),
                    Options.compile(Nano.usage()).parse(argv));
            assertNotNull(nano);
            nano.run();
        }
    }

    @Test
    @Timeout(10)
    void restrictedModeDoesNotReadArbitraryFiles() throws Exception {
        Path root = Files.createTempDirectory("nano-restricted-root");
        Path secret = Files.createTempFile("nano-restricted-secret", ".txt");
        String marker = "SECRET_OUTSIDE_ROOT_a1b2c3d4";
        Files.write(secret, marker.getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (LineDisciplineTerminal terminal =
                    new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8)) {
                terminal.setSize(Size.of(80, 25));
                // Deliver Enter as a raw CR (nano runs in raw mode; avoid CR->NL translation)
                Attributes attrs = terminal.getAttributes();
                attrs.setInputFlag(InputFlag.ICRNL, false);
                terminal.setAttributes(attrs);
                // ^R (Read File), type an absolute path escaping the root, accept
                terminal.processInputByte(KeyMap.ctrl('R').getBytes()[0]);
                for (byte b : secret.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8)) {
                    terminal.processInputByte(b);
                }
                terminal.processInputByte('\r');
                // Quit both the (possibly opened) buffer and the original one, discarding changes
                terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
                terminal.processInputByte('n');
                terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
                terminal.processInputByte('n');
                String[] argv = {"--ignorercfiles", "--restricted"};
                Nano nano =
                        new Nano(terminal, root, Options.compile(Nano.usage()).parse(argv));
                nano.run();
            }
            assertFalse(
                    output.toString(StandardCharsets.UTF_8).contains(marker),
                    "restricted mode must not read files outside the specified one");
        } finally {
            Files.deleteIfExists(secret);
            Files.deleteIfExists(root);
        }
    }

    @Test
    @Timeout(10)
    void writeOutAsksBeforeOverwritingExistingFileUnderRoot() throws Exception {
        Path root = Files.createTempDirectory("nano-overwrite-root");
        // Exists under the editor root only, not in the JVM working directory
        String name = "nano-overwrite-target-7f3a.txt";
        Path target = root.resolve(name);
        Files.write(target, "original".getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (LineDisciplineTerminal terminal =
                    new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8)) {
                terminal.setSize(Size.of(80, 25));
                Attributes attrs = terminal.getAttributes();
                attrs.setInputFlag(InputFlag.ICRNL, false);
                terminal.setAttributes(attrs);
                // Type new text, ^O (Write Out), the existing name, accept
                for (byte b : "replacement".getBytes(StandardCharsets.UTF_8)) {
                    terminal.processInputByte(b);
                }
                terminal.processInputByte(KeyMap.ctrl('O').getBytes()[0]);
                for (byte b : name.getBytes(StandardCharsets.UTF_8)) {
                    terminal.processInputByte(b);
                }
                terminal.processInputByte('\r');
                // Decline the overwrite prompt, then quit discarding the buffer
                terminal.processInputByte('n');
                terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
                terminal.processInputByte('n');
                String[] argv = {"--ignorercfiles"};
                Nano nano =
                        new Nano(terminal, root, Options.compile(Nano.usage()).parse(argv));
                nano.run();
            }
            assertEquals(
                    "original",
                    new String(Files.readAllBytes(target), StandardCharsets.UTF_8),
                    "declining the overwrite prompt must leave the file untouched");
            assertTrue(
                    output.toString(StandardCharsets.UTF_8).contains("OVERWRITE"),
                    "write out to an existing file under the root must ask before overwriting");
        } finally {
            Files.deleteIfExists(target);
            Files.deleteIfExists(root);
        }
    }

    // Unix-style Jimfs so file names may carry ESC/BEL on every OS (Windows rejects them)
    private static final String EVIL_NAME = "evil\u001b]2;name\u0007.txt";

    @Test
    @Timeout(10)
    void controlCharactersInFileAreNotSentToTerminal() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path root = Files.createDirectories(fs.getPath("/work"));
            Files.write(
                    root.resolve(EVIL_NAME),
                    "a\u001b]0;pwned\u0007b\u001b[3Jc\u009b2Jd\u007fe\n".getBytes(StandardCharsets.UTF_8));
            String out = runNano(root, KeyMap.ctrl('X'), EVIL_NAME);
            assertFalse(out.contains("\u001b]"), "file content or name must not emit an OSC sequence");
            assertFalse(out.contains("\u0007"), "file content or name must not emit BEL");
            assertFalse(out.contains("\u001b[3J"), "file content must not emit a CSI sequence");
            assertFalse(out.contains("\u009b"), "file content must not emit an 8-bit CSI");
            assertTrue(
                    out.contains("a\u241b]0;pwned\u2407b\u241b[3Jc\ufffd2Jd\u2421e"),
                    "control characters must be shown as visible placeholders");
            assertTrue(out.contains("evil\u241b]2;name\u2407.txt"), "file name must be shown with placeholders");
        }
    }

    @Test
    @Timeout(10)
    void controlCharactersInStatusMessageAreNotSentToTerminal() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path root = Files.createDirectories(fs.getPath("/work"));
            Files.write(root.resolve("first.txt"), "x\n".getBytes(StandardCharsets.UTF_8));
            Files.write(root.resolve(EVIL_NAME), "y\n".getBytes(StandardCharsets.UTF_8));
            // M-. (next buffer) shows "Switched to File: <name>" in the status line
            String out = runNano(root, KeyMap.alt('.') + KeyMap.ctrl('X') + KeyMap.ctrl('X'), "first.txt", EVIL_NAME);
            assertFalse(out.contains("\u001b]"), "status message must not emit an OSC sequence");
            assertTrue(
                    out.contains("[ Switched to File: evil\u241b]2;name\u2407.txt ]"),
                    "status message must be shown with placeholders");
        }
    }

    @Test
    @Timeout(10)
    void controlCharactersInWriteOutPromptAreNotSentToTerminal() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path root = Files.createDirectories(fs.getPath("/work"));
            Files.write(root.resolve(EVIL_NAME), "y\n".getBytes(StandardCharsets.UTF_8));
            // ^O pre-fills the write-out prompt with the file name, ^C cancels it, ^X quits
            String out = runNano(root, KeyMap.ctrl('O') + KeyMap.ctrl('C') + KeyMap.ctrl('X'), EVIL_NAME);
            assertFalse(out.contains("\u001b]"), "write-out prompt must not emit an OSC sequence");
            assertFalse(out.contains("\u0007"), "write-out prompt must not emit BEL");
            assertTrue(
                    out.contains("File Name to Write: evil\u241b]2;name\u2407.txt"),
                    "write-out prompt must show the file name with placeholders");
        }
    }

    @Test
    @Timeout(10)
    void endOfLineCountsControlCharacterPlaceholders() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path root = Files.createDirectories(fs.getPath("/work"));
            Path file = Files.write(root.resolve("nul.txt"), "a\u0000b\n".getBytes(StandardCharsets.UTF_8));
            // ^E (end of line), type X, ^O + Enter to save under the same name, ^X quits
            runNano(root, KeyMap.ctrl('E') + "X" + KeyMap.ctrl('O') + "\r" + KeyMap.ctrl('X'), "nul.txt");
            assertEquals(
                    "a\u0000bX\n",
                    new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
                    "end of line must land after the last character of a line holding a control character");
        }
    }

    private static String runNano(Path root, String input, String... files) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8)) {
            terminal.setSize(Size.of(80, 25));
            // Input is queued before run(), so apply the raw-mode flags Nano would set: keep CR and ^C as keys
            Attributes attrs = terminal.getAttributes();
            attrs.setInputFlag(InputFlag.ICRNL, false);
            attrs.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attrs);
            for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
                terminal.processInputByte(b);
            }
            String[] argv = {"--ignorercfiles"};
            Nano nano = new Nano(terminal, root, Options.compile(Nano.usage()).parse(argv));
            nano.open(files);
            nano.run();
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
