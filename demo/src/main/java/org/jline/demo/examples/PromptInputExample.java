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
 * Example demonstrating input prompts with default values and masking.
 */
public class PromptInputExample {

    // SNIPPET_START: PromptInputExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Text input with default value
        builder.createInputPrompt()
                .name("username")
                .message("Enter your username:")
                .defaultValue("john.doe")
                .addPrompt();

        // Password input with masking
        builder.createInputPrompt()
                .name("password")
                .message("Enter your password:")
                .mask('*')
                .addPrompt();

        // Email input with validation pattern (conceptual)
        builder.createInputPrompt()
                .name("email")
                .message("Enter your email address:")
                .defaultValue("user@example.com")
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            InputResult username = (InputResult) results.get("username");
            InputResult password = (InputResult) results.get("password");
            InputResult email = (InputResult) results.get("email");

            System.out.println("Username: " + username.getInput());
            System.out.println(
                    "Password: " + new String(new char[password.getInput().length()]).replace('\0', '*'));
            System.out.println("Email: " + email.getInput());
        } catch (Exception e) {
            System.out.println("Input cancelled");
        }
    }
    // SNIPPET_END: PromptInputExample
}
