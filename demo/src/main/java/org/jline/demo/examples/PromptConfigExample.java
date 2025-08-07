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
import org.jline.prompt.impl.DefaultPrompter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating prompt configuration customization.
 */
public class PromptConfigExample {

    // SNIPPET_START: PromptConfigExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a custom configuration
        PrompterConfig customConfig = PrompterConfig.custom(
                "→", // indicator
                "☐ ", // unchecked box
                "☑ ", // checked box
                "✗ ", // unavailable item
                null, false);
        // Create prompter with custom configuration
        Prompter prompter = new DefaultPrompter(terminal, customConfig);

        PromptBuilder builder = prompter.newBuilder();

        // Demonstrate custom symbols
        builder.createListPrompt()
                .name("theme")
                .message("Choose a theme (notice custom indicator):")
                .newItem("light")
                .text("Light Theme")
                .add()
                .newItem("dark")
                .text("Dark Theme")
                .add()
                .add("custom", "Custom Theme", true) // disabled to show unavailable symbol
                .addPrompt();

        builder.createCheckboxPrompt()
                .name("options")
                .message("Select options (notice custom checkboxes):")
                .newItem("option1")
                .text("Enable notifications")
                .checked(true)
                .add()
                .newItem("option2")
                .text("Auto-save")
                .add()
                .newItem("option3")
                .text("Dark mode")
                .checked(true)
                .add()
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            ListResult themeResult = (ListResult) results.get("theme");
            CheckboxResult optionsResult = (CheckboxResult) results.get("options");

            System.out.println("Selected theme: " + themeResult.getSelectedId());
            System.out.println("Selected options: " + optionsResult.getSelectedIds());
        } catch (Exception e) {
            System.out.println("Configuration cancelled");
        }
    }
    // SNIPPET_END: PromptConfigExample
}
