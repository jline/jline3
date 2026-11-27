/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pinning the observable behaviour of the {@code ls} command (output formats,
 * sorting, filtering, size and date formatting) and of the {@code --color} option
 * shared by {@code ls} and {@code grep}.
 */
class PosixCommandsLsTest {

    private static final String ESC = "\u001b[";

    @TempDir
    Path tempDir;

    private Terminal terminal;
    private PosixCommands.Context context;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() throws IOException {
        out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        Map<String, Object> vars = new HashMap<>();
        terminal = new DumbTerminal(in, out);
        context = new PosixCommands.Context(
                System.in, new PrintStream(out), new PrintStream(err), tempDir, terminal, vars::get);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (terminal != null) {
            terminal.close();
        }
    }

    private String output() {
        return out.toString().replace("\r\n", "\n");
    }

    private static boolean isPosix() {
        return !System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private void write(String name, int bytes) throws IOException {
        Files.write(tempDir.resolve(name), new byte[bytes]);
    }

    @Test
    void commaSeparatedFormat() throws Exception {
        write("a.txt", 1);
        write("b.txt", 1);
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-m", "a.txt", "b.txt"});
        assertEquals("a.txt, b.txt\n", output());
    }

    @Test
    void oneEntryPerLineFormat() throws Exception {
        write("a.txt", 1);
        write("b.txt", 1);
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1", "a.txt", "b.txt"});
        assertEquals("a.txt\nb.txt\n", output());
    }

    @Test
    void formatFlagPrecedence() throws Exception {
        write("a.txt", 1);
        write("b.txt", 1);
        // -m wins over -l and -C
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-C", "-l", "-m", "a.txt", "b.txt"});
        assertEquals("a.txt, b.txt\n", output());
        // -1 wins over everything else
        out.reset();
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-m", "-1", "a.txt", "b.txt"});
        assertEquals("a.txt\nb.txt\n", output());
    }

    @Test
    void defaultsToOneEntryPerLineWhenNotATerminal() throws Exception {
        write("a.txt", 1);
        write("b.txt", 1);
        PrintStream err = new PrintStream(new ByteArrayOutputStream());
        PosixCommands.Context notATty =
                new PosixCommands.Context(System.in, new PrintStream(out), err, tempDir, null, n -> null);
        PosixCommands.ls(notATty, new String[] {"ls", "a.txt", "b.txt"});
        assertEquals("a.txt\nb.txt\n", output());
    }

    @Test
    void hiddenFilesAreOnlyListedWithAllFlag() throws Exception {
        write(".hidden", 1);
        write("visible", 1);

        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1"});
        List<String> withoutAll = Arrays.asList(output().split("\n"));
        assertTrue(withoutAll.contains("visible"), withoutAll.toString());
        assertTrue(withoutAll.contains("."), withoutAll.toString());
        assertTrue(withoutAll.contains(".."), withoutAll.toString());
        assertFalse(withoutAll.contains(".hidden"), withoutAll.toString());

        out.reset();
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1", "-a"});
        assertTrue(Arrays.asList(output().split("\n")).contains(".hidden"), output());
    }

    @Test
    void sortBySizeAndReverse() throws Exception {
        write("small", 10);
        write("big", 5000);
        write("medium", 300);

        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1", "-S", "small", "big", "medium"});
        assertEquals("big\nmedium\nsmall\n", output());

        out.reset();
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1", "-S", "-r", "small", "big", "medium"});
        assertEquals("small\nmedium\nbig\n", output());
    }

    @Test
    void sortByModificationTime() throws Exception {
        write("old", 1);
        write("new", 1);
        Instant now = Instant.now();
        Files.setLastModifiedTime(tempDir.resolve("old"), FileTime.from(now.minus(2, ChronoUnit.DAYS)));
        Files.setLastModifiedTime(tempDir.resolve("new"), FileTime.from(now));

        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1", "-t", "old", "new"});
        assertEquals("new\nold\n", output());
    }

    @Test
    void classifyAppendsTypeIndicators() throws Exception {
        Files.createDirectory(tempDir.resolve("dir"));
        write("plain", 1);

        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1", "-F"});

        List<String> lines = Arrays.asList(output().split("\n"));
        assertTrue(lines.contains("dir/"), lines.toString());

        String plain = Files.isExecutable(tempDir.resolve("plain")) ? "plain*" : "plain";
        assertTrue(lines.contains(plain), lines.toString());
    }

    @Test
    void humanReadableSizesInLongListing() throws Exception {
        Assumptions.assumeTrue(isPosix(), "POSIX file attributes not supported on this platform");
        write("tiny", 500);
        write("kilo", 2048);

        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-l", "-h", "tiny", "kilo"});
        String[] lines = output().split("\n");
        // formatSize() formats with the default locale, so 2.0K is "2,0K" under e.g. a German locale
        String twoKilobytes = String.format("%.1f", 2.0) + "K";
        assertTrue(lines[0].contains(" " + twoKilobytes + " ") && lines[0].endsWith(" kilo"), lines[0]);
        assertTrue(lines[1].contains(" 500B ") && lines[1].endsWith(" tiny"), lines[1]);
    }

    @Test
    void longListingShowsTimeForRecentFilesAndYearForOldFiles() throws Exception {
        Assumptions.assumeTrue(isPosix(), "POSIX file attributes not supported on this platform");
        write("recent", 1);
        write("ancient", 1);
        Files.setLastModifiedTime(tempDir.resolve("ancient"), FileTime.from(Instant.parse("2001-03-04T05:06:07Z")));

        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-l", "recent", "ancient"});
        String[] lines = output().split("\n");
        assertTrue(lines[0].matches(".*\\s2001\\sancient$"), lines[0]);
        assertTrue(lines[1].matches(".*\\d\\d:\\d\\d\\srecent$"), lines[1]);
        assertTrue(lines[0].startsWith("-"), lines[0]);
    }

    @Test
    void lsColorAlwaysStylesDirectories() throws Exception {
        Files.createDirectory(tempDir.resolve("dir"));
        PosixCommands.ls(context, new String[] {"ls", "--color=always", "-1"});
        assertTrue(output().contains(ESC), output());
    }

    @Test
    void lsColorNeverDoesNotEmitEscapes() throws Exception {
        Files.createDirectory(tempDir.resolve("dir"));
        PosixCommands.ls(context, new String[] {"ls", "--color=never", "-1"});
        assertFalse(output().contains(ESC), output());
    }

    @Test
    void lsRejectsInvalidColorArgument() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> PosixCommands.ls(context, new String[] {"ls", "--color=sometimes"}));
        assertEquals("invalid argument 'sometimes' for '--color'", e.getMessage());
    }

    @Test
    void grepAcceptsAllColorModes() throws Exception {
        Files.write(tempDir.resolve("f.txt"), "hello world\n".getBytes());
        for (String mode : new String[] {"always", "yes", "force", "auto", "tty", "if-tty", "no", "none"}) {
            out.reset();
            PosixCommands.grep(context, new String[] {"grep", "--color=" + mode, "world", "f.txt"});
            assertTrue(output().contains("world"), mode + ": " + output());
        }

        out.reset();
        PosixCommands.grep(context, new String[] {"grep", "--color=never", "world", "f.txt"});
        assertEquals("hello world\n", output());
    }

    @Test
    void grepRejectsInvalidColorArgument() throws Exception {
        Files.write(tempDir.resolve("f.txt"), "hello\n".getBytes());
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> PosixCommands.grep(context, new String[] {"grep", "--color=sometimes", "hello", "f.txt"}));
        assertEquals("invalid argument 'sometimes' for '--color'", e.getMessage());
    }
}
