/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.examples;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the PromptCommands usage.
 * This shows how to use the prompt command from scripts or REPL consoles.
 */
public class PromptCommandExample {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PromptCommandExample() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void main(String[] args) {
        try {
            // Create terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create context
            PromptCommands.Context context = new PromptCommands.Context(
                    System.in,
                    System.out,
                    System.err,
                    Paths.get(System.getProperty("user.dir")),
                    terminal,
                    name -> System.getProperty(name));

            System.out.println("=== PromptCommands Example ===\n");

            // Example 1: List prompt
            System.out.println("1. List Prompt Example:");
            try {
                PromptCommands.prompt(
                        context,
                        new String[] {"list", "-m", "Choose your favorite color:", "Red", "Green", "Blue", "Yellow"});
            } catch (Exception e) {
                System.err.println("List prompt cancelled or failed");
            }

            System.out.println();

            // Example 2: Checkbox prompt
            System.out.println("2. Checkbox Prompt Example:");
            try {
                PromptCommands.prompt(context, new String[] {
                    "checkbox",
                    "-m",
                    "Select programming languages you know:",
                    "Java",
                    "Python",
                    "JavaScript",
                    "C++",
                    "Go"
                });
            } catch (Exception e) {
                System.err.println("Checkbox prompt cancelled or failed");
            }

            System.out.println();

            // Example 3: Choice prompt
            System.out.println("3. Choice Prompt Example:");
            try {
                PromptCommands.prompt(context, new String[] {
                    "choice", "-m", "Select difficulty level:", "-k", "emh", "Easy", "Medium", "Hard"
                });
            } catch (Exception e) {
                System.err.println("Choice prompt cancelled or failed");
            }

            System.out.println();

            // Example 4: Input prompt
            System.out.println("4. Input Prompt Example:");
            try {
                PromptCommands.prompt(context, new String[] {"input", "-m", "Enter your name:", "-d", "John Doe"});
            } catch (Exception e) {
                System.err.println("Input prompt cancelled or failed");
            }

            System.out.println();

            // Example 5: Confirm prompt
            System.out.println("5. Confirm Prompt Example:");
            try {
                PromptCommands.prompt(context, new String[] {"confirm", "-m", "Do you want to continue?", "-d", "y"});
            } catch (Exception e) {
                System.err.println("Confirm prompt cancelled or failed");
            }

            System.out.println("\n=== Example Complete ===");

            // Bonus: Demonstrate PromptBuilder API usage
            demonstratePromptBuilderAPI(terminal);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Demonstrate using the PromptBuilder API directly (alternative to command-line usage).
     */
    private static void demonstratePromptBuilderAPI(Terminal terminal) {
        try {
            System.out.println("\n=== PromptBuilder API Demo ===");

            Prompter prompter = PrompterFactory.create(terminal);

            // Build multiple prompts using the fluent API
            PromptBuilder builder = prompter.newBuilder();

            builder.createListPrompt()
                    .name("framework")
                    .message("Choose a web framework:")
                    .newItem("spring")
                    .text("Spring Boot")
                    .add()
                    .newItem("quarkus")
                    .text("Quarkus")
                    .add()
                    .newItem("micronaut")
                    .text("Micronaut")
                    .add()
                    .addPrompt()
                    .createCheckboxPrompt()
                    .name("databases")
                    .message("Select databases to support:")
                    .newItem("postgres")
                    .text("PostgreSQL")
                    .checked(true)
                    .add()
                    .newItem("mysql")
                    .text("MySQL")
                    .add()
                    .newItem("mongodb")
                    .text("MongoDB")
                    .add()
                    .addPrompt()
                    .createConfirmPrompt()
                    .name("proceed")
                    .message("Generate project with these settings?")
                    .defaultValue(true)
                    .addPrompt();

            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            System.out.println("Framework: " + results.get("framework").getDisplayResult());
            System.out.println("Databases: " + results.get("databases").getDisplayResult());
            System.out.println("Proceed: " + results.get("proceed").getDisplayResult());

        } catch (Exception e) {
            System.err.println("PromptBuilder demo cancelled or failed");
        }
    }
}
