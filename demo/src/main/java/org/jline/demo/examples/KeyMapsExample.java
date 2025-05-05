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
import java.util.Map;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating key maps in JLine.
 */
public class KeyMapsExample {

    // SNIPPET_START: KeyMapsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        // Get all key maps
        Map<String, KeyMap<Binding>> keyMaps = reader.getKeyMaps();

        // Display available key maps
        terminal.writer().println("Available key maps:");
        for (String name : keyMaps.keySet()) {
            terminal.writer().println("  " + name);
        }
        terminal.writer().flush();

        // Explain key maps
        terminal.writer().println("\nKey map descriptions:");
        terminal.writer().println("  " + LineReader.MAIN + ": Main key map for normal input mode");
        terminal.writer().println("  " + LineReader.VIINS + ": Vi input mode");
        terminal.writer().println("  " + LineReader.VICMD + ": Vi command mode");
        terminal.writer().println("  " + LineReader.EMACS + ": Emacs mode");
        terminal.writer().println("  " + LineReader.MENU + ": Menu selection mode");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: KeyMapsExample
}
