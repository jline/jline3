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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating editing modes in JLine.
 */
public class EditingModesExample {

    // SNIPPET_START: EditingModesExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader with Emacs editing mode (default)
        LineReader emacsReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.EDITING_MODE, "emacs")
                .build();

        // Create a line reader with Vi editing mode
        LineReader viReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.EDITING_MODE, "vi")
                .build();

        // Demonstrate Emacs mode
        terminal.writer().println("Emacs editing mode:");
        terminal.writer().println("  Ctrl+A: Beginning of line");
        terminal.writer().println("  Ctrl+E: End of line");
        terminal.writer().println("  Ctrl+F: Forward character");
        terminal.writer().println("  Ctrl+B: Backward character");
        terminal.writer().println("  Alt+F: Forward word");
        terminal.writer().println("  Alt+B: Backward word");
        terminal.writer().println("  Ctrl+K: Kill to end of line");
        terminal.writer().println("  Ctrl+Y: Yank (paste)");
        terminal.writer().println("\nType some text in Emacs mode:");
        terminal.writer().flush();

        String line = emacsReader.readLine("emacs> ");
        terminal.writer().println("You entered: " + line);

        // Demonstrate Vi mode
        terminal.writer().println("\nVi editing mode:");
        terminal.writer().println("  ESC: Enter command mode");
        terminal.writer().println("  i: Enter insert mode");
        terminal.writer().println("  h, j, k, l: Move cursor");
        terminal.writer().println("  w: Forward word");
        terminal.writer().println("  b: Backward word");
        terminal.writer().println("  d: Delete");
        terminal.writer().println("  y: Yank (copy)");
        terminal.writer().println("  p: Put (paste)");
        terminal.writer().println("\nType some text in Vi mode:");
        terminal.writer().flush();

        line = viReader.readLine("vi> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: EditingModesExample
}
