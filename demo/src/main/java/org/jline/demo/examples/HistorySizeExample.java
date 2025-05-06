/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.nio.file.Paths;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating history size configuration in JLine.
 */
public class HistorySizeExample {

    // SNIPPET_START: HistorySizeExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Configure history with size limits
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.HISTORY_FILE, Paths.get("~/.myapp_history"))
                .variable(LineReader.HISTORY_SIZE, 1000) // Maximum entries in memory
                .variable(LineReader.HISTORY_FILE_SIZE, 2000) // Maximum entries in file
                .build();

        System.out.println("History configured with size limits");
    }
    // SNIPPET_END: HistorySizeExample
}
