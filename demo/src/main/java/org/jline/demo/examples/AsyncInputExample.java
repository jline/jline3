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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Example demonstrating asynchronous input handling in JLine.
 */
public class AsyncInputExample {

    // SNIPPET_START: AsyncInputExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        NonBlockingReader reader = terminal.reader();

        // Flag to control input handling
        AtomicBoolean running = new AtomicBoolean(true);

        // Start input handling thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                // Continuously read input
                while (running.get()) {
                    int c = reader.read(100);
                    if (c != -1) {
                        // Process the input
                        terminal.writer().println("\nReceived input: " + (char) c);

                        if (c == 'q' || c == 'Q') {
                            running.set(false);
                        }

                        terminal.writer().flush();
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    terminal.writer().println("Error reading input: " + e.getMessage());
                    terminal.writer().flush();
                }
            }
        });

        // Main application loop
        try {
            terminal.writer().println("Press keys (q to quit):");
            terminal.writer().flush();

            int count = 0;
            while (running.get() && count < 30) {
                // Simulate application work
                terminal.writer().print(".");
                terminal.writer().flush();

                TimeUnit.MILLISECONDS.sleep(500);
                count++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Shutdown input handling
            running.set(false);
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            terminal.writer().println("\nExiting...");
            terminal.close();
        }
    }
    // SNIPPET_END: AsyncInputExample
}
