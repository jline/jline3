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
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating basic key bindings in JLine.
 */
public class KeyBindingBasicsExample {

    // SNIPPET_START: KeyBindingBasicsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Get the main key map
        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);

        // Display some default key bindings
        terminal.writer().println("Default key bindings:");
        terminal.writer().println("  Ctrl+A: " + keyMap.getBound(KeyMap.ctrl('A')));
        terminal.writer().println("  Ctrl+E: " + keyMap.getBound(KeyMap.ctrl('E')));
        terminal.writer().println("  Ctrl+L: " + keyMap.getBound(KeyMap.ctrl('L')));
        terminal.writer().println("  Ctrl+R: " + keyMap.getBound(KeyMap.ctrl('R')));
        terminal.writer().println("  Ctrl+U: " + keyMap.getBound(KeyMap.ctrl('U')));
        terminal.writer().flush();

        // Read a line to demonstrate the key bindings
        terminal.writer().println("\nType some text (try using the key bindings above):");
        String line = reader.readLine("prompt> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: KeyBindingBasicsExample
}
