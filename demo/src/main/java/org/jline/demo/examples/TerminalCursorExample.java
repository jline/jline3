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
import org.jline.utils.InfoCmp.Capability;

/**
 * Example demonstrating terminal cursor control in JLine.
 */
public class TerminalCursorExample {

    // SNIPPET_START: TerminalCursorExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Clear screen
        terminal.puts(Capability.clear_screen);

        // Move cursor to position (5, 5)
        terminal.puts(Capability.cursor_address, 5, 5);
        terminal.writer().print("Hello at position (5, 5)");
        terminal.flush();

        Thread.sleep(1000);

        // Move cursor to position (10, 10)
        terminal.puts(Capability.cursor_address, 10, 10);
        terminal.writer().print("Hello at position (10, 10)");
        terminal.flush();

        Thread.sleep(1000);

        // Save cursor position
        terminal.puts(Capability.save_cursor);

        // Move cursor to position (15, 15)
        terminal.puts(Capability.cursor_address, 15, 15);
        terminal.writer().print("Hello at position (15, 15)");
        terminal.flush();

        Thread.sleep(1000);

        // Restore cursor position
        terminal.puts(Capability.restore_cursor);
        terminal.writer().print(" (back to saved position)");
        terminal.flush();

        Thread.sleep(2000);

        // Move cursor to bottom
        terminal.puts(Capability.cursor_address, terminal.getHeight() - 1, 0);
        terminal.writer().println("Press Enter to exit");
        terminal.flush();

        // Wait for Enter
        terminal.reader().read();

        terminal.close();
    }
    // SNIPPET_END: TerminalCursorExample
}
