/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;

/**
 * Example demonstrating LineReader options.
 */
public class LineReaderOptionsExample {

    // SNIPPET_START: LineReaderOptionsExample
    public LineReader configureOptions(Terminal terminal) {
        // Configure options during creation
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.CASE_INSENSITIVE, true)
                .option(LineReader.Option.AUTO_REMOVE_SLASH, true)
                .build();

        // Or set options after creation
        reader.setOpt(LineReader.Option.HISTORY_IGNORE_DUPS);
        reader.unsetOpt(LineReader.Option.HISTORY_BEEP);

        return reader;
    }
    // SNIPPET_END: LineReaderOptionsExample

    public static void main(String[] args) {
        System.out.println("This example demonstrates how to configure LineReader options.");
        System.out.println("See the configureOptions method for details.");
    }
}
