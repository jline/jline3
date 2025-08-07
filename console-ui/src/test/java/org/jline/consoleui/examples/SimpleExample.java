/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

@SuppressWarnings("removal")
public class SimpleExample {

    public static void main(String[] args) {
        List<AttributedString> header = new ArrayList<>();
        header.add(new AttributedStringBuilder().append("Simple list example:").toAttributedString());

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            ConsolePrompt prompt = new ConsolePrompt(terminal);
            PromptBuilder promptBuilder = prompt.getPromptBuilder();

            promptBuilder
                    .createListPrompt()
                    .name("pizzatype")
                    .message("Which pizza do you want?")
                    .newItem()
                    .text("Margherita")
                    .add() // without name (name defaults to text)
                    .newItem("veneziana")
                    .text("Veneziana")
                    .add()
                    .newItem("hawai")
                    .text("Hawai")
                    .add()
                    .newItem("quattro")
                    .text("Quattro Stagioni")
                    .add()
                    .addPrompt();

            Map<String, PromptResultItemIF> result = prompt.prompt(header, promptBuilder.build());
            System.out.println("result = " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
