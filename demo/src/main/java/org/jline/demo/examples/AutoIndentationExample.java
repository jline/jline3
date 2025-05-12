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
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating auto indentation in JLine.
 */
public class AutoIndentationExample {

    // SNIPPET_START: AutoIndentationExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a parser with EOF on unclosed brackets
        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE);

        // Create a line reader with auto indentation
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ") // Secondary prompt
                .variable(LineReader.INDENTATION, 2) // Indentation size
                .option(LineReader.Option.INSERT_BRACKET, true) // Insert closing bracket automatically
                .build();

        // Display instructions
        terminal.writer().println("Auto Indentation Example");
        terminal.writer().println("------------------------");
        terminal.writer().println("Try typing multi-line code with brackets:");
        terminal.writer().println("Example: if (true) {");
        terminal.writer().println("Note: You need to manually close the last bracket");
        terminal.writer().println("Type 'exit' to quit");
        terminal.writer().println();

        // Read input with auto indentation
        String line;
        while (true) {
            try {
                line = reader.readLine("indent> ");

                if (line.equalsIgnoreCase("exit")) {
                    break;
                }

                terminal.writer().println("You entered: " + line);
                terminal.writer().flush();
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
                terminal.writer().flush();
            }
        }

        terminal.close();
    }
    // SNIPPET_END: AutoIndentationExample
}
