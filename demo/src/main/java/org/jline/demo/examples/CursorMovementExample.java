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
import org.jline.utils.InfoCmp.Capability;

/**
 * Example demonstrating cursor movement in JLine.
 */
public class CursorMovementExample {

    // SNIPPET_START: CursorMovementExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Clear the screen
        terminal.puts(Capability.clear_screen);
        terminal.flush();

        // Move cursor to position (5, 10) and print text
        terminal.puts(Capability.cursor_address, 5, 10);
        terminal.writer().print("Text at position (5, 10)");
        terminal.flush();

        TimeUnit.SECONDS.sleep(1);

        // Move cursor to position (8, 15) and print text
        terminal.puts(Capability.cursor_address, 8, 15);
        terminal.writer().print("Text at position (8, 15)");
        terminal.flush();

        TimeUnit.SECONDS.sleep(1);

        // Save cursor position
        terminal.puts(Capability.save_cursor);

        // Move cursor to position (12, 5) and print text
        terminal.puts(Capability.cursor_address, 12, 5);
        terminal.writer().print("Text at position (12, 5)");
        terminal.flush();

        TimeUnit.SECONDS.sleep(1);

        // Restore cursor position and print additional text
        terminal.puts(Capability.restore_cursor);
        terminal.writer().print(" - Cursor restored here");
        terminal.flush();

        // Move cursor to bottom for exit message
        terminal.puts(Capability.cursor_address, 15, 0);
        terminal.writer().println("\nPress Enter to exit");
        terminal.flush();

        terminal.reader().read();
        terminal.close();
    }
    // SNIPPET_END: CursorMovementExample
}
