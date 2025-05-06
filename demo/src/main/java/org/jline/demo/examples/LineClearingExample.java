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
 * Example demonstrating line clearing in JLine.
 */
public class LineClearingExample {

    // SNIPPET_START: LineClearingExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Print some content
        terminal.writer().println("Line 1 - This will remain unchanged");
        terminal.writer().println("Line 2 - This will be cleared and replaced");
        terminal.writer().println("Line 3 - This will remain unchanged");
        terminal.writer().flush();

        // Wait a moment
        TimeUnit.SECONDS.sleep(2);

        // Move cursor to beginning of line 2
        terminal.puts(Capability.cursor_address, 1, 0);

        // Clear the entire line
        terminal.puts(Capability.clr_eol);
        terminal.flush();

        // Print new content on line 2
        terminal.writer().print("Line 2 - This is the new content");
        terminal.writer().flush();

        // Move cursor to a safe position
        terminal.puts(Capability.cursor_address, 3, 0);
        terminal.flush();

        terminal.close();
    }
    // SNIPPET_END: LineClearingExample
}
