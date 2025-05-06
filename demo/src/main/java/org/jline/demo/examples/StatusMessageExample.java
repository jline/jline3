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

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating how to create status messages with AttributedString.
 */
public class StatusMessageExample {

    // SNIPPET_START: StatusMessageExample
    public static AttributedString createStatusMessage(String status) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("Status: ");

        switch (status.toLowerCase()) {
            case "success":
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append("SUCCESS");
                break;
            case "warning":
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                        .append("WARNING");
                break;
            case "error":
                builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                        .append("ERROR");
                break;
            default:
                builder.append(status);
                break;
        }

        return builder.toAttributedString();
    }
    // SNIPPET_END: StatusMessageExample

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Display different status messages
        createStatusMessage("success").println(terminal);
        createStatusMessage("warning").println(terminal);
        createStatusMessage("error").println(terminal);
        createStatusMessage("unknown").println(terminal);

        terminal.close();
    }
}
