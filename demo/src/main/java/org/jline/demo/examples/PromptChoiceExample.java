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
 * Example demonstrating choice prompts with key-based selection.
 */
public class PromptChoiceExample {

    // SNIPPET_START: PromptChoiceExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Create a choice prompt with key-based selection
        PromptBuilder builder = prompter.newBuilder();
        builder.createChoicePrompt()
                .name("action")
                .message("What would you like to do?")
                .newChoice("create")
                .text("Create new file")
                .key('c')
                .add()
                .newChoice("edit")
                .text("Edit existing file")
                .key('e')
                .defaultChoice(true)
                .add()
                .newChoice("delete")
                .text("Delete file")
                .key('d')
                .add()
                .newChoice("copy")
                .text("Copy file")
                .key('o')
                .add()
                .newChoice("move")
                .text("Move file")
                .key('m')
                .add()
                .newChoice("quit")
                .text("Quit")
                .key('q')
                .add()
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            ChoiceResult result = (ChoiceResult) results.get("action");
            System.out.println("Selected action: " + result.getSelectedId());
        } catch (Exception e) {
            System.out.println("Action cancelled");
        }
    }
    // SNIPPET_END: PromptChoiceExample
}
