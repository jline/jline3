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
 * Example demonstrating Exec terminal provider in JLine.
 */
public class ExecTerminalExample {

    // SNIPPET_START: ExecTerminalExample
    public static void main(String[] args) throws IOException {
        // Create an Exec-based terminal
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("exec") // Explicitly select Exec provider
                .build();

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from Exec terminal!");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: ExecTerminalExample
}
