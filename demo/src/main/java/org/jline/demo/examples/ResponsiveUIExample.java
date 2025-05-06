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

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;

/**
 * Example demonstrating responsive UI in JLine.
 */
public class ResponsiveUIExample {

    // SNIPPET_START: ResponsiveUIExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a display for managing the screen
        Display display = new Display(terminal, true);

        // Get initial terminal size
        Size size = terminal.getSize();

        // Create initial content
        List<AttributedString> content = createContent(size);

        // Update the display
        display.resize(size.getRows(), size.getColumns());
        display.update(content, -1);

        // Register a signal handler for window resize events
        terminal.handle(Terminal.Signal.WINCH, signal -> {
            Size newSize = terminal.getSize();

            // Create new content based on the new size
            List<AttributedString> newContent = createContent(newSize);

            // Update the display
            display.resize(newSize.getRows(), newSize.getColumns());
            display.update(newContent, -1);
        });

        terminal.writer().println("\nResize your terminal window to see the UI adapt.");
        terminal.writer().println("Press Enter to exit.");
        terminal.writer().flush();

        // Wait for Enter key
        terminal.reader().read();

        terminal.close();
    }

    private static List<AttributedString> createContent(Size size) {
        List<AttributedString> content = new ArrayList<>();

        // Add a header
        content.add(new AttributedString(
                "Responsive UI Example",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold()));
        content.add(new AttributedString("Terminal size: " + size.getColumns() + "x" + size.getRows()));
        content.add(new AttributedString(""));

        // Create a table with columns that adapt to the terminal width
        int availableWidth = size.getColumns() - 4; // Leave some margin

        if (availableWidth >= 60) {
            // Wide terminal - show 3 columns
            content.add(new AttributedString("ID  | Name                | Description"));
            content.add(new AttributedString("----+---------------------+-------------------------"));
            content.add(new AttributedString("1   | Item One            | First item in the list"));
            content.add(new AttributedString("2   | Item Two            | Second item in the list"));
            content.add(new AttributedString("3   | Item Three          | Third item in the list"));
        } else if (availableWidth >= 40) {
            // Medium terminal - show 2 columns
            content.add(new AttributedString("ID  | Name"));
            content.add(new AttributedString("----+---------------------"));
            content.add(new AttributedString("1   | Item One"));
            content.add(new AttributedString("2   | Item Two"));
            content.add(new AttributedString("3   | Item Three"));
            content.add(new AttributedString(""));
            content.add(new AttributedString("Descriptions:"));
            content.add(new AttributedString("1: First item in the list"));
            content.add(new AttributedString("2: Second item in the list"));
            content.add(new AttributedString("3: Third item in the list"));
        } else {
            // Narrow terminal - show 1 column at a time
            content.add(new AttributedString("Items:"));
            content.add(new AttributedString(""));
            content.add(new AttributedString("1: Item One"));
            content.add(new AttributedString("2: Item Two"));
            content.add(new AttributedString("3: Item Three"));
        }

        return content;
    }
    // SNIPPET_END: ResponsiveUIExample
}
