/*
 * Copyright (c) 2002-2025, the original author(s).
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
 * Example demonstrating basic terminal creation using TerminalBuilder.
 */
public class BasicTerminalCreation {

    public static void main(String[] args) throws IOException {
        // SNIPPET_START: BasicTerminalCreation
        // Simple terminal creation
        Terminal terminal = TerminalBuilder.terminal();

        // Or using the builder pattern
        Terminal terminal2 = TerminalBuilder.builder().system(true).build();
        // SNIPPET_END: BasicTerminalCreation

        System.out.println("Terminal created: " + terminal.getClass().getSimpleName());
        System.out.println("Terminal 2 created: " + terminal2.getClass().getSimpleName());

        terminal.close();
        terminal2.close();
    }
}
