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

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating basic history setup in JLine.
 */
public class HistorySetupExample {

    // SNIPPET_START: HistorySetupExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a history instance
        History history = new DefaultHistory();

        // Create a line reader with history
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .history(history)
                .variable(LineReader.HISTORY_FILE, Paths.get("history.txt"))
                .build();

        System.out.println("Type commands and use up/down arrows to navigate history");
        // Now the user can navigate history with up/down arrows
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: HistorySetupExample
}
