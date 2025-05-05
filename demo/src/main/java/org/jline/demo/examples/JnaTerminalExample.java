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
 * Example demonstrating JNA terminal provider in JLine.
 */
public class JnaTerminalExample {

    // SNIPPET_START: JnaTerminalExample
    public static void main(String[] args) throws IOException {
        // Create a JNA-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("jna") // Explicitly select JNA provider
                .build();

        System.out.println("Terminal type: " + terminal.getType());
        System.out.println("Terminal size: " + terminal.getWidth() + "x" + terminal.getHeight());

        terminal.writer().println("Hello from JNA terminal!");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: JnaTerminalExample
}
