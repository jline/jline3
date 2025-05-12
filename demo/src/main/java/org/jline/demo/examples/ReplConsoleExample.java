/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating a simple REPL console with JLine.
 */
public class ReplConsoleExample {

    // SNIPPET_START: ReplConsoleExample
    public static void main(String[] args) {
        try {
            // Setup the terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create the line reader
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .build();

            // Set the prompt
            String prompt = "repl> ";

            // REPL loop
            while (true) {
                try {
                    // Read a line
                    String line = reader.readLine(prompt);

                    // Process the line (in this example, just echo it back)
                    if (line.equalsIgnoreCase("exit")) {
                        break;
                    }

                    // Display the result
                    terminal.writer().println("You entered: " + line);
                    terminal.writer().flush();

                } catch (UserInterruptException e) {
                    // Ignore Ctrl+C
                } catch (EndOfFileException e) {
                    // Exit on Ctrl+D
                    return;
                }
            }

            terminal.writer().println("Goodbye!");
            terminal.writer().flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ReplConsoleExample
}
