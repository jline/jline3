/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.LineReader;

/**
 * Example demonstrating how to read input with LineReader.
 */
public class LineReaderInputExample {
    // SNIPPET_START: LineReaderInputExample
    public void demonstrateInput(LineReader reader) {
        // Read a line with a prompt
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);

        // HIGHLIGHT_START: Read a line with a right prompt
        // Read a line with a right prompt (displayed at the right edge)
        String lineWithRightPrompt = reader.readLine("prompt> ", "right prompt", (Character) null, null);
        System.out.println("You entered: " + lineWithRightPrompt);
        // HIGHLIGHT_END

        // Read a masked line (for passwords)
        String password = reader.readLine("Password: ", '*');
        System.out.println("Password accepted");
    }
    // SNIPPET_END: LineReaderInputExample
}
