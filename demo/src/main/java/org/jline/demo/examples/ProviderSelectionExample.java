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
 * Example demonstrating terminal provider selection in JLine.
 */
public class ProviderSelectionExample {

    // SNIPPET_START: ProviderSelectionExample
    public static void main(String[] args) throws IOException {
        // Let JLine automatically select the best provider
        Terminal autoTerminal = TerminalBuilder.builder().system(true).build();
        System.out.println("Auto-selected provider: " + autoTerminal.getClass().getSimpleName());

        // Explicitly specify the JNA provider
        Terminal jnaTerminal = TerminalBuilder.builder()
                .system(true)
                .provider("jna") // Explicitly select JNA provider
                .build();
        System.out.println("JNA provider: " + jnaTerminal.getClass().getSimpleName());

        // Explicitly specify the Jansi provider
        Terminal jansiTerminal = TerminalBuilder.builder()
                .system(true)
                .provider("jansi") // Explicitly select Jansi provider
                .build();
        System.out.println("Jansi provider: " + jansiTerminal.getClass().getSimpleName());

        // Close the terminals
        autoTerminal.close();
        jnaTerminal.close();
        jansiTerminal.close();
    }
    // SNIPPET_END: ProviderSelectionExample
}
