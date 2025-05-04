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
import java.nio.file.Paths;

import org.jline.builtins.Completers.FilesCompleter;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Examples demonstrating tab completion functionality.
 */
public class TabCompletionExample {
    public static void main(String[] args) throws IOException {
        // SNIPPET_START: basic-completion
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
        // SNIPPET_END: basic-completion

        // SNIPPET_START: argument-completion
        // Create a more complex completer for command arguments
        Completer argCompleter = new ArgumentCompleter(
                new StringsCompleter("open", "close", "save"),
                new FilesCompleter(Paths.get(System.getProperty("user.dir"))));

        // Create a line reader with the argument completer
        LineReader argReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(argCompleter)
                .build();

        System.out.println("Type 'open', 'close', or 'save' followed by a file path");
        System.out.println("Press Tab to complete the file path");
        String command = argReader.readLine("command> ");
        System.out.println("You entered: " + command);
        // SNIPPET_END: argument-completion
    }
}
