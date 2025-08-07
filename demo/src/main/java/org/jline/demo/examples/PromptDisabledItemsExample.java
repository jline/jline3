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
 * Example demonstrating disabled items with custom disabled text.
 */
public class PromptDisabledItemsExample {

    // SNIPPET_START: PromptDisabledItemsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // List with some disabled items
        builder.createListPrompt()
                .name("subscription")
                .message("Choose your subscription plan:")
                .newItem("free")
                .text("Free Plan")
                .add()
                .newItem("basic")
                .text("Basic Plan - $9.99/month")
                .add()
                .newItem("premium")
                .text("Premium Plan - $19.99/month")
                .add()
                .add("enterprise", "Enterprise Plan - Contact Sales", true) // disabled
                .addPrompt();

        // Checkbox with disabled items
        builder.createCheckboxPrompt()
                .name("features")
                .message("Select additional features:")
                .newItem("backup")
                .text("Daily Backup")
                .add()
                .newItem("ssl")
                .text("SSL Certificate")
                .add()
                .add("priority", "Priority Support", false, true) // not checked, disabled
                .add("custom", "Custom Domain", false, true) // not checked, disabled
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            ListResult planResult = (ListResult) results.get("subscription");
            CheckboxResult featuresResult = (CheckboxResult) results.get("features");

            System.out.println("Selected plan: " + planResult.getSelectedId());
            System.out.println("Selected features: " + featuresResult.getSelectedIds());
        } catch (Exception e) {
            System.out.println("Selection cancelled");
        }
    }
    // SNIPPET_END: PromptDisabledItemsExample
}
