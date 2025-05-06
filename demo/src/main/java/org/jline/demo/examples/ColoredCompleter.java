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
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating a colored completer in JLine.
 */
// SNIPPET_START: ColoredCompleter
public class ColoredCompleter implements Completer {
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Command in bold red
        candidates.add(new Candidate(
                "help",
                new AttributedStringBuilder()
                        .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                        .append("help")
                        .toAnsi(),
                null,
                null,
                null,
                null,
                true));

        // File in blue
        candidates.add(new Candidate(
                "file.txt",
                new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                        .append("file.txt")
                        .toAnsi(),
                "A text file",
                null,
                null,
                null,
                true));
    }
}
// SNIPPET_END: ColoredCompleter
