/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the FileNameCompleter.
 */
public class FileNameCompleterExample {

    // SNIPPET_START: FileNameCompleterExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a file name completer
        FileNameCompleter completer = new FileNameCompleter();

        // Create a line reader with the file name completer
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();

        // Read input with file name completion
        String line = reader.readLine("Enter a file path: ");
        System.out.println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: FileNameCompleterExample
}
