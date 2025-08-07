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
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating terminal creation in JLine.
 */
public class TerminalCreationExample {

    // SNIPPET_START: TerminalCreationExample
    public static void main(String[] args) throws IOException {
        // Create a system terminal (auto-detected)
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a dumb terminal (minimal functionality)
        Terminal dumbTerminal = TerminalBuilder.builder().dumb(true).build();

        // Create a terminal with specific settings
        Terminal customTerminal = TerminalBuilder.builder()
                .name("CustomTerminal")
                .system(false)
                .streams(System.in, System.out)
                .encoding(StandardCharsets.UTF_8)
                .build();
    }
    // SNIPPET_END: TerminalCreationExample
}
