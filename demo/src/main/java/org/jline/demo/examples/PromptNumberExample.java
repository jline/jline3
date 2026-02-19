/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating number prompts with validation.
 */
public class PromptNumberExample {

    // SNIPPET_START: PromptNumberExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Integer input with range validation
        builder.createNumberPrompt()
                .name("age")
                .message("Enter your age:")
                .min(1.0)
                .max(150.0)
                .allowDecimals(false)
                .addPrompt();

        // Decimal input with custom error messages
        builder.createNumberPrompt()
                .name("price")
                .message("Enter price:")
                .min(0.01)
                .max(99999.99)
                .defaultValue("9.99")
                .invalidNumberMessage("Please enter a valid price")
                .outOfRangeMessage("Price must be between $0.01 and $99,999.99")
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            InputResult age = (InputResult) results.get("age");
            InputResult price = (InputResult) results.get("price");

            System.out.println("Age: " + age.getInput());
            System.out.println("Price: $" + price.getInput());
        } catch (Exception e) {
            System.out.println("Input cancelled");
        }
    }
    // SNIPPET_END: PromptNumberExample
}
