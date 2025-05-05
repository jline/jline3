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

import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating AggregateCompleter in JLine.
 */
public class AggregateCompleterExample {

    // SNIPPET_START: AggregateCompleterExample
    public static void main(String[] args) throws IOException {
        Completer aggregateCompleter = new AggregateCompleter(
                new StringsCompleter("help", "exit"),
                new ArgumentCompleter(new StringsCompleter("open"), new Completers.FilesCompleter(Paths.get(""))));

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(aggregateCompleter)
                .build();

        System.out.println("Type a command and press Tab to see completions");
        String line = reader.readLine("agg> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: AggregateCompleterExample
}
