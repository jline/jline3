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
 * Example demonstrating checkbox prompts with pre-checked items.
 */
public class PromptCheckboxExample {

    // SNIPPET_START: PromptCheckboxExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Create a checkbox prompt with some pre-checked items
        PromptBuilder builder = prompter.newBuilder();
        builder.createCheckboxPrompt()
                .name("features")
                .message("Select features to enable:")
                .newItem("logging")
                .text("Logging")
                .checked(true)
                .add() // Pre-checked
                .newItem("caching")
                .text("Caching")
                .add()
                .newItem("monitoring")
                .text("Monitoring")
                .checked(true)
                .add() // Pre-checked
                .newItem("security")
                .text("Security")
                .add()
                .newItem("database")
                .text("Database Connection")
                .add()
                .newItem("messaging")
                .text("Message Queue")
                .add()
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            CheckboxResult result = (CheckboxResult) results.get("features");
            System.out.println("Selected features: " + result.getSelectedIds());
        } catch (Exception e) {
            System.out.println("Selection cancelled");
        }
    }
    // SNIPPET_END: PromptCheckboxExample
}
