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

        // Explicitly specify the JNI provider (recommended for maximum compatibility)
        Terminal jniTerminal = TerminalBuilder.builder()
                .system(true)
                .provider("jni") // Explicitly select JNI provider
                .build();
        System.out.println("JNI provider: " + jniTerminal.getClass().getSimpleName());

        // Explicitly specify the FFM provider (recommended for Java 22+)
        try {
            Terminal ffmTerminal = TerminalBuilder.builder()
                    .system(true)
                    .provider("ffm") // Explicitly select FFM provider
                    .build();
            System.out.println("FFM provider: " + ffmTerminal.getClass().getSimpleName());
            ffmTerminal.close();
        } catch (Exception e) {
            System.out.println("FFM provider not available: " + e.getMessage());
        }

        // Close the terminals
        autoTerminal.close();
        jniTerminal.close();
    }
    // SNIPPET_END: ProviderSelectionExample
}
