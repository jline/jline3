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
 * Example demonstrating argument completion.
 */
public class ArgumentCompleterExample {
    // SNIPPET_START: ArgumentCompleterExample
    public static void main(String[] args) throws IOException {
        // First argument is a command, second is a file
        Completer commandCompleter = new StringsCompleter("open", "save", "delete");
        Completer fileCompleter = new FilesCompleter(Paths.get(System.getProperty("user.dir")));

        // HIGHLIGHT_START: Create an ArgumentCompleter
        Completer argCompleter = new ArgumentCompleter(commandCompleter, fileCompleter);
        // HIGHLIGHT_END

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(argCompleter)
                .build();

        System.out.println("Type a command followed by a file path and press Tab");
        String line = reader.readLine("cmd> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: ArgumentCompleterExample
}
