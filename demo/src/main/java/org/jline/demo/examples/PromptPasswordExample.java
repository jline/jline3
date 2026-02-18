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
 * Example demonstrating password prompts with masking options.
 */
public class PromptPasswordExample {

    // SNIPPET_START: PromptPasswordExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Password with default mask character '*'
        builder.createPasswordPrompt()
                .name("password")
                .message("Enter your password:")
                .addPrompt();

        // Password with custom mask character
        builder.createPasswordPrompt()
                .name("secret")
                .message("Enter API secret:")
                .mask('\u2022') // bullet character
                .addPrompt();

        // Password with no visible mask (hidden input)
        builder.createPasswordPrompt()
                .name("token")
                .message("Enter access token:")
                .showMask(false)
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            InputResult password = (InputResult) results.get("password");
            InputResult secret = (InputResult) results.get("secret");
            InputResult token = (InputResult) results.get("token");

            System.out.println("Password length: " + password.getInput().length());
            System.out.println("Secret length: " + secret.getInput().length());
            System.out.println("Token length: " + token.getInput().length());
        } catch (Exception e) {
            System.out.println("Input cancelled");
        }
    }
    // SNIPPET_END: PromptPasswordExample
}
