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
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating how to create a progress bar with AttributedString.
 */
public class ProgressBarExample {

    // SNIPPET_START: ProgressBarExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        for (int i = 0; i <= 100; i += 5) {
            // Create a progress bar
            AttributedString progressBar = createProgressBar(i, 50);

            // Clear the line and print the progress bar
            terminal.writer().print("\r");
            progressBar.print(terminal);

            // Sleep to simulate work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        terminal.writer().println();
        terminal.close();
    }

    private static AttributedString createProgressBar(int percentage, int width) {
        AttributedStringBuilder builder = new AttributedStringBuilder();

        // Calculate the number of completed and remaining segments
        int completed = width * percentage / 100;
        int remaining = width - completed;

        // Add the percentage
        builder.style(AttributedStyle.DEFAULT.bold()).append(String.format("%3d%% ", percentage));

        // Add the progress bar
        builder.append("[");

        // Add completed segments
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append(repeat("=", completed));

        // Add remaining segments
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK)).append(repeat(" ", remaining));

        // Close the progress bar
        builder.style(AttributedStyle.DEFAULT).append("]");

        return builder.toAttributedString();
    }

    // Helper method to replace String.repeat() which is only available in Java 11+
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    // SNIPPET_END: ProgressBarExample
}
