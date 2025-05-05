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
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating persistent history in JLine.
 */
public class PersistentHistoryExample {

    // SNIPPET_START: PersistentHistoryExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Set the history file
        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("~/.myapp_history"));

        // Use the reader...
        String line = reader.readLine("prompt> ");

        // Save history explicitly (though it's usually done automatically)
        ((DefaultHistory) reader.getHistory()).save();

        System.out.println("History saved to ~/.myapp_history");
    }
    // SNIPPET_END: PersistentHistoryExample
}
