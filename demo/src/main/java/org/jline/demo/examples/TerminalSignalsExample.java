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
import java.util.concurrent.CountDownLatch;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating terminal signals in JLine.
 */
public class TerminalSignalsExample {

    // SNIPPET_START: TerminalSignalsExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a latch to wait for signal
        CountDownLatch latch = new CountDownLatch(1);

        // Register signal handler for CTRL+C (SIGINT)
        terminal.handle(Terminal.Signal.INT, signal -> {
            terminal.writer().println("Received SIGINT (CTRL+C)");
            terminal.writer().flush();
            latch.countDown();
        });

        terminal.writer().println("Press CTRL+C to test signal handling");
        terminal.writer().flush();

        // Wait for signal
        latch.await();

        terminal.close();
    }
    // SNIPPET_END: TerminalSignalsExample
}
