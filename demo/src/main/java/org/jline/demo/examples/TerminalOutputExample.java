/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.PrintWriter;

import org.jline.terminal.Terminal;

/**
 * Example demonstrating terminal output in JLine.
 */
public class TerminalOutputExample {

    // SNIPPET_START: TerminalOutputExample
    public void writeOutput(Terminal terminal) {
        // Get the terminal writer
        PrintWriter writer = terminal.writer();

        // Write text
        writer.println("Hello, JLine!");
        writer.flush();

        // Use ANSI escape sequences for formatting (if supported)
        writer.println("\u001B[1;31mThis text is bold and red\u001B[0m");
        writer.flush();
    }
    // SNIPPET_END: TerminalOutputExample
}
