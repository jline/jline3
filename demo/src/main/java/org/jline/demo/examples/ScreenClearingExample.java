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
 * Example demonstrating screen clearing in JLine.
 */
public class ScreenClearingExample {

    // SNIPPET_START: ScreenClearingExample
    public static void main(String[] args) throws IOException, InterruptedException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Print some content
        terminal.writer().println("This is some content that will be cleared.");
        terminal.writer().println("Line 2");
        terminal.writer().println("Line 3");
        terminal.writer().println("Line 4");
        terminal.writer().println("Line 5");
        terminal.writer().flush();

        // Wait a moment
        TimeUnit.SECONDS.sleep(2);

        // Clear the screen
        terminal.puts(Capability.clear_screen);
        terminal.flush();

        // Print new content
        terminal.writer().println("The screen has been cleared!");
        terminal.writer().println("This is new content.");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: ScreenClearingExample
}
