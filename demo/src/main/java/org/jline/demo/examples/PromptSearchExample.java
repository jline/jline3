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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jline.prompt.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating search prompts with dynamic filtering.
 */
public class PromptSearchExample {

    // SNIPPET_START: PromptSearchExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Prompter prompter = PrompterFactory.create(terminal);

        // Sample data to search through
        List<String> countries = Arrays.asList(
                "Argentina",
                "Australia",
                "Austria",
                "Belgium",
                "Brazil",
                "Canada",
                "Chile",
                "China",
                "Denmark",
                "Egypt",
                "Finland",
                "France",
                "Germany",
                "Greece",
                "India",
                "Italy",
                "Japan",
                "Mexico",
                "Norway",
                "Spain");

        PromptBuilder builder = prompter.newBuilder();

        builder.<String>createSearchPrompt()
                .name("country")
                .message("Search for a country:")
                .searchFunction(query -> countries.stream()
                        .filter(c -> c.toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toList()))
                .displayFunction(c -> c)
                .valueFunction(c -> c.toLowerCase())
                .placeholder("Type to search countries...")
                .minSearchLength(1)
                .maxResults(5)
                .addPrompt();

        try {
            Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(null, builder.build());

            SearchResult<?> country = (SearchResult<?>) results.get("country");
            System.out.println("Selected: " + country.getSelectedValue());
        } catch (Exception e) {
            System.out.println("Search cancelled");
        }
    }
    // SNIPPET_END: PromptSearchExample
}
