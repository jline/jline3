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
 * Example demonstrating Jansi terminal provider in JLine.
 */
public class JansiTerminalExample {

    // SNIPPET_START: JansiTerminalExample
    public static void main(String[] args) throws IOException {
        // Create a Jansi-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("jansi") // Explicitly select Jansi provider
                .build();

        System.out.println("Terminal type: " + terminal.getType());

        // Use ANSI escape sequences for colors
        terminal.writer().println("\u001B[1;31mRed text\u001B[0m");
        terminal.writer().println("\u001B[1;32mGreen text\u001B[0m");
        terminal.writer().println("\u001B[1;34mBlue text\u001B[0m");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: JansiTerminalExample
}
