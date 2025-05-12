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
import java.util.regex.Pattern;

import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the RegexCompleter.
 */
public class RegexCompleterExample {

    // SNIPPET_START: RegexCompleterExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a pattern-based completer that simulates regex completion
        // Format: command [--option] <file>
        // Where command can be: cat, ls, or grep
        Completer commandCompleter = new StringsCompleter("cat", "ls", "grep");
        Completer optionCompleter = new StringsCompleter("--color", "--help", "--version");
        Completer fileCompleter = new FileNameCompleter();

        // Create a custom completer that uses patterns to determine completion
        Completer patternCompleter = new Completer() {
            private final Pattern commandPattern = Pattern.compile("^(cat|ls|grep)$");
            private final Pattern optionPattern = Pattern.compile("^(cat|ls|grep)\\s+(--[a-z]+)$");
            private final Pattern filePattern = Pattern.compile("^(cat|ls|grep)\\s+(--[a-z]+\\s+)?(.*)$");

            @Override
            public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                String buffer = line.line();

                if (buffer.trim().isEmpty() || commandPattern.matcher(buffer).matches()) {
                    // Complete command
                    commandCompleter.complete(reader, line, candidates);
                } else if (optionPattern.matcher(buffer).matches()) {
                    // Complete option
                    optionCompleter.complete(reader, line, candidates);
                } else if (filePattern.matcher(buffer).matches()) {
                    // Complete file
                    fileCompleter.complete(reader, line, candidates);
                }
            }
        };

        // Create a line reader with the pattern completer
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(patternCompleter)
                .build();

        // Read input with pattern-based completion
        String line = reader.readLine("Enter a command: ");
        System.out.println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: RegexCompleterExample
}
