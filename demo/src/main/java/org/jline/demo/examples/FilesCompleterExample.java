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
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating FilesCompleter in JLine.
 */
public class FilesCompleterExample {

    // SNIPPET_START: FilesCompleterExample
    public static void main(String[] args) throws IOException {
        // Create a completer that only completes .txt files
        Completer filesCompleter = new FilesCompleter(Paths.get("."), "*.txt");

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(filesCompleter)
                .build();

        String line = reader.readLine("file> ");
        System.out.println("You selected file: " + line);
    }
    // SNIPPET_END: FilesCompleterExample
}
