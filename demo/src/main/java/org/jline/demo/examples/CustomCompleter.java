/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * Example demonstrating a custom completer in JLine.
 */
// SNIPPET_START: CustomCompleter
public class CustomCompleter implements Completer {
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Get the word being completed
        String word = line.word();

        // Add completion candidates based on the current word
        if ("he".startsWith(word)) {
            candidates.add(new Candidate("help", "help", null, "Show help", null, null, true));
        }
        if ("ex".startsWith(word)) {
            candidates.add(new Candidate("exit", "exit", null, "Exit application", null, null, true));
        }

        // You can add more sophisticated logic here
        if ("co".startsWith(word)) {
            candidates.add(new Candidate("connect", "connect", null, "Connect to server", null, null, true));
        }
        if ("di".startsWith(word)) {
            candidates.add(new Candidate("disconnect", "disconnect", null, "Disconnect from server", null, null, true));
        }
    }
}
// SNIPPET_END: CustomCompleter
