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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.AutosuggestionWidgets;

/**
 * Example demonstrating autosuggestion widgets in JLine.
 */
public class AutosuggestionWidgetsExample {

    // SNIPPET_START: AutosuggestionWidgetsExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a line reader with some completions for demonstration
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("help", "hello", "history", "exit", "quit", "clear"))
                .build();

        // Create autosuggestion widgets
        AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(reader);

        // Enable autosuggestions
        autosuggestionWidgets.enable();

        // Display instructions
        terminal.writer().println("Autosuggestion Widgets Example");
        terminal.writer().println("-----------------------------");
        terminal.writer().println("As you type, you'll see suggestions based on history.");
        terminal.writer().println("- Press RIGHT ARROW or END to accept the full suggestion");
        terminal.writer().println("- Press Ctrl+F to accept the next word of the suggestion");
        terminal.writer().println("- Type 'exit' to quit");
        terminal.writer().println();

        // Add some history entries for suggestions
        reader.getHistory().add("help show available commands");
        reader.getHistory().add("hello world");
        reader.getHistory().add("history show command history");
        reader.getHistory().add("clear screen");

        // Read input with autosuggestions
        String line;
        while (true) {
            try {
                line = reader.readLine("autosuggestion> ");

                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                terminal.writer().println("You entered: " + line);
                terminal.writer().flush();
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
                terminal.writer().flush();
            }
        }

        terminal.close();
    }
    // SNIPPET_END: AutosuggestionWidgetsExample
}
