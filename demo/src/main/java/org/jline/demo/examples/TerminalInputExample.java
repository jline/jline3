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
import java.io.Reader;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating terminal input in JLine.
 */
public class TerminalInputExample {

    // SNIPPET_START: TerminalInputExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Get the terminal reader
        Reader reader = terminal.reader();

        terminal.writer().println("Press any key (or 'q' to quit):");
        terminal.writer().flush();

        // Read characters
        int c;
        while ((c = reader.read()) != 'q') {
            terminal.writer().printf("You pressed: %c (ASCII: %d)%n", (char) c, c);
            terminal.writer().flush();
        }

        terminal.close();
    }
    // SNIPPET_END: TerminalInputExample
}
