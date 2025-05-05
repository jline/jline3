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

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Example demonstrating timeout read in JLine.
 */
public class TimeoutReadExample {

    // SNIPPET_START: TimeoutReadExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        NonBlockingReader reader = terminal.reader();

        terminal.writer().println("Press any key within 5 seconds:");
        terminal.writer().flush();

        try {
            // Read with a 5-second timeout
            int c = reader.read(5000L);

            if (c != -1) {
                terminal.writer().println("You pressed: " + (char) c);
            } else {
                terminal.writer().println("Timeout expired!");
            }
        } catch (IOException e) {
            terminal.writer().println("Error reading input: " + e.getMessage());
        }

        terminal.close();
    }
    // SNIPPET_END: TimeoutReadExample
}
