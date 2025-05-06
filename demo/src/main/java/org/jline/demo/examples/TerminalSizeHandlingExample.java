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

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

/**
 * Example demonstrating terminal size handling in JLine.
 */
public class TerminalSizeHandlingExample {

    // SNIPPET_START: TerminalSizeHandlingExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Get initial terminal size
        Size size = terminal.getSize();
        terminal.writer().println("Initial terminal size: " + size.getColumns() + "x" + size.getRows());
        terminal.writer().flush();

        // Create a latch to wait for resize events
        CountDownLatch latch = new CountDownLatch(1);

        // Register a signal handler for window resize events
        terminal.handle(Terminal.Signal.WINCH, signal -> {
            Size newSize = terminal.getSize();
            terminal.writer().println("\nTerminal resized: " + newSize.getColumns() + "x" + newSize.getRows());

            // Draw a box that fits the terminal
            drawBox(terminal, newSize);

            terminal.writer().flush();
        });

        // Draw initial box
        drawBox(terminal, size);

        terminal.writer().println("\nResize your terminal window to see the box adapt.");
        terminal.writer().println("Press Enter to exit.");
        terminal.writer().flush();

        // Wait for Enter key
        terminal.reader().read();

        terminal.close();
    }

    private static void drawBox(Terminal terminal, Size size) {
        int width = Math.min(size.getColumns() - 2, 78); // Max width of 80 chars
        int height = Math.min(size.getRows() - 5, 15); // Leave room for messages

        // Clear the screen area where the box will be drawn
        terminal.puts(Capability.clear_screen);

        // Draw top border
        terminal.writer().print("┌");
        for (int i = 0; i < width; i++) {
            terminal.writer().print("─");
        }
        terminal.writer().println("┐");

        // Draw sides
        for (int i = 0; i < height; i++) {
            terminal.writer().print("│");
            for (int j = 0; j < width; j++) {
                terminal.writer().print(" ");
            }
            terminal.writer().println("│");
        }

        // Draw bottom border
        terminal.writer().print("└");
        for (int i = 0; i < width; i++) {
            terminal.writer().print("─");
        }
        terminal.writer().println("┘");

        // Print size information in the middle of the box
        String sizeInfo = "Terminal size: " + size.getColumns() + "x" + size.getRows();
        int row = height / 2;
        int col = (width - sizeInfo.length()) / 2;

        terminal.puts(Capability.cursor_address, row + 1, col + 1);
        terminal.writer().print(sizeInfo);
    }
    // SNIPPET_END: TerminalSizeHandlingExample
}
