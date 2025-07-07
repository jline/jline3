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
 * Example demonstrating confirm prompts with default values.
 */
public class PromptConfirmExample {

    // SNIPPET_START: PromptConfirmExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Confirmation with default "yes"
        builder.createConfirmPrompt()
                .name("save")
                .message("Save changes before exit?")
                .defaultValue(true)
                .addPrompt();

        // Confirmation with default "no"
        builder.createConfirmPrompt()
                .name("delete")
                .message("Delete all temporary files?")
                .defaultValue(false)
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            ConfirmResult saveResult = (ConfirmResult) results.get("save");
            ConfirmResult deleteResult = (ConfirmResult) results.get("delete");

            if (saveResult.isConfirmed()) {
                System.out.println("Changes saved!");
            }

            if (deleteResult.isConfirmed()) {
                System.out.println("Temporary files deleted!");
            } else {
                System.out.println("Temporary files preserved.");
            }
        } catch (Exception e) {
            System.out.println("Operation cancelled");
        }
    }
    // SNIPPET_END: PromptConfirmExample
}
