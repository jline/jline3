/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * This example demonstrates the basic architecture components of JLine.
 * It contains code snippets used in the architecture documentation.
 */
public class ArchitectureExample {

    public static void main(String[] args) throws Exception {
        // SNIPPET_START: BasicTerminalAndLineReader
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Read input
        String line = reader.readLine("prompt> ");
        // SNIPPET_END: BasicTerminalAndLineReader

        System.out.println("You entered: " + line);

        // SNIPPET_START: AddingTabCompletion
        // Create a completer
        Completer completer = new StringsCompleter("command1", "command2", "help", "quit");

        // Create a line reader with completion
        LineReader readerWithCompletion = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();
        // SNIPPET_END: AddingTabCompletion

        // SNIPPET_START: UsingHistory
        // Create a history file
        Path historyFile = Paths.get(System.getProperty("user.home"), ".myapp_history");

        // Create a line reader with history
        LineReader readerWithHistory = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.HISTORY_FILE, historyFile)
                .build();
        // SNIPPET_END: UsingHistory

        // Demonstrate using the reader with history
        String historyLine = readerWithHistory.readLine("history> ");
        System.out.println("You entered with history: " + historyLine);

        // Demonstrate using the reader with completion
        String completionLine = readerWithCompletion.readLine("completion> ");
        System.out.println("You entered with completion: " + completionLine);

        // Close the terminal
        terminal.close();
    }
}
