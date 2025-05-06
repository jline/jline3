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
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating built-in widgets in JLine.
 */
public class BuiltinWidgetsExample {

    // SNIPPET_START: BuiltinWidgetsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Get the main key map
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);

        // Bind keys to built-in widgets
        keyMap.bind(new Reference(LineReader.CLEAR_SCREEN), KeyMap.ctrl('L')); // Clear screen
        keyMap.bind(new Reference(LineReader.BACKWARD_KILL_WORD), KeyMap.alt('h')); // Delete word backward
        keyMap.bind(new Reference(LineReader.KILL_WORD), KeyMap.alt('d')); // Delete word forward
        keyMap.bind(new Reference(LineReader.BEGINNING_OF_LINE), KeyMap.ctrl('A')); // Move to beginning of line
        keyMap.bind(new Reference(LineReader.END_OF_LINE), KeyMap.ctrl('E')); // Move to end of line
        keyMap.bind(new Reference(LineReader.UP_HISTORY), KeyMap.ctrl('P')); // Previous history entry
        keyMap.bind(new Reference(LineReader.DOWN_HISTORY), KeyMap.ctrl('N')); // Next history entry
        keyMap.bind(new Reference(LineReader.BACKWARD_WORD), KeyMap.alt('b')); // Move backward one word
        keyMap.bind(new Reference(LineReader.FORWARD_WORD), KeyMap.alt('f')); // Move forward one word
        keyMap.bind(new Reference(LineReader.CAPITALIZE_WORD), KeyMap.alt('c')); // Capitalize word
        keyMap.bind(new Reference(LineReader.TRANSPOSE_CHARS), KeyMap.ctrl('T')); // Transpose characters

        // Display instructions
        terminal.writer().println("Built-in widgets bound to keys:");
        terminal.writer().println("  Ctrl+L: Clear screen");
        terminal.writer().println("  Alt+H: Delete word backward");
        terminal.writer().println("  Alt+D: Delete word forward");
        terminal.writer().println("  Ctrl+A: Move to beginning of line");
        terminal.writer().println("  Ctrl+E: Move to end of line");
        terminal.writer().println("  Ctrl+P: Previous history entry");
        terminal.writer().println("  Ctrl+N: Next history entry");
        terminal.writer().println("  Alt+B: Move backward one word");
        terminal.writer().println("  Alt+F: Move forward one word");
        terminal.writer().println("  Alt+C: Capitalize word");
        terminal.writer().println("  Ctrl+T: Transpose characters");
        terminal.writer().println("\nType some text and try the key bindings:");
        terminal.writer().flush();

        // Read lines until "exit" is entered
        String line;
        while (!(line = reader.readLine("prompt> ")).equalsIgnoreCase("exit")) {
            terminal.writer().println("You entered: " + line);
        }

        terminal.close();
    }
    // SNIPPET_END: BuiltinWidgetsExample
}
