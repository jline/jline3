/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the printAbove feature of JLine.
 *
 * This example is used in the documentation to show how to print text above
 * the current input line.
 */
public class PrintAboveExample {
    public static void main(String[] args) throws Exception {
        // SNIPPET_START: PrintAboveExample
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Start a background thread to print messages
        new Thread(() -> {
                    try {
                        for (int i = 0; i < 10; i++) {
                            Thread.sleep(1000);
                            // HIGHLIGHT: This line prints a message above the current input line
                            reader.printAbove("Notification #" + i);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .start();

        // Read input normally
        while (true) {
            String line = reader.readLine("prompt> ");
            System.out.println("You entered: " + line);
        }
        // SNIPPET_END: PrintAboveExample
    }
}
