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

/**
 * Example demonstrating dumb terminal in JLine.
 */
public class DumbTerminalExample {

    // SNIPPET_START: DumbTerminalExample
    public static void main(String[] args) throws IOException {
        // Create a dumb terminal
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true) // Request a dumb terminal
                .build();

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from dumb terminal!");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: DumbTerminalExample
}
