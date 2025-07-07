/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.examples;

import java.io.IOException;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the convenience methods for adding items to prompts.
 * Shows the difference between the verbose builder pattern and the convenient add() methods.
 */
public class ConvenienceMethodsExample {

    public static void main(String[] args) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Prompter prompter = PrompterFactory.create(terminal);

            try {
                // Example 1: List prompt with convenience methods
                demonstrateListConvenienceMethods(prompter);

                // Example 2: Checkbox prompt with convenience methods
                demonstrateCheckboxConvenienceMethods(prompter);

                // Example 3: Choice prompt with convenience methods
                demonstrateChoiceConvenienceMethods(prompter);

            } catch (UserInterruptException e) {
                // Operation cancelled by user
            }
        }
    }

    private static void demonstrateListConvenienceMethods(Prompter prompter)
            throws IOException, UserInterruptException {
        // Example: List prompt with convenience methods
        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("language")
                .message("Choose a programming language:")
                .add("java", "Java")
                .add("python", "Python")
                .add("javascript", "JavaScript")
                .add("go", "Go", true) // disabled
                .addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());
        // Results available in: results.get("language").getDisplayResult()
    }

    private static void demonstrateCheckboxConvenienceMethods(Prompter prompter)
            throws IOException, UserInterruptException {
        // Example: Checkbox prompt with convenience methods
        PromptBuilder builder = prompter.newBuilder();
        builder.createCheckboxPrompt()
                .name("features")
                .message("Select features to enable:")
                .add("logging", "Logging", true) // checked
                .add("caching", "Caching")
                .add("monitoring", "Monitoring", true) // checked
                .addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());
        // Results available in: results.get("features").getDisplayResult()
    }

    private static void demonstrateChoiceConvenienceMethods(Prompter prompter)
            throws IOException, UserInterruptException {
        // Example: Choice prompt with convenience methods
        PromptBuilder builder = prompter.newBuilder();
        builder.createChoicePrompt()
                .name("action")
                .message("What would you like to do?")
                .add("create", "Create new file", 'c')
                .add("edit", "Edit existing file", 'e', true) // default
                .add("delete", "Delete file", 'd')
                .addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());
        // Results available in: results.get("action").getDisplayResult()
    }
}
