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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;

/**
 * Example demonstrating Display class in JLine.
 */
public class DisplayExample {

    // SNIPPET_START: DisplayExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a Display instance
        Display display = new Display(terminal, true);

        // Create some content to display
        List<AttributedString> lines = new ArrayList<>();
        lines.add(new AttributedString("JLine Display Example"));
        lines.add(new AttributedString("==================="));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("This example demonstrates the Display class."));
        lines.add(new AttributedString("The content will update every second."));

        // Display the initial content
        display.resize(terminal.getHeight(), terminal.getWidth());
        display.update(lines, 0);

        // Update the display with a progress indicator
        for (int i = 0; i < 10; i++) {
            // Wait a bit
            TimeUnit.SECONDS.sleep(1);

            // Update the progress line
            StringBuilder progress = new StringBuilder("[");
            for (int j = 0; j < 10; j++) {
                progress.append(j <= i ? "=" : " ");
            }
            progress.append("] ").append((i + 1) * 10).append("%");

            // Update the lines
            lines.set(
                    4,
                    new AttributedString(
                            "Progress: " + progress, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)));

            // Add a status line
            if (lines.size() <= 5) {
                lines.add(new AttributedString(""));
            }
            lines.set(
                    5,
                    new AttributedString(
                            "Status: Processing step " + (i + 1) + " of 10",
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)));

            // Update the display
            display.update(lines, 0);
        }

        // Final update
        lines.set(
                4,
                new AttributedString(
                        "Progress: [==========] 100%", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)));
        lines.set(
                5, new AttributedString("Status: Complete!", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("Press Enter to exit"));

        display.update(lines, 0);

        // Wait for Enter
        terminal.reader().read();
        terminal.close();
    }
    // SNIPPET_END: DisplayExample
}
