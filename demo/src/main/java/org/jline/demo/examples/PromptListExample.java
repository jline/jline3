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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

/**
 * Example demonstrating list prompts with multi-column layout.
 */
public class PromptListExample {

    // SNIPPET_START: PromptListExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Create a list prompt with multiple items for multi-column layout
        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("language")
                .message("Choose your favorite programming language:")
                .newItem("java")
                .text("Java")
                .add()
                .newItem("python")
                .text("Python")
                .add()
                .newItem("javascript")
                .text("JavaScript")
                .add()
                .newItem("typescript")
                .text("TypeScript")
                .add()
                .newItem("go")
                .text("Go")
                .add()
                .newItem("rust")
                .text("Rust")
                .add()
                .newItem("kotlin")
                .text("Kotlin")
                .add()
                .newItem("scala")
                .text("Scala")
                .add()
                .addPrompt();

        try {
            // Add a header for better presentation
            List<AttributedString> header = Arrays.asList(new AttributedString("Programming Language Selection"));

            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

            ListResult result = (ListResult) results.get("language");
            System.out.println("Selected language: " + result.getSelectedId());
        } catch (Exception e) {
            System.out.println("Selection cancelled");
        }
    }
    // SNIPPET_END: PromptListExample
}
