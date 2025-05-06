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
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating NullCompleter in JLine.
 */
public class NullCompleterExample {

    // SNIPPET_START: NullCompleterExample
    public static void main(String[] args) throws IOException {
        // Create an argument completer with a null completer at the end
        Completer completer = new ArgumentCompleter(
                new StringsCompleter("command"),
                new StringsCompleter("subcommand1", "subcommand2"),
                NullCompleter.INSTANCE);

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();

        String line = reader.readLine("null> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: NullCompleterExample
}
