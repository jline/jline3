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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating SystemCompleter in JLine.
 */
public class SystemCompleterExample {

    // SNIPPET_START: SystemCompleterExample
    public static void main(String[] args) throws IOException {
        // Create a system completer
        SystemCompleter systemCompleter = new SystemCompleter();

        // Add completers for different commands
        systemCompleter.add("help", new StringsCompleter("commands", "usage", "options"));

        // Add a more complex completer for the "connect" command
        systemCompleter.add(
                "connect",
                new ArgumentCompleter(
                        new StringsCompleter("connect"),
                        new StringsCompleter("server1", "server2", "server3"),
                        NullCompleter.INSTANCE));

        // Compile the completers
        systemCompleter.compile();

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemCompleter)
                .build();

        String line = reader.readLine("system> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: SystemCompleterExample
}
