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
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Example demonstrating non-blocking reader in JLine.
 */
public class NonBlockingReaderExample {

    // SNIPPET_START: NonBlockingReaderExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Get a non-blocking reader
        NonBlockingReader reader = terminal.reader();

        terminal.writer().println("Type something (program will exit after 10 seconds):");
        terminal.writer().flush();

        // Track start time
        long startTime = System.currentTimeMillis();

        // Run for 10 seconds
        while (System.currentTimeMillis() - startTime < 10000) {
            try {
                // Check if input is available
                if (reader.available() > 0) {
                    // Read a character (non-blocking)
                    int c = reader.read();
                    terminal.writer().println("Read character: " + (char) c);
                    terminal.writer().flush();
                }

                // Simulate background work
                terminal.writer().print(".");
                terminal.writer().flush();
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        terminal.writer().println("\nTime's up!");
        terminal.close();
    }
    // SNIPPET_END: NonBlockingReaderExample
}
