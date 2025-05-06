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
import java.time.LocalDateTime;
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
public class CustomWidgetExample {

    // SNIPPET_START: CustomWidgetExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create a custom widget that inserts the current date and time
        Widget insertDateTimeWidget = () -> {
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            reader.getBuffer().write(dateTime);
            return true;
        };

        // Create a custom widget that converts the current word to uppercase
        Widget uppercaseWordWidget = () -> {
            // Get the current buffer
            String buffer = reader.getBuffer().toString();
            int cursor = reader.getBuffer().cursor();

            // Find the start and end of the current word
            int start = buffer.lastIndexOf(' ', cursor - 1) + 1;
            if (start < 0) start = 0;

            int end = buffer.indexOf(' ', cursor);
            if (end < 0) end = buffer.length();

            // Extract the current word
            String word = buffer.substring(start, end);

            // Replace with uppercase version
            reader.getBuffer().cursor(start);
            reader.getBuffer().delete(end - start);
            reader.getBuffer().write(word.toUpperCase());

            return true;
        };

        // Register the widgets
        reader.getWidgets().put("insert-date-time", insertDateTimeWidget);
        reader.getWidgets().put("uppercase-word", uppercaseWordWidget);

        // Bind keys to the widgets
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        keyMap.bind(insertDateTimeWidget, KeyMap.alt('t'));
        keyMap.bind(uppercaseWordWidget, KeyMap.alt('u'));

        // Display instructions
        terminal.writer().println("Custom Widget Example");
        terminal.writer().println("  Alt+T: Insert current date and time");
        terminal.writer().println("  Alt+U: Convert current word to uppercase");
        terminal.writer().println();

        // Read input with custom widgets
        String line = reader.readLine("widgets> ");

        terminal.writer().println("You entered: " + line);
        terminal.close();
    }
    // SNIPPET_END: CustomWidgetExample
}
