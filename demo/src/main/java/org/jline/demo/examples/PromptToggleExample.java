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
 * Example demonstrating toggle prompts for binary choices.
 */
public class PromptToggleExample {

    // SNIPPET_START: PromptToggleExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Toggle with default labels (Yes/No)
        builder.createTogglePrompt()
                .name("notifications")
                .message("Enable notifications?")
                .defaultValue(true)
                .addPrompt();

        // Toggle with custom labels
        builder.createTogglePrompt()
                .name("theme")
                .message("Select theme:")
                .activeLabel("Dark")
                .inactiveLabel("Light")
                .defaultValue(false)
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            ToggleResult notifications = (ToggleResult) results.get("notifications");
            ToggleResult theme = (ToggleResult) results.get("theme");

            System.out.println("Notifications: " + (notifications.isActive() ? "Enabled" : "Disabled"));
            System.out.println("Theme: " + theme.getDisplayResult());
        } catch (Exception e) {
            System.out.println("Selection cancelled");
        }
    }
    // SNIPPET_END: PromptToggleExample
}
