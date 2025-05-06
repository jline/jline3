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

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating terminal size handling in JLine.
 */
public class TerminalSizeExample {

    // SNIPPET_START: TerminalSizeExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Get initial size
        Size size = terminal.getSize();
        terminal.writer().printf("Terminal size: %d columns x %d rows%n", size.getColumns(), size.getRows());
        terminal.writer().flush();

        // Register a resize listener
        terminal.handle(Terminal.Signal.WINCH, signal -> {
            Size newSize = terminal.getSize();
            terminal.writer()
                    .printf("Terminal resized: %d columns x %d rows%n", newSize.getColumns(), newSize.getRows());
            terminal.writer().flush();
        });

        terminal.writer().println("Resize your terminal window to see the size change.");
        terminal.writer().println("Press Enter to exit.");
        terminal.writer().flush();

        // Wait for Enter
        terminal.reader().read();

        terminal.close();
    }
    // SNIPPET_END: TerminalSizeExample
}
