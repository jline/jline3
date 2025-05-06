/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.util.Collections;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;

/**
 * Example demonstrating multi-segment status line in JLine.
 */
public class MultiSegmentStatusExample {

    // SNIPPET_START: MultiSegmentStatusExample
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create a Status instance
        Status status = Status.getStatus(terminal);

        if (status != null) {
            // Create a multi-segment status line
            AttributedStringBuilder asb = new AttributedStringBuilder();

            // Left-aligned segment
            asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)).append("Server: Connected");

            // Center segment (with padding)
            int width = terminal.getWidth();
            int leftLen = "Server: Connected".length();
            int rightLen = "Users: 42".length();
            int padding = (width - leftLen - rightLen) / 2;
            for (int i = 0; i < padding; i++) {
                asb.append(" ");
            }

            // Right-aligned segment
            asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append("Users: 42");

            status.update(Collections.singletonList(asb.toAttributedString()));
        }

        // Read input normally
        while (true) {
            String line = reader.readLine("prompt> ");
            System.out.println("You entered: " + line);

            if (line.equals("exit")) {
                break;
            }
        }
    }
    // SNIPPET_END: MultiSegmentStatusExample
}
