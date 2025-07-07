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
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating basic Prompt module usage.
 */
public class PromptBasicExample {

    // SNIPPET_START: PromptBasicExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a prompter using the factory
        Prompter prompter = PrompterFactory.create(terminal);

        // Get a PromptBuilder to create prompts
        PromptBuilder builder = prompter.newBuilder();

        // Create a simple list prompt
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
                .addPrompt();

        try {
            // Display the prompt and get the result
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            ListResult result = (ListResult) results.get("framework");
            System.out.println("You chose: " + result.getSelectedId());
        } catch (Exception e) {
            System.out.println("Prompt cancelled");
        }
    }
    // SNIPPET_END: PromptBasicExample
}
