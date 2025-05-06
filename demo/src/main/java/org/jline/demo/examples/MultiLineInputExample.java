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
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;

/**
 * Example demonstrating multi-line input in JLine.
 */
public class MultiLineInputExample {

    // SNIPPET_START: MultiLineInputExample
    public String readMultiLineInput(Terminal terminal) {
        // Configure multi-line support
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M> ")
                .build();

        System.out.println("Enter a multi-line input (e.g., with unclosed quotes or brackets):");
        // Read multi-line input
        String multiLine = reader.readLine("multi> ");

        return multiLine;
    }
    // SNIPPET_END: MultiLineInputExample

    public static void main(String[] args) {
        System.out.println("This example demonstrates how to read multi-line input in JLine.");
        System.out.println("See the readMultiLineInput method for details.");
    }
}
