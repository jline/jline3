/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jline.prompt.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating the new Prompter API.
 */
public class NewApiExample {

    public static void main(String[] args) {
        List<AttributedString> header = new ArrayList<>();
        addInHeader(header, AttributedStyle.DEFAULT.italic().foreground(2), "Hello New API World!");
        addInHeader(header, "This is a demonstration of the new Prompter API. It provides a simple console interface");
        addInHeader(
                header, "for querying information from the user. Prompter is inspired by Inquirer.js which is written");
        addInHeader(header, "in JavaScript.");

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            // Create a Prompter instance
            Prompter prompter = PrompterFactory.create(terminal);

            // Get a prompt builder
            PromptBuilder promptBuilder = prompter.newBuilder();

            // Add a list prompt
            promptBuilder
                    .createListPrompt()
                    .name("color")
                    .message("What is your favorite color?")
                    .newItem("red")
                    .text("Red")
                    .add()
                    .newItem("green")
                    .text("Green")
                    .add()
                    .newItem("blue")
                    .text("Blue")
                    .add()
                    .addPrompt();

            // Add a checkbox prompt
            promptBuilder
                    .createCheckboxPrompt()
                    .name("features")
                    .message("Select features:")
                    .newItem("feature1")
                    .text("Feature 1")
                    .add()
                    .newItem("feature2")
                    .text("Feature 2")
                    .checked(true)
                    .add()
                    .newItem("feature3")
                    .text("Feature 3")
                    .add()
                    .addPrompt();

            // Add a confirm prompt
            promptBuilder
                    .createConfirmPrompt()
                    .name("confirm")
                    .message("Are you sure?")
                    .defaultValue(true)
                    .addPrompt();

            // Prompt the user
            try {
                Map<String, ? extends PromptResult<? extends Prompt>> result =
                        prompter.prompt(header, promptBuilder.build());

                // Access the results
                ListResult colorResult = (ListResult) result.get("color");
                System.out.println("Favorite color: " + colorResult.getSelectedId());

                CheckboxResult featuresResult = (CheckboxResult) result.get("features");
                System.out.println("Selected features: " + featuresResult.getSelectedIds());

                ConfirmResult confirmResult = (ConfirmResult) result.get("confirm");
                System.out.println("Confirmed: " + confirmResult.isConfirmed());

                // Access the associated prompt
                ListPrompt selectedPrompt = colorResult.getPrompt();
                if (selectedPrompt != null) {
                    System.out.println(
                            "Prompt items: " + selectedPrompt.getItems().size());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addInHeader(List<AttributedString> header, String text) {
        header.add(new AttributedString(text));
    }

    private static void addInHeader(List<AttributedString> header, AttributedStyle style, String text) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.styled(style, text);
        header.add(asb.toAttributedString());
    }
}
