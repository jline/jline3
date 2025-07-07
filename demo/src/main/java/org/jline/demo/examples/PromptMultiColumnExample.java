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
 * Example demonstrating multi-column layout with many items.
 */
public class PromptMultiColumnExample {

    // SNIPPET_START: PromptMultiColumnExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Create a list with many items to demonstrate multi-column layout
        PromptBuilder builder = prompter.newBuilder();
        ListBuilder listBuilder = builder.createListPrompt().name("country").message("Select your country:");

        // Add many countries to demonstrate column layout
        String[] countries = {
            "United States", "Canada", "United Kingdom", "Germany", "France",
            "Italy", "Spain", "Netherlands", "Belgium", "Switzerland",
            "Austria", "Sweden", "Norway", "Denmark", "Finland",
            "Australia", "New Zealand", "Japan", "South Korea", "Singapore",
            "Brazil", "Argentina", "Mexico", "India", "China"
        };

        for (int i = 0; i < countries.length; i++) {
            listBuilder.newItem("country" + i).text(countries[i]).add();
        }

        listBuilder.addPrompt();

        try {
            // Add informative header
            List<AttributedString> header = Arrays.asList(
                    new AttributedString("Country Selection"),
                    new AttributedString("Use arrow keys to navigate (left/right for columns, up/down for rows)"));

            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

            ListResult result = (ListResult) results.get("country");
            System.out.println("Selected country: "
                    + countries[Integer.parseInt(result.getSelectedId().substring(7))]);
        } catch (Exception e) {
            System.out.println("Selection cancelled");
        }
    }
    // SNIPPET_END: PromptMultiColumnExample
}
