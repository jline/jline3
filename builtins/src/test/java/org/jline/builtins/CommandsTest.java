/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommandsTest {
    @Test
    public void testHistoryForFileWithMoreHistoryRecordsThanAtHistoryFileSize() {
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
            Terminal terminal =
                    TerminalBuilder.builder().streams(System.in, System.out).build();
            terminal.setSize(new Size(50, 30));
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
            Commands.history(lineReader, out, out, Paths.get(""), new String[] {"-d"});
            assertEquals(maxLines + 1, os.toString("UTF8").split("\\s+\\d{2}:\\d{2}:\\d{2}\\s+").length);
        } catch (Exception e) {
            throw new RuntimeException("Test failed", e);
        }
    }
}
