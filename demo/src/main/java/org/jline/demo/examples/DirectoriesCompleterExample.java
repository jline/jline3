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

import org.jline.builtins.Completers.DirectoriesCompleter;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating DirectoriesCompleter in JLine.
 */
public class DirectoriesCompleterExample {

    // SNIPPET_START: DirectoriesCompleterExample
    public static void main(String[] args) throws IOException {
        // Create a completer that only completes directory names
        Completer dirCompleter = new DirectoriesCompleter(Paths.get("."));

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(dirCompleter)
                .build();

        String line = reader.readLine("dir> ");
        System.out.println("You selected directory: " + line);
    }
    // SNIPPET_END: DirectoriesCompleterExample
}
