/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.Console;

/**
 * Example demonstrating the JLine Console Provider.
 * <p>
 * When the {@code jline-console-provider} module is on the module path and activated
 * with {@code -Djdk.console=org.jline.console.provider}, {@link System#console()}
 * returns a JLine-backed console with line editing, history navigation, and other
 * terminal features.
 * <p>
 * Run with: {@code ./mvx demo console}
 * <p>
 * This demo uses only {@link java.io.Console} API — no JLine imports needed.
 * The provider is activated entirely through the module system and a system property.
 */
public class ConsoleProviderExample {

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("No console available.");
            System.err.println("Make sure you are running from a terminal (not redirected).");
            System.exit(1);
        }

        console.printf("%n=== JLine Console Provider Demo ===%n%n");
        console.printf("Console impl:  %s%n", console.getClass().getName());
        console.printf("%n");

        // Test readLine with prompt
        String name = console.readLine("Enter your name: ");
        if (name == null) {
            console.printf("%nEOF received.%n");
            return;
        }
        console.printf("Hello, %s!%n%n", name);

        // Test readPassword (input is hidden)
        char[] password = console.readPassword("Enter a secret (input hidden): ");
        if (password != null) {
            console.printf("Secret length: %d characters%n%n", password.length);
            java.util.Arrays.fill(password, ' ');
        }

        // Test format / printf
        console.format("Formatted with console.format()%n");
        console.printf("Formatted with console.printf()%n%n");

        // Interactive loop — try arrow keys, line editing
        console.printf("Interactive mode — type lines and press Enter.%n");
        console.printf("Try arrow keys for editing, Up/Down for history.%n");
        console.printf("Type 'quit' or press Ctrl-D to exit.%n%n");

        String line;
        int count = 0;
        while ((line = console.readLine("[%d] > ", ++count)) != null) {
            if ("quit".equalsIgnoreCase(line) || "exit".equalsIgnoreCase(line)) {
                break;
            }
            console.printf("  echo: %s%n", line);
        }

        console.printf("%nGoodbye!%n");
    }
}
