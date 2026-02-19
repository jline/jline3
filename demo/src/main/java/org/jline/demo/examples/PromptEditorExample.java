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
 * Example demonstrating editor prompts for multi-line text editing.
 */
public class PromptEditorExample {

    // SNIPPET_START: PromptEditorExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();

        // Basic editor with initial text
        builder.createEditorPrompt()
                .name("notes")
                .message("Edit your notes:")
                .initialText("Enter your notes here...")
                .enableWrapping(true)
                .addPrompt();

        // Editor for code with line numbers
        builder.createEditorPrompt()
                .name("config")
                .message("Edit configuration:")
                .initialText("# Configuration\nkey=value\n")
                .fileExtension("properties")
                .title("Application Config")
                .showLineNumbers(true)
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            EditorResult notes = (EditorResult) results.get("notes");
            EditorResult config = (EditorResult) results.get("config");

            System.out.println("Notes:\n" + notes.getText());
            System.out.println("Config:\n" + config.getText());
        } catch (Exception e) {
            System.out.println("Editing cancelled");
        }
    }
    // SNIPPET_END: PromptEditorExample
}
