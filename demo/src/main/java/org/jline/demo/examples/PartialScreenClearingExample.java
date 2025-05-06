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
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

/**
 * Example demonstrating partial screen clearing in JLine.
 */
public class PartialScreenClearingExample {

    // SNIPPET_START: PartialScreenClearingExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Print some content
        terminal.writer().println("Line 1 - This will remain");
        terminal.writer().println("Line 2 - This will remain");
        terminal.writer().println("Line 3 - This will be cleared");
        terminal.writer().println("Line 4 - This will be cleared");
        terminal.writer().println("Line 5 - This will be cleared");
        terminal.writer().flush();

        // Wait a moment
        TimeUnit.SECONDS.sleep(2);

        // Move cursor to line 3
        terminal.puts(Capability.cursor_address, 2, 0);

        // Clear from cursor to end of screen
        terminal.puts(Capability.clr_eos);
        terminal.flush();

        // Print new content
        terminal.writer().println("New Line 3 - After partial clearing");
        terminal.writer().println("New Line 4");
        terminal.writer().println("New Line 5");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: PartialScreenClearingExample
}
