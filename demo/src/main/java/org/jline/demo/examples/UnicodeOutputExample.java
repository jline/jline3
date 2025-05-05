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

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating Unicode output in JLine.
 */
public class UnicodeOutputExample {

    // SNIPPET_START: UnicodeOutputExample
    public static void main(String[] args) throws IOException {
        // Create a terminal with UTF-8 encoding
        Terminal terminal = TerminalBuilder.builder().encoding("UTF-8").build();

        // Display Unicode characters
        terminal.writer().println("Unicode Output Example");
        terminal.writer().println();

        // Emoji
        terminal.writer().println("Emoji:");
        terminal.writer().println("  😀 😎 🚀 🌍 🎉 🎵 🍕 🏆");

        // Box drawing
        terminal.writer().println("\nBox drawing:");
        terminal.writer().println("  ┌───────────────────┐");
        terminal.writer().println("  │ Unicode characters │");
        terminal.writer().println("  └───────────────────┘");

        // Math symbols
        terminal.writer().println("\nMath symbols:");
        terminal.writer().println("  π ∑ √ ∞ ∫ ≤ ≥ ≠ ∈ ∉ ∩ ∪");

        // International characters
        terminal.writer().println("\nInternational characters:");
        terminal.writer().println("  English: Hello");
        terminal.writer().println("  French: Bonjour");
        terminal.writer().println("  Spanish: Hola");
        terminal.writer().println("  German: Guten Tag");
        terminal.writer().println("  Chinese: 你好");
        terminal.writer().println("  Japanese: こんにちは");
        terminal.writer().println("  Russian: Привет");
        terminal.writer().println("  Arabic: مرحبا");

        // Styled Unicode
        terminal.writer().println("\nStyled Unicode:");
        new AttributedString("  🔴 Red alert!", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                .println(terminal);

        new AttributedString("  🟢 Green light!", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .println(terminal);

        new AttributedString("  🔵 Blue sky!", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                .println(terminal);

        terminal.writer().println("\nPress Enter to exit.");
        terminal.flush();

        // Wait for Enter key
        terminal.reader().read();

        terminal.close();
    }
    // SNIPPET_END: UnicodeOutputExample
}
