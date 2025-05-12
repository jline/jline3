/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;

/**
 * Example demonstrating key bindings in JLine.
 */
public class KeyBindingExample {

    // SNIPPET_START: KeyBindingExample
    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Get the main key map
            KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);

            // Bind Ctrl+T to transpose characters (swap the character before and at the cursor)
            keyMap.bind(new Reference(LineReader.TRANSPOSE_CHARS), ctrl('T'));

            // Bind Alt+U to uppercase the current word
            keyMap.bind(new Reference("upcase-word"), alt('U'));

            // Bind Alt+L to lowercase the current word
            keyMap.bind(new Reference("downcase-word"), alt('L'));

            // Bind Alt+C to capitalize the current word
            keyMap.bind(new Reference(LineReader.CAPITALIZE_WORD), alt('C'));

            // Bind Ctrl+K to kill (cut) text from cursor to end of line
            keyMap.bind(new Reference(LineReader.KILL_LINE), ctrl('K'));

            // Bind Ctrl+Y to yank (paste) previously killed text
            keyMap.bind(new Reference(LineReader.YANK), ctrl('Y'));

            // Bind Ctrl+R to reverse search in history
            keyMap.bind(new Reference(LineReader.HISTORY_INCREMENTAL_SEARCH_BACKWARD), ctrl('R'));

            // Display instructions
            terminal.writer().println("Key Binding Example");
            terminal.writer().println("------------------");
            terminal.writer().println("Try these key bindings:");
            terminal.writer().println("  Ctrl+T: Transpose characters");
            terminal.writer().println("  Alt+U: Uppercase word");
            terminal.writer().println("  Alt+L: Lowercase word");
            terminal.writer().println("  Alt+C: Capitalize word");
            terminal.writer().println("  Ctrl+K: Kill line");
            terminal.writer().println("  Ctrl+Y: Yank");
            terminal.writer().println("  Ctrl+R: Search history");
            terminal.writer().println();
            terminal.writer().println("Type 'exit' to quit");
            terminal.writer().println();

            // Read lines with custom key bindings
            while (true) {
                try {
                    String line = reader.readLine("keybinding> ");

                    if ("exit".equalsIgnoreCase(line)) {
                        break;
                    }

                    terminal.writer().println("You entered: " + line);
                    terminal.writer().flush();
                } catch (UserInterruptException e) {
                    // Ctrl+C
                    terminal.writer().println("Interrupted");
                } catch (EndOfFileException e) {
                    // Ctrl+D
                    terminal.writer().println("EOF");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: KeyBindingExample
}
