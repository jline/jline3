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
 * Example demonstrating Unicode input in JLine.
 */
public class UnicodeInputExample {

    // SNIPPET_START: UnicodeInputExample
    public static void main(String[] args) throws IOException {
        // Create a terminal with UTF-8 encoding
        Terminal terminal = TerminalBuilder.builder().encoding("UTF-8").build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Display instructions
        terminal.writer().println("Unicode Input Example");
        terminal.writer().println("Try typing Unicode characters like:");
        terminal.writer().println("  • Emoji: 😀 🚀 🌍 🎉");
        terminal.writer().println("  • Math symbols: π ∑ √ ∞");
        terminal.writer().println("  • International characters: é ñ 你好 こんにちは");
        terminal.writer().println();

        // Read input with Unicode support
        String line = reader.readLine("unicode> ");

        // Display the input
        terminal.writer().println("You entered: " + line);

        // Display character information
        terminal.writer().println("\nCharacter information:");
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            terminal.writer().printf("  Position %d: '%c' (Unicode: U+%04X)%n", i, c, (int) c);
        }

        terminal.close();
    }
    // SNIPPET_END: UnicodeInputExample
}
