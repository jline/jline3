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
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating history expansion in JLine.
 */
public class HistoryExpansionExample {

    // SNIPPET_START: HistoryExpansionExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Enable history expansion
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, false)
                .build();

        System.out.println("History expansion enabled. You can use:");
        System.out.println("!! - repeat the last command");
        System.out.println("!n - repeat command number n");
        System.out.println("!-n - repeat nth previous command");
        System.out.println("!string - repeat last command starting with string");
        System.out.println("!?string - repeat last command containing string");
        System.out.println("^string1^string2 - replace string1 with string2 in the last command");

        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: HistoryExpansionExample
}
