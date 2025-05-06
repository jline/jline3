/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.ArrayList;
import java.util.List;

import org.jline.reader.History;
import org.jline.reader.LineReader;

/**
 * Example demonstrating history search in JLine.
 */
public class HistorySearchExample {

    // SNIPPET_START: HistorySearchExample
    public List<String> searchHistory(LineReader reader, String term) {
        List<String> results = new ArrayList<>();
        History history = reader.getHistory();

        for (History.Entry entry : history) {
            if (entry.line().contains(term)) {
                results.add(entry.line());
            }
        }

        return results;
    }
    // SNIPPET_END: HistorySearchExample

    public void demonstrateHistorySearch(LineReader reader) {
        System.out.println("Searching history for 'git':");
        List<String> gitCommands = searchHistory(reader, "git");

        for (String command : gitCommands) {
            System.out.println(" - " + command);
        }
    }
}
