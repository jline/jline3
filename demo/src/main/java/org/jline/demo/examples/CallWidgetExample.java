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
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating calling widgets programmatically in JLine.
 */
public class CallWidgetExample {

    // SNIPPET_START: CallWidgetExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Display instructions
        terminal.writer().println("This example demonstrates calling widgets programmatically.");
        terminal.writer().println("The line will be pre-filled and the cursor positioned.");
        terminal.writer().println("\nPress a key to continue...");
        terminal.writer().flush();
        terminal.reader().read();

        // Set up a callback to be called before reading a line
        reader.setVariable(LineReader.BELL_STYLE, "none");
        reader.setVariable(LineReader.CALLBACK_INIT, (Function<LineReader, Boolean>) r -> {
            // Pre-fill the line buffer
            r.getBuffer().write("This is a pre-filled line");

            // Move cursor to the beginning of the line
            r.callWidget(LineReader.BEGINNING_OF_LINE);

            // Move forward by 5 characters
            for (int i = 0; i < 5; i++) {
                r.callWidget(LineReader.FORWARD_CHAR);
            }

            return true;
        });

        // Read a line
        String line = reader.readLine("prompt> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: CallWidgetExample
}
