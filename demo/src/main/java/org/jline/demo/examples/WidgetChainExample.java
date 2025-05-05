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

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating widget chains in JLine.
 */
public class WidgetChainExample {

    // SNIPPET_START: WidgetChainExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create a widget that chains multiple operations
        Widget formatTextWidget = () -> {
            // First, call the beginning-of-line widget
            reader.callWidget(LineReader.BEGINNING_OF_LINE);

            // Then, call the capitalize-word widget
            reader.callWidget(LineReader.CAPITALIZE_WORD);

            // Move to the end of the line
            reader.callWidget(LineReader.END_OF_LINE);

            // Add a period if not already present
            String buffer = reader.getBuffer().toString();
            if (!buffer.endsWith(".")) {
                reader.getBuffer().write(".");
            }

            return true;
        };

        // Create a widget that wraps text in quotes
        Widget quoteTextWidget = () -> {
            // Save the current buffer
            String buffer = reader.getBuffer().toString();

            // Clear the buffer
            reader.callWidget(LineReader.BEGINNING_OF_LINE);
            reader.callWidget(LineReader.KILL_LINE);

            // Write the quoted text
            reader.getBuffer().write("\"" + buffer + "\"");

            return true;
        };

        // Register the widgets
        reader.getWidgets().put("format-text", formatTextWidget);
        reader.getWidgets().put("quote-text", quoteTextWidget);

        // Bind keys to the widgets
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        keyMap.bind(formatTextWidget, KeyMap.alt('f'));
        keyMap.bind(quoteTextWidget, KeyMap.alt('q'));

        // Display instructions
        terminal.writer().println("Widget Chain Example");
        terminal.writer().println("  Alt+F: Format text (capitalize first word and add period)");
        terminal.writer().println("  Alt+Q: Quote text (wrap in double quotes)");
        terminal.writer().println();

        // Read input with widget chains
        String line = reader.readLine("chain> ");

        terminal.writer().println("You entered: " + line);
        terminal.close();
    }
    // SNIPPET_END: WidgetChainExample
}
