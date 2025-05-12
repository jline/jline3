/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating custom prompts in JLine.
 */
public class CustomPromptExample {

    // SNIPPET_START: CustomPromptExample
    public static void main(String[] args) {
        try {
            // Create a terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create a line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Set variables for prompt customization
            reader.setVariable("prompt", "%N@%M:%w%n> ");
            reader.setVariable("rprompt", "%T");
            reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "%M> ");

            // Custom prompt with colors
            AttributedString prompt =
                    new AttributedString("jline> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

            // Custom right prompt with colors
            AttributedString rightPrompt =
                    new AttributedString("[demo]", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));

            // Read lines with different prompt styles
            try {
                // Using pattern-based prompt
                String line1 = reader.readLine();
                terminal.writer().println("You entered: " + line1);

                // Using AttributedString prompt (convert to String)
                String line2 = reader.readLine(prompt.toAnsi(), rightPrompt.toAnsi(), (Character) null, null);
                terminal.writer().println("You entered: " + line2);

                // Using a simple string prompt
                String line3 = reader.readLine("simple> ");
                terminal.writer().println("You entered: " + line3);

                // Using a masked prompt for passwords
                String password = reader.readLine("Password: ", '*');
                terminal.writer().println("Password length: " + password.length());

            } catch (UserInterruptException e) {
                // Ctrl+C
                terminal.writer().println("Interrupted");
            } catch (EndOfFileException e) {
                // Ctrl+D
                terminal.writer().println("EOF");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: CustomPromptExample
}
