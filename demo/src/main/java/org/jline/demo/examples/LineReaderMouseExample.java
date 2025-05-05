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

/**
 * Example demonstrating mouse support in LineReader.
 */
public class LineReaderMouseExample {

    // SNIPPET_START: LineReaderMouseExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Enable mouse support
            terminal.trackMouse(Terminal.MouseTracking.Normal);

            // Create a LineReader with mouse support
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .variable(LineReader.MOUSE, true) // Enable mouse support
                    .build();

            System.out.println("Mouse support enabled in LineReader.");
            System.out.println("Try clicking in the input line or using the mouse wheel.");
            System.out.println("Type 'exit' to quit.");

            // Read lines with mouse support
            String line;
            while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
                System.out.println("You entered: " + line);
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.trackMouse(Terminal.MouseTracking.Off);

            terminal.close();
        }
    }
    // SNIPPET_END: LineReaderMouseExample
}
