/*
 * Copyright (c) the original author(s).
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
 * Example demonstrating key press prompts for single key capture.
 */
public class PromptKeyPressExample {

    // SNIPPET_START: PromptKeyPressExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Key press with default hint
        builder.createKeyPressPrompt()
                .name("continue")
                .message("Ready to proceed.")
                .addPrompt();

        // Key press with custom hint
        builder.createKeyPressPrompt()
                .name("action")
                .message("Choose action:")
                .hint("Press 's' to save, 'q' to quit, or any key to continue...")
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            KeyPressResult continueKey = (KeyPressResult) results.get("continue");
            KeyPressResult actionKey = (KeyPressResult) results.get("action");

            System.out.println("First key pressed: " + continueKey.getKey());
            System.out.println("Action key pressed: " + actionKey.getKey());
        } catch (Exception e) {
            System.out.println("Cancelled");
        }
    }
    // SNIPPET_END: PromptKeyPressExample
}
