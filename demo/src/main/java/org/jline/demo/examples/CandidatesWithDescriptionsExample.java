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
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating candidates with descriptions in JLine.
 */
public class CandidatesWithDescriptionsExample {

    // SNIPPET_START: CandidatesWithDescriptionsExample
    public static void main(String[] args) throws IOException {
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate("help", "help", null, "Display help information", null, null, true));
        candidates.add(new Candidate("exit", "exit", null, "Exit the application", null, null, true));

        Completer completer = (reader, line, completions) -> {
            completions.addAll(candidates);
        };

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .option(LineReader.Option.AUTO_LIST, true)
                .build();

        System.out.println("Type a command and press Tab to see completions with descriptions");
        String line = reader.readLine("desc> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: CandidatesWithDescriptionsExample
}
