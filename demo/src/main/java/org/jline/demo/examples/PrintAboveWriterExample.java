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
import org.jline.reader.PrintAboveWriter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating the PrintAboveWriter feature of JLine.
 *
 * This example is used in the documentation to show how to use PrintAboveWriter
 * for more control over printing above the current input line.
 */
public class PrintAboveWriterExample {
    public static void main(String[] args) throws Exception {
        // SNIPPET_START: PrintAboveWriterExample
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // HIGHLIGHT_START: Create a PrintAboveWriter
        // Create a PrintAboveWriter
        PrintAboveWriter writer = new PrintAboveWriter(reader);
        // HIGHLIGHT_END

        // Start a background thread to print messages
        new Thread(() -> {
                    try {
                        for (int i = 0; i < 10; i++) {
                            Thread.sleep(1000);

                            // Create a styled message
                            AttributedStringBuilder asb = new AttributedStringBuilder();
                            asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                    .append("Notification #")
                                    .append(String.valueOf(i))
                                    .style(AttributedStyle.DEFAULT);

                            // Print the message above the current line
                            writer.write(asb.toAnsi(terminal));
                            writer.flush();
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
        // SNIPPET_END: PrintAboveWriterExample
    }
}
