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
import java.util.Map;

import org.jline.prompt.*;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating proper error handling for prompts.
 */
public class PromptErrorHandlingExample {

    // SNIPPET_START: PromptErrorHandlingExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        builder.createInputPrompt().name("username").message("Enter username:").addPrompt();

        builder.createConfirmPrompt()
                .name("proceed")
                .message("Continue with operation?")
                .defaultValue(false)
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            InputResult username = (InputResult) results.get("username");
            ConfirmResult proceed = (ConfirmResult) results.get("proceed");

            // Validate input
            if (username.getInput().trim().isEmpty()) {
                System.err.println("Error: Username cannot be empty");
                return;
            }

            if (!proceed.isConfirmed()) {
                System.out.println("Operation cancelled by user");
                return;
            }

            System.out.println("Processing for user: " + username.getInput());

        } catch (UserInterruptException e) {
            // User pressed Ctrl+C
            System.out.println("\nOperation interrupted by user");
            System.exit(1);
        } catch (Exception e) {
            // Handle other exceptions
            System.err.println("An error occurred: " + e.getMessage());

            // Provide fallback or retry logic
            System.out.println("Falling back to default configuration...");

            // Log the error for debugging
            if (Boolean.getBoolean("jline.prompt.debug")) {
                e.printStackTrace();
            }
        }
    }
    // SNIPPET_END: PromptErrorHandlingExample
}
