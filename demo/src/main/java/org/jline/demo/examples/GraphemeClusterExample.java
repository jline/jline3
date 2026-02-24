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
import java.util.Collections;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.DiffHelper;
import org.jline.utils.Display;

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
 *   <li>Automatic mode management via {@code TerminalBuilder}</li>
 *   <li>Visual comparison with ZWJ emoji sequences</li>
 * </ul>
 * </p>
 */
public class GraphemeClusterExample {

    private static final String FAMILY_EMOJI = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";

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
     * <p>Mode 2027 is automatically enabled by the terminal during
     * construction when the terminal supports it, so no special
     * LineReader configuration is needed.</p>
     */
    public static void lineReaderMode() throws IOException {
        lineReaderMode(true);
    }

    public static void lineReaderMode(boolean graphemeCluster) throws IOException {
        try (Terminal terminal =
                TerminalBuilder.builder().graphemeCluster(graphemeCluster).build()) {
            PrintWriter writer = terminal.writer();

            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            writer.println("Grapheme cluster mode supported: " + terminal.supportsGraphemeClusterMode());
            writer.println("Grapheme cluster mode enabled: " + terminal.getGraphemeClusterMode());
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
                writer.print("Code points: ");
                line.codePoints().forEach(cp -> writer.printf("U+%04X ", cp));
                writer.println();
                terminal.flush();
            }
        }
    }
    // SNIPPET_END: GraphemeClusterLineReader

    /**
     * Raw input diagnostic mode — reads characters directly from the terminal
     * and prints their code points, bypassing LineReader processing.
     */
    public static void rawMode() throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();

            writer.println("Raw input mode — paste or type characters, then press Enter.");
            writer.println("Press Ctrl-D to exit.");
            writer.println();
            terminal.flush();

            Attributes prev = terminal.enterRawMode();
            try {
                StringBuilder sb = new StringBuilder();
                while (true) {
                    int c = terminal.reader().read();
                    if (c == -1 || c == 4) { // EOF or Ctrl-D
                        break;
                    }
                    if (c == '\r' || c == '\n') {
                        // Print analysis of accumulated input
                        terminal.setAttributes(prev);
                        writer.println();
                        if (sb.length() > 0) {
                            String input = sb.toString();
                            writer.println("Input: " + input);
                            writer.println("Length (chars): " + input.length());
                            writer.println("Length (code points): " + input.codePointCount(0, input.length()));
                            writer.print("Code points: ");
                            input.codePoints().forEach(cp -> {
                                if (cp == 0x200D) {
                                    writer.printf("U+200D(ZWJ) ");
                                } else if (cp == 0xFE0F) {
                                    writer.printf("U+FE0F(VS16) ");
                                } else {
                                    writer.printf("U+%04X ", cp);
                                }
                            });
                            writer.println();
                            sb.setLength(0);
                        }
                        writer.println("(paste more or Ctrl-D to exit)");
                        terminal.flush();
                        terminal.enterRawMode();
                        continue;
                    }
                    // Handle surrogate pairs for supplementary code points
                    if (Character.isHighSurrogate((char) c)) {
                        int low = terminal.reader().read();
                        if (Character.isLowSurrogate((char) low)) {
                            sb.appendCodePoint(Character.toCodePoint((char) c, (char) low));
                        }
                    } else {
                        sb.appendCodePoint(c);
                    }
                }
            } finally {
                terminal.setAttributes(prev);
            }
        }
    }

    /**
     * Diagnostic mode — tests the rendering paths to identify where ZWJ
     * combining breaks.
     */
    public static void diagMode() throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();

            writer.println("=== ZWJ Rendering Diagnostic ===");
            writer.println();

            // Test 1: Direct writer.println (bypasses Display/toAnsi)
            writer.println("1. Direct writer.println:");
            writer.println("   [" + FAMILY_EMOJI + "]");
            writer.println();

            // Test 2: AttributedString.print (same path as Display.rawPrint)
            writer.println("2. AttributedString.print (toAnsi path):");
            writer.print("   [");
            new AttributedString(FAMILY_EMOJI).print(terminal);
            writer.println("]");
            writer.println();

            // Test 3: toAnsi() output via writer
            String ansi = new AttributedString(FAMILY_EMOJI).toAnsi(terminal);
            writer.println("3. toAnsi() code points:");
            writer.print("   ");
            ansi.codePoints().forEach(cp -> {
                if (cp == 0x200D) {
                    writer.printf("ZWJ ");
                } else {
                    writer.printf("U+%04X ", cp);
                }
            });
            writer.println();
            writer.println();

            // Test 4: With mode 2027 enabled
            boolean supported = terminal.supportsGraphemeClusterMode();
            writer.println("4. Mode 2027 supported: " + supported);
            if (supported) {
                terminal.setGraphemeClusterMode(true);
                writer.println("   Mode 2027 ON, writer.println:");
                writer.println("   [" + FAMILY_EMOJI + "]");
                writer.println("   Mode 2027 ON, AttributedString.print:");
                writer.print("   [");
                new AttributedString(FAMILY_EMOJI).print(terminal);
                writer.println("]");
                terminal.setGraphemeClusterMode(false);
            }
            writer.println();

            // Test 5: Prompt + emoji via flush (simulates Display pattern)
            writer.println("5. Prompt then emoji with flush between:");
            writer.print("   [prompt> ");
            terminal.flush();
            writer.print(FAMILY_EMOJI);
            terminal.flush();
            writer.println("]");
            writer.println();

            // Test 6: Emoji code points one-by-one with flush between each
            writer.println("6. Code points written one-by-one with flushes:");
            writer.print("   [");
            FAMILY_EMOJI.codePoints().forEach(cp -> {
                writer.print(new String(Character.toChars(cp)));
                terminal.flush();
            });
            writer.println("]");
            writer.println();

            // Test 7: Simulated Display.update() — DiffHelper + rawPrint
            writer.println("7. Simulated Display.update() via DiffHelper.diff:");
            {
                AttributedString oldLine = new AttributedString("emoji> ");
                AttributedStringBuilder sb = new AttributedStringBuilder();
                sb.append("emoji> ");
                sb.append(FAMILY_EMOJI);
                AttributedString newLine = sb.toAttributedString();
                List<DiffHelper.Diff> diffs = DiffHelper.diff(oldLine, newLine);
                writer.print("   Diffs: ");
                for (DiffHelper.Diff diff : diffs) {
                    writer.print(
                            diff.operation + "(len=" + diff.text.length() + ",cols=" + diff.text.columnLength() + ") ");
                }
                writer.println();
                writer.print("   Rendering INSERT parts: [");
                for (DiffHelper.Diff diff : diffs) {
                    if (diff.operation == DiffHelper.Operation.INSERT) {
                        diff.text.print(terminal);
                    }
                }
                writer.println("]");
            }
            writer.println();

            // Test 8: Actual Display.update() with synthetic lines
            writer.println("8. Actual Display.update():");
            {
                Display display = new Display(terminal, false);
                display.resize(terminal.getHeight(), terminal.getWidth());
                // First update: just the prompt
                display.update(Collections.singletonList(new AttributedString("emoji> ")), 7, true);
                writer.println();
                writer.print("   After prompt, updating with emoji: ");
                terminal.flush();
                // Second update: prompt + emoji
                AttributedStringBuilder sb = new AttributedStringBuilder();
                sb.append("emoji> ");
                sb.append(FAMILY_EMOJI);
                display.update(
                        Collections.singletonList(sb.toAttributedString()),
                        7 + new AttributedString(FAMILY_EMOJI).columnLength(),
                        true);
                writer.println();
                // Third update: clear
                display.update(Collections.singletonList(new AttributedString("")), 0, true);
            }
            writer.println();

            // Test 9: columnLength with and without mode 2027
            writer.println("9. columnLength comparison:");
            {
                AttributedString emoji = new AttributedString(FAMILY_EMOJI);
                AttributedString flag = new AttributedString("\uD83C\uDDEB\uD83C\uDDF7");
                writer.println("   Mode 2027 OFF:");
                writer.println("     Family emoji: " + emoji.columnLength(terminal) + " cols");
                writer.println("     Flag:         " + flag.columnLength(terminal) + " cols");
                if (supported) {
                    terminal.setGraphemeClusterMode(true);
                    writer.println("   Mode 2027 ON:");
                    writer.println("     Family emoji: " + emoji.columnLength(terminal) + " cols");
                    writer.println("     Flag:         " + flag.columnLength(terminal) + " cols");
                    terminal.setGraphemeClusterMode(false);
                }
            }
            writer.println();

            terminal.flush();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "manual".equals(args[0])) {
            manualMode();
        } else if (args.length > 0 && "raw".equals(args[0])) {
            rawMode();
        } else if (args.length > 0 && "diag".equals(args[0])) {
            diagMode();
        } else if (args.length > 0 && "nocluster".equals(args[0])) {
            lineReaderMode(false);
        } else {
            lineReaderMode();
        }
    }
}
