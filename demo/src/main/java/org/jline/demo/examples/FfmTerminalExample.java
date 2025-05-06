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
 * Example demonstrating FFM terminal provider in JLine.
 */
public class FfmTerminalExample {

    // SNIPPET_START: FfmTerminalExample
    public static void main(String[] args) throws IOException {
        // Create an FFM-based terminal (requires Java 22+)
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .provider("ffm") // Explicitly select FFM provider
                .build();

        System.out.println("Terminal type: " + terminal.getType());

        terminal.writer().println("Hello from FFM terminal!");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: FfmTerminalExample
}
