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
import org.jline.consoleui.prompt.builder.CheckboxPromptBuilder;
import org.jline.consoleui.prompt.builder.ListPromptBuilder;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

@SuppressWarnings("removal")
public class LongList {

    public static void main(String[] args) {
        List<AttributedString> header = new ArrayList<>();
        header.add(new AttributedStringBuilder()
                .append("This is a demonstration of ConsoleUI java library. It provides a simple console interface")
                .toAttributedString());
        header.add(new AttributedStringBuilder()
                .append("for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written")
                .toAttributedString());
        header.add(new AttributedStringBuilder().append("in JavaScript.").toAttributedString());

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            ConsolePrompt.UiConfig config = new ConsolePrompt.UiConfig(">", "( )", "(x)", "( )");
            ConsolePrompt prompt = new ConsolePrompt(terminal, config);
            PromptBuilder promptBuilder = prompt.getPromptBuilder();

            ListPromptBuilder listPrompt = promptBuilder.createListPrompt();
            listPrompt.name("longlist").message("What's your favourite Letter?").relativePageSize(66);

            for (char letter = 'A'; letter <= 'C'; letter++)
                for (char letter2 = 'A'; letter2 <= 'Z'; letter2++)
                    listPrompt.newItem().text("" + letter + letter2).add();
            listPrompt.addPrompt();

            CheckboxPromptBuilder checkboxPrompt = promptBuilder.createCheckboxPrompt();
            checkboxPrompt
                    .name("longcheckbox")
                    .message("What's your favourite Letter? Select all you want...")
                    .relativePageSize(66);

            for (char letter = 'A'; letter <= 'C'; letter++)
                for (char letter2 = 'A'; letter2 <= 'Z'; letter2++)
                    checkboxPrompt.newItem().text("" + letter + letter2).add();
            checkboxPrompt.addPrompt();

            Map<String, PromptResultItemIF> result = prompt.prompt(header, promptBuilder.build());
            System.out.println("result = " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
