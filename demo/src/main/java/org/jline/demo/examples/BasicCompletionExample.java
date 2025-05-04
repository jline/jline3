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

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating basic tab completion.
 */
public class BasicCompletionExample {
    // SNIPPET_START: BasicCompletionExample
    public static void main(String[] args) throws IOException {
        // HIGHLIGHT_START: Create a simple completer with fixed options
        // Create a simple completer with fixed options
        Completer completer = new StringsCompleter("help", "exit", "list", "version");
        // HIGHLIGHT_END

        // Create a line reader with the completer
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();

        System.out.println("Type a command and press Tab to see completions");
        // Now when the user presses Tab, they'll see the available commands
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: BasicCompletionExample
}
