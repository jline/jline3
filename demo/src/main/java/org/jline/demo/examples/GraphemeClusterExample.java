/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating mode 2027 (grapheme cluster) support.
 *
 * <p>
 * Mode 2027 enables UAX #29 grapheme cluster segmentation in the terminal,
 * which allows multi-codepoint characters like ZWJ emoji sequences (e.g.,
 * family emoji 👨‍👩‍👧‍👦) to be treated as single display units instead of
 * multiple separate characters.
 * </p>
 *
 * <p>
 * Without mode 2027, the terminal uses per-codepoint {@code wcwidth()} for
 * cursor positioning, which causes misalignment when editing lines containing
 * ZWJ emoji or flag sequences.
 * </p>
 *
 * <p>
 * This example shows:
 * <ul>
 *   <li>Probing terminal support for mode 2027</li>
 *   <li>Enabling/disabling the mode manually</li>
 *   <li>Using {@code LineReader Option.GRAPHEME_CLUSTER} for automatic management</li>
 *   <li>Visual comparison with ZWJ emoji sequences</li>
 * </ul>
 * </p>
 */
public class GraphemeClusterExample {

    // SNIPPET_START: GraphemeClusterManual
    /**
     * Demonstrates manual grapheme cluster mode management.
     */
    public static void manualMode() throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();

            // Check if the terminal supports mode 2027
            boolean supported = terminal.supportsGraphemeClusterMode();
            writer.println("Grapheme cluster mode supported: " + supported);

            if (supported) {
                // Enable mode 2027
                terminal.setGraphemeClusterMode(true);
                writer.println("Mode 2027 enabled: " + terminal.getGraphemeClusterMode());

                writer.println();
                writer.println("ZWJ emoji sequences (should display as single glyphs):");
                writer.println("  Family:       👨\u200D👩\u200D👧\u200D👦  (4 people joined with ZWJ)");
                writer.println("  Couple:       👩\u200D❤\uFE0F\u200D👨  (heart couple)");
                writer.println("  Profession:   👩\u200D🔬  (woman scientist)");
                writer.println("  Skin tone:    👋🏽  (waving hand, medium skin)");
                writer.println("  Flag:         🇫🇷  (regional indicators F+R)");
                writer.println();
                writer.println("Cursor should align correctly after these characters.");

                // Mode is automatically disabled when the terminal is closed
            } else {
                writer.println("This terminal does not support mode 2027.");
                writer.println("ZWJ emoji may cause cursor misalignment during line editing.");
            }
            terminal.flush();
        }
    }
    // SNIPPET_END: GraphemeClusterManual

    // SNIPPET_START: GraphemeClusterLineReader
    /**
     * Demonstrates automatic grapheme cluster mode with LineReader.
     *
     * <p>When {@code Option.GRAPHEME_CLUSTER} is enabled, the LineReader
     * automatically enables mode 2027 at the start of {@code readLine()}
     * and disables it when editing is done.</p>
     */
    public static void lineReaderMode() throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .option(LineReader.Option.GRAPHEME_CLUSTER, true)
                    .build();

            writer.println("Grapheme cluster mode supported: " + terminal.supportsGraphemeClusterMode());
            writer.println();
            writer.println("Try typing or pasting ZWJ emoji sequences:");
            writer.println("  👨\u200D👩\u200D👧\u200D👦  👩\u200D🔬  🏳\u200D🌈  👋🏽  🇫🇷");
            writer.println();
            writer.println("The cursor should track correctly as you edit.");
            writer.println("Type 'quit' to exit.");
            writer.println();
            terminal.flush();

            String line;
            while (true) {
                line = reader.readLine("emoji> ");
                if ("quit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                writer.println("You typed: " + line);
                writer.println("Length (chars): " + line.length());
                writer.println("Length (code points): " + line.codePointCount(0, line.length()));
                terminal.flush();
            }
        }
    }
    // SNIPPET_END: GraphemeClusterLineReader

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "manual".equals(args[0])) {
            manualMode();
        } else {
            lineReaderMode();
        }
    }
}
