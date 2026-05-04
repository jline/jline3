/*
 * Copyright (c) the original author(s).
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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating non-blocking LineReader in JLine.
 */
public class NonBlockingLineReaderExample {

    // SNIPPET_START: NonBlockingLineReaderExample
    public static void main(String[] args) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            LineReader lineReader =
                    LineReaderBuilder.builder().terminal(terminal).build();

            // Flag to control background task
            AtomicBoolean running = new AtomicBoolean(true);

            // Start background task
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    while (running.get()) {
                        // Simulate background work
                        terminal.writer().print(".");
                        terminal.writer().flush();
                        TimeUnit.SECONDS.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    if (running.get()) {
                        terminal.writer().println("Error in background task: " + e.getMessage());
                        terminal.writer().flush();
                    }
                }
            });

            try {
                // Main input loop
                while (running.get()) {
                    try {
                        // Read a line (this will block)
                        String line = lineReader.readLine("\nprompt> ");

                        if ("exit".equalsIgnoreCase(line)) {
                            running.set(false);
                        } else {
                            terminal.writer().println("You entered: " + line);
                            terminal.writer().flush();
                        }
                    } catch (UserInterruptException e) {
                        // Ctrl+C pressed
                        running.set(false);
                    }
                }

                terminal.writer().println("\nExiting...");
            } finally {
                // Shutdown background task while terminal is still open
                running.set(false);
                executor.shutdownNow();
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    // SNIPPET_END: NonBlockingLineReaderExample
}
