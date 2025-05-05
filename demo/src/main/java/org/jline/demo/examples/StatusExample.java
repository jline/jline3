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
 * Example demonstrating status line in JLine.
 */
public class StatusExample {

    // SNIPPET_START: StatusExample
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Create a Status instance
        Status status = Status.getStatus(terminal);
        if (status != null) {
            // Update the status line
            status.update(Collections.singletonList(new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                    .append("Connected to server | ")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                    .append("3 tasks running")
                    .toAttributedString()));
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
    // SNIPPET_END: StatusExample
}
