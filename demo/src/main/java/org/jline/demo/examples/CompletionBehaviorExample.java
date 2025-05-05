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
 * Example demonstrating completion behavior configuration in JLine.
 */
public class CompletionBehaviorExample {

    // SNIPPET_START: CompletionBehaviorExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Completer completer = new StringsCompleter("help", "exit", "list", "connect", "disconnect");

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .option(LineReader.Option.AUTO_LIST, true) // Automatically list options
                .option(LineReader.Option.LIST_PACKED, true) // Display completions in a compact form
                .option(LineReader.Option.AUTO_MENU, true) // Show menu automatically
                .option(LineReader.Option.MENU_COMPLETE, true) // Cycle through completions
                .build();

        System.out.println("Type a command and press Tab to see enhanced completion behavior");
        String line = reader.readLine("cmd> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: CompletionBehaviorExample
}
