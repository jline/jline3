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
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example showing a simple REPL (Read-Eval-Print Loop) using JLine.
 */
public class SimpleLineReadingExample {

    // SNIPPET_START: SimpleLineReadingExample
    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Set the prompt
            String prompt = "jline> ";

            // REPL loop
            while (true) {
                String line = null;
                try {
                    // Read a line
                    line = reader.readLine(prompt);

                    // Process the line (in this example, just echo it back)
                    System.out.println("You entered: " + line);

                    // Exit if the user types "exit"
                    if ("exit".equalsIgnoreCase(line)) {
                        break;
                    }
                } catch (UserInterruptException e) {
                    // Ctrl+C pressed, ignore
                    System.out.println("KeyboardInterrupt (Ctrl+C)");
                } catch (EndOfFileException e) {
                    // Ctrl+D pressed, exit
                    System.out.println("End of file (Ctrl+D)");
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: SimpleLineReadingExample
}
