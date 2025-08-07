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
 * Example demonstrating pre-checked checkbox items.
 */
public class PromptPreCheckedExample {

    // SNIPPET_START: PromptPreCheckedExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Configuration prompt with some options pre-selected
        builder.createCheckboxPrompt()
                .name("config")
                .message("Configure application settings (pre-selected recommended options):")
                .newItem("logging")
                .text("Enable Logging")
                .checked(true)
                .add() // Recommended
                .newItem("metrics")
                .text("Enable Metrics")
                .checked(true)
                .add() // Recommended
                .newItem("debug")
                .text("Debug Mode")
                .checked(false)
                .add() // Not recommended for production
                .newItem("cache")
                .text("Enable Caching")
                .checked(true)
                .add() // Recommended
                .newItem("compression")
                .text("Enable Compression")
                .checked(false)
                .add()
                .newItem("ssl")
                .text("Force SSL")
                .checked(true)
                .add() // Recommended for security
                .newItem("cors")
                .text("Enable CORS")
                .checked(false)
                .add()
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            CheckboxResult result = (CheckboxResult) results.get("config");

            System.out.println("Configuration settings:");
            for (String setting : result.getSelectedIds()) {
                System.out.println("  ✓ " + setting);
            }

            if (result.getSelectedIds().isEmpty()) {
                System.out.println("  No settings enabled");
            }
        } catch (Exception e) {
            System.out.println("Configuration cancelled");
        }
    }
    // SNIPPET_END: PromptPreCheckedExample
}
