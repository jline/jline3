/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The POSIX listing/output builtins print filesystem-derived names (the file
 * name, a symlink target, the {@code ==> name <==} header, the grep filename
 * prefix). The name is chosen by whoever created the file, so an entry whose
 * name carries an OSC or CSI sequence must not reach the terminal verbatim.
 *
 * <p>The commands are driven with {@code --color=never} where they support it so
 * that legitimate SGR (which is itself ESC-based) stays out of the output; any
 * ESC/BEL that remains can only have come from an unescaped name.
 *
 * <p>Windows NTFS forbids control characters (0x00–0x1F) in file names,
 * so these tests are skipped on Windows — the attack surface they validate
 * only exists on Unix-like filesystems.
 */
@DisabledOnOs(OS.WINDOWS)
class PosixCommandsControlCharTest {

    @TempDir
    Path tempDir;

    private Terminal terminal;
    private ByteArrayOutputStream out;
    private PosixCommands.Context context;

    // OSC 0 (set window title) framed by ESC ] ... BEL, embedded in a file name.
    private static final String EVIL = "a\u001b]0;pwned\u0007b.txt";

    @BeforeEach
    void setUp() throws Exception {
        out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        Map<String, Object> vars = new HashMap<>();
        terminal = new DumbTerminal(in, out);
        context = new PosixCommands.Context(
                in, new PrintStream(out), new PrintStream(new ByteArrayOutputStream()), tempDir, terminal, vars::get);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (terminal != null) {
            terminal.close();
        }
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void assertNoControlBytes(String s) {
        assertFalse(s.indexOf('\u001b') >= 0, "ESC must not reach the terminal");
        assertFalse(s.indexOf('\u0007') >= 0, "BEL must not reach the terminal");
    }

    @Test
    void lsStripsControlCharsInFileName() throws Exception {
        Path evil = tempDir.resolve(EVIL);
        Files.createFile(evil);
        // -1 prints each name straight to the terminal (no column re-rendering).
        PosixCommands.ls(context, new String[] {"ls", "-1", "--color=never", evil.toString()});
        String o = output();
        assertNoControlBytes(o);
        assertTrue(o.contains("a]0;pwnedb.txt"), "the printable part of the name should still render: " + o);
    }

    @Test
    void lsLongStripsControlCharsInFileName() throws Exception {
        Path evil = tempDir.resolve(EVIL);
        Files.createFile(evil);
        PosixCommands.ls(context, new String[] {"ls", "-l", "--color=never", evil.toString()});
        assertNoControlBytes(output());
    }

    @Test
    void lsStripsControlCharsInSymlinkTarget() throws Exception {
        // A link whose target name carries the control sequence; ls prints "link -> <target>".
        Path target = tempDir.resolve(EVIL);
        Files.createFile(target);
        Path link = tempDir.resolve("link");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (UnsupportedOperationException e) {
            return; // symlinks unavailable on this platform/filesystem
        }
        PosixCommands.ls(context, new String[] {"ls", "-l", "--color=never", link.toString()});
        assertNoControlBytes(output());
    }

    @Test
    void headStripsControlCharsInFileHeader() throws Exception {
        Path evil = tempDir.resolve(EVIL);
        Files.write(evil, "hello\n".getBytes(StandardCharsets.UTF_8));
        Path plain = tempDir.resolve("plain.txt");
        Files.write(plain, "world\n".getBytes(StandardCharsets.UTF_8));
        // More than one source makes head emit the "==> name <==" headers.
        PosixCommands.head(context, new String[] {"head", evil.toString(), plain.toString()});
        assertNoControlBytes(output());
    }

    @Test
    void lsMultiDirStripsControlCharsInHeader() throws Exception {
        // When ls lists multiple directories it emits a "dirname:" header for each.
        // A directory whose name contains control chars must not leak them.
        String evilDir = "d\u001b]0;pwned\u0007ir";
        Path dir1 = tempDir.resolve(evilDir);
        Files.createDirectory(dir1);
        Files.createFile(dir1.resolve("file.txt"));
        Path dir2 = tempDir.resolve("safe");
        Files.createDirectory(dir2);
        Files.createFile(dir2.resolve("file.txt"));
        PosixCommands.ls(context, new String[] {"ls", "-1", "--color=never", dir1.toString(), dir2.toString()});
        assertNoControlBytes(output());
    }

    @Test
    void grepStripsControlCharsInFileHeader() throws Exception {
        Path evil = tempDir.resolve(EVIL);
        Files.write(evil, "match\n".getBytes(StandardCharsets.UTF_8));
        Path plain = tempDir.resolve("plain.txt");
        Files.write(plain, "match\n".getBytes(StandardCharsets.UTF_8));
        // More than one source makes grep prefix each match with the file name.
        PosixCommands.grep(context, new String[] {"grep", "--color=never", "match", evil.toString(), plain.toString()});
        assertNoControlBytes(output());
    }
}
