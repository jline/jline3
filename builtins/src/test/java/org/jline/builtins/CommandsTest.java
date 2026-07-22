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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandsTest {
    @Test
    void testHistoryForFileWithMoreHistoryRecordsThanAtHistoryFileSize() {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final File tmpHistoryFile =
                    Files.createTempFile("tmpHistory", "temp").toFile();
            tmpHistoryFile.deleteOnExit();
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpHistoryFile)))) {
                bw.write("1536743099591:SELECT \\n CURRENT_TIMESTAMP \\n as \\n c1;\n"
                        + "1536743104551:SELECT \\n 'asd' as \"sdf\", 4 \\n \\n as \\n c2;\\n\n"
                        + "1536743104551:SELECT \\n 'asd' \\n as \\n c2;\\n\n"
                        + "1536743104551:!/ 2\n"
                        + "1536743104551:SELECT \\n 2123 \\n as \\n c2 from dual;\\n\n"
                        + "1536743107526:!history\n"
                        + "1536743115431:SELECT \\n 2 \\n as \\n c2;\n"
                        + "1536743115431:SELECT \\n '213' \\n as \\n c1;\n"
                        + "1536743115431:!/ 8\n");
                bw.flush();
            }
            try (Terminal terminal =
                    TerminalBuilder.builder().streams(System.in, System.out).build()) {
                terminal.setSize(Size.of(50, 30));
                final History historyFromFile = new DefaultHistory();
                final LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .variable(LineReader.HISTORY_FILE, tmpHistoryFile.getAbsolutePath())
                        .history(historyFromFile);
                final LineReader lineReader = lineReaderBuilder.build();
                historyFromFile.attach(lineReader);
                final int maxLines = 3;
                lineReader.setVariable(LineReader.HISTORY_FILE_SIZE, maxLines);
                lineReader.getHistory().save();
                PrintStream out = new PrintStream(os, false);
                Commands.history(lineReader, out, out, Path.of(""), new String[] {"-d"});
                assertEquals(
                        maxLines + 1, os.toString(StandardCharsets.UTF_8).split("\\s+\\d{2}:\\d{2}:\\d{2}\\s+").length);
            }
        } catch (Exception e) {
            throw new RuntimeException("Test failed", e);
        }
    }

    @Test
    void highlighterViewRejectsThemePathTraversal(@TempDir Path tmp) throws Exception {
        Path configDir = Files.createDirectory(tmp.resolve("config"));
        Files.write(configDir.resolve("jnanorc"), List.of("theme dark.nanorctheme"));
        Files.write(configDir.resolve("dark.nanorctheme"), List.of("DEFAULT white"));
        // sits one level above the theme directory; the viewer must not reach it
        Files.write(tmp.resolve("outside.nanorctheme"), List.of("SECRET_TOKEN white"));

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream termBytes = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(new ByteArrayInputStream(new byte[0]), termBytes)
                .build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            ConfigurationPath configPath = new ConfigurationPath(configDir, configDir);
            Commands.highlighter(
                    reader,
                    terminal,
                    new PrintStream(outBytes),
                    new PrintStream(errBytes),
                    new String[] {"--view", "../outside.nanorctheme"},
                    configPath);
        }
        String out = outBytes.toString(StandardCharsets.UTF_8);
        String err = errBytes.toString(StandardCharsets.UTF_8);
        String term = termBytes.toString(StandardCharsets.UTF_8);
        assertTrue(err.contains("Invalid theme name"), "traversal should be rejected, err=" + err);
        assertFalse(out.contains("outside"), "escaped path must not be resolved, out=" + out);
        assertFalse(term.contains("SECRET_TOKEN"), "content outside the theme dir must not leak");
    }

    @Test
    void highlighterViewReadsThemeInConfigDir(@TempDir Path tmp) throws Exception {
        Path configDir = Files.createDirectory(tmp.resolve("config"));
        Files.write(configDir.resolve("jnanorc"), List.of("theme dark.nanorctheme"));
        Files.write(configDir.resolve("dark.nanorctheme"), List.of("DEFAULT white"));

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            ConfigurationPath configPath = new ConfigurationPath(configDir, configDir);
            Commands.highlighter(
                    reader,
                    terminal,
                    new PrintStream(outBytes),
                    new PrintStream(errBytes),
                    new String[] {"--view", "dark.nanorctheme"},
                    configPath);
        }
        String out = outBytes.toString(StandardCharsets.UTF_8);
        String err = errBytes.toString(StandardCharsets.UTF_8);
        assertFalse(err.contains("Invalid theme name"), "valid theme name must be accepted, err=" + err);
        assertTrue(out.contains("dark.nanorctheme"), "theme path should be printed, out=" + out);
    }
}
