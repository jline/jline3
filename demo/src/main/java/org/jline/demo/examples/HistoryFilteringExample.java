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
 * Example demonstrating history filtering in JLine.
 */
public class HistoryFilteringExample {

    // SNIPPET_START: HistoryFilteringExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Don't add duplicate entries
        reader.setOpt(LineReader.Option.HISTORY_IGNORE_DUPS);

        // Don't add entries that start with space
        reader.setOpt(LineReader.Option.HISTORY_IGNORE_SPACE);

        // Beep when trying to navigate past the end of history
        reader.setOpt(LineReader.Option.HISTORY_BEEP);

        // Verify history expansion (like !!, !$, etc.)
        reader.setOpt(LineReader.Option.HISTORY_VERIFY);

        System.out.println("History filtering configured");
    }
    // SNIPPET_END: HistoryFilteringExample
}
