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
 * Examples demonstrating the LineReader functionality.
 */
public class LineReaderExample {
    public static void main(String[] args) throws IOException {
        // SNIPPET_START: basic-line-reader
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().build();

        // HIGHLIGHT_START: Create a basic line reader
        // Create a basic line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        // HIGHLIGHT_END

        // Create a line reader with custom configuration
        LineReader customReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .appName("MyApp")
                .variable(LineReader.HISTORY_FILE, Paths.get("history.txt"))
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.HISTORY_BEEP, false)
                .build();
        // SNIPPET_END: basic-line-reader

        // SNIPPET_START: read-line
        // Read a line of input
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);

        // Read a password (input will be masked)
        String password = reader.readLine("Password: ", '*');
        System.out.println("Password length: " + password.length());
        // SNIPPET_END: read-line
    }
}
