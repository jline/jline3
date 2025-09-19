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
 * Example demonstrating terminal signal handling configuration.
 */
public class TerminalSignalHandling {

    public static void main(String[] args) throws IOException {
        // SNIPPET_START: TerminalSignalHandling
        // Create a terminal with custom signal handling
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .signalHandler(Terminal.SignalHandler.SIG_IGN)
                .build();
        // SNIPPET_END: TerminalSignalHandling

        System.out.println(
                "Terminal with signal handling created: " + terminal.getClass().getSimpleName());

        terminal.close();
    }
}
