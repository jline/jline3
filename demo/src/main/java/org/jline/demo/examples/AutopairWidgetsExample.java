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
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.AutopairWidgets;

/**
 * Example demonstrating autopair widgets in JLine.
 */
public class AutopairWidgetsExample {

    // SNIPPET_START: AutopairWidgetsExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create autopair widgets
        // The second parameter (true) enables curly bracket pairing
        AutopairWidgets autopairWidgets = new AutopairWidgets(reader, true);

        // Enable autopair
        autopairWidgets.enable();

        // Display instructions
        terminal.writer().println("Autopair Widgets Example");
        terminal.writer().println("-----------------------");
        terminal.writer().println("Autopair widgets will automatically:");
        terminal.writer().println("1. Insert matching pairs (quotes, brackets)");
        terminal.writer().println("2. Skip over matched pairs");
        terminal.writer().println("3. Auto-delete pairs on backspace");
        terminal.writer().println("4. Expand/contract spaces between brackets");
        terminal.writer().println();
        terminal.writer().println("Try typing quotes, brackets, etc.");
        terminal.writer().println("Type 'exit' to quit");
        terminal.writer().println();

        // Read input with autopair
        String line;
        while (true) {
            try {
                line = reader.readLine("autopair> ");

                if (line.equalsIgnoreCase("exit")) {
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
    // SNIPPET_END: AutopairWidgetsExample
}
