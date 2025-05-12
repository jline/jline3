/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

/**
 * Example demonstrating custom key bindings.
 */
public class CustomKeyBindingsExample {

    // SNIPPET_START: CustomKeyBindingsExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a line reader with custom key bindings
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Define custom key bindings

        // 1. Bind Ctrl+T to transpose characters
        reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("transpose-chars"), "\u0014"); // Ctrl+T

        // 2. Bind Alt+U to uppercase word
        reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("up-case-word"), "\u001b\u0075"); // Alt+U

        // 3. Bind Alt+L to lowercase word
        reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("down-case-word"), "\u001b\u006C"); // Alt+L

        // 4. Bind Alt+C to capitalize word
        reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("capitalize-word"), "\u001b\u0063"); // Alt+C

        // 5. Bind F5 to clear screen
        reader.getKeyMaps()
                .get(LineReader.MAIN)
                .bind(new Reference("clear-screen"), terminal.getStringCapability(Capability.key_f5));

        // 6. Bind Ctrl+X Ctrl+E to edit-and-execute-command
        reader.getKeyMaps()
                .get(LineReader.MAIN)
                .bind(new Reference("edit-and-execute-command"), "\u0018\u0005"); // Ctrl+X Ctrl+E

        // Print instructions
        terminal.writer().println("Custom Key Bindings Example");
        terminal.writer().println("Try these key combinations:");
        terminal.writer().println("  Ctrl+T  - Transpose characters");
        terminal.writer().println("  Alt+U   - Uppercase word");
        terminal.writer().println("  Alt+L   - Lowercase word");
        terminal.writer().println("  Alt+C   - Capitalize word");
        terminal.writer().println("  F5      - Clear screen");
        terminal.writer().println("  Ctrl+X Ctrl+E - Edit command in external editor");
        terminal.writer().println();

        // Read input with custom key bindings
        String line = reader.readLine("custom-keys> ");
        terminal.writer().println("You entered: " + line);

        // Demonstrate a custom key map with a binding reader
        terminal.writer().println("\nCustom KeyMap Example:");
        terminal.writer().println("Press 'q' to quit, 'h' for help, or 'a', 'b', 'c' for actions");

        // Create a custom key map
        KeyMap<String> keyMap = new KeyMap<>();
        keyMap.bind("quit", "q");
        keyMap.bind("help", "h");
        keyMap.bind("action-a", "a");
        keyMap.bind("action-b", "b");
        keyMap.bind("action-c", "c");

        // Create a binding reader
        BindingReader bindingReader = new BindingReader(terminal.reader());

        // Process key bindings
        while (true) {
            String operation = bindingReader.readBinding(keyMap);

            if ("quit".equals(operation)) {
                terminal.writer().println("Quitting...");
                break;
            } else if ("help".equals(operation)) {
                terminal.writer().println("Help: Press a, b, c for actions, q to quit");
            } else if ("action-a".equals(operation)) {
                terminal.writer().println("Executing action A");
            } else if ("action-b".equals(operation)) {
                terminal.writer().println("Executing action B");
            } else if ("action-c".equals(operation)) {
                terminal.writer().println("Executing action C");
            }

            terminal.flush();
        }

        terminal.close();
    }
    // SNIPPET_END: CustomKeyBindingsExample
}
