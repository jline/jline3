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
import java.util.concurrent.TimeUnit;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.EnumCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating EnumCompleter in JLine.
 */
public class EnumCompleterExample {

    // SNIPPET_START: EnumCompleterExample
    public static void main(String[] args) throws IOException {
        // Create a completer that completes enum values
        Completer enumCompleter = new EnumCompleter(TimeUnit.class);

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(enumCompleter)
                .build();

        String line = reader.readLine("timeunit> ");
        System.out.println("You selected: " + line);
    }
    // SNIPPET_END: EnumCompleterExample
}
