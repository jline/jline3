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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating custom widgets in JLine.
 */
public class CustomWidgetsExample {

    // SNIPPET_START: CustomWidgetsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create custom widgets

        // Widget to insert the current date
        Widget insertDateWidget = () -> {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            reader.getBuffer().write(date);
            return true;
        };

        // Widget to duplicate the current line
        Widget duplicateLineWidget = () -> {
            String currentLine = reader.getBuffer().toString();
            reader.getBuffer().write("\n" + currentLine);
            return true;
        };

        // Widget to reverse the current word
        Widget reverseWordWidget = () -> {
            // Get the current buffer
            String buffer = reader.getBuffer().toString();
            int cursor = reader.getBuffer().cursor();

            // Find the start and end of the current word
            int start = buffer.lastIndexOf(' ', cursor - 1) + 1;
            int end = buffer.indexOf(' ', cursor);
            if (end == -1) end = buffer.length();

            // Extract the current word
            String word = buffer.substring(start, end);

            // Replace with reversed version
            reader.getBuffer().cursor(start);
            reader.getBuffer().delete(end - start);
            reader.getBuffer().write(new StringBuilder(word).reverse().toString());

            return true;
        };

        // Register the widgets
        reader.getWidgets().put("insert-date", insertDateWidget);
        reader.getWidgets().put("duplicate-line", duplicateLineWidget);
        reader.getWidgets().put("reverse-word", reverseWordWidget);

        // Bind keys to the widgets
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        keyMap.bind(insertDateWidget, KeyMap.alt('d'));
        keyMap.bind(duplicateLineWidget, KeyMap.alt('l'));
        keyMap.bind(reverseWordWidget, KeyMap.alt('r'));

        // Display instructions
        terminal.writer().println("Custom widgets:");
        terminal.writer().println("  Alt+D: Insert current date");
        terminal.writer().println("  Alt+L: Duplicate current line");
        terminal.writer().println("  Alt+R: Reverse current word");
        terminal.writer().println("\nType some text and try the custom widgets:");
        terminal.writer().flush();

        // Read lines until "exit" is entered
        String line;
        while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
            terminal.writer().println("You entered: " + line);
        }

        terminal.close();
    }
    // SNIPPET_END: CustomWidgetsExample
}
