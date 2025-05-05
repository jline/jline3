/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.History;
import org.jline.reader.LineReader;

/**
 * Example demonstrating programmatic history access in JLine.
 */
public class ProgrammaticHistoryAccessExample {

    // SNIPPET_START: ProgrammaticHistoryAccessExample
    public void demonstrateHistoryAccess(LineReader reader) {
        // Get the history
        History history = reader.getHistory();

        // Iterate through history entries
        System.out.println("History entries:");
        for (History.Entry entry : history) {
            System.out.println(entry.index() + ": " + entry.line());
        }

        // Get a specific entry
        if (history.size() > 0) {
            String lastCommand = history.get(history.size() - 1);
            System.out.println("Last command: " + lastCommand);
        }

        // Add an entry programmatically
        history.add("manually added command");
        System.out.println("Added command to history");

        // Clear history (commented out to avoid actually clearing history)
        // history.purge();
    }
    // SNIPPET_END: ProgrammaticHistoryAccessExample
}
