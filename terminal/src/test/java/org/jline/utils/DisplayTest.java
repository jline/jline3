/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.jline.utils.InfoCmp.Capability.enter_ca_mode;
import static org.jline.utils.InfoCmp.Capability.exit_ca_mode;
import static org.junit.jupiter.api.Assertions.*;

class DisplayTest {

    @Test
    void i737() throws IOException {
        int rows = 10;
        int cols = 25;
        try (VirtualTerminal terminal = new VirtualTerminal("jline", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            Attributes savedAttributes = terminal.enterRawMode();
            terminal.puts(enter_ca_mode);
            int height = terminal.getRows();

            Display display = new Display(terminal, true);
            display.resize(terminal);

            // Build Strings to displayed
            List<AttributedString> lines1 = new ArrayList<>();
            for (int i = 1; i < height + 1; i++) {
                lines1.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            List<AttributedString> lines2 = new ArrayList<>();
            for (int i = 0; i < height; i++) {
                lines2.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            display.update(lines1, 0);

            display.update(lines2, 0);

            long[] screen = terminal.dump();
            List<AttributedString> lines = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                for (int i = 0; i < cols; i++) {
                    sb.append((char) screen[i + cols * r]);
                }
                lines.add(sb.toAttributedString());
            }
            assertEquals("009: Chaine de test...   ", lines.get(rows - 1).toString());

            terminal.setAttributes(savedAttributes);
            terminal.puts(exit_ca_mode);
        }
    }

    @Test
    void resizeReflowsCursorWithRenderedLines() throws IOException {
        int rows = 12;
        int oldCols = 80;
        int newCols = 49;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, oldCols, rows)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(oldCols, rows));

            String command = "./validation/manual/jline-readline-state-diagnostics.scala";
            AttributedString rendered = new AttributedString("-- user --\n" + command + "\n> " + command);
            List<AttributedString> oldLines =
                    rendered.columnSplitLength(terminal, oldCols, true, display.delayLineWrap());
            int oldCursor = Size.of(oldCols, rows)
                    .cursorPos(
                            oldLines.size() - 1,
                            oldLines.get(oldLines.size() - 1).columnLength(terminal));
            display.update(oldLines, oldCursor);

            List<AttributedString> expectedLines =
                    rendered.columnSplitLength(terminal, newCols, true, display.delayLineWrap());
            int expectedCursor = Size.of(newCols, rows)
                    .cursorPos(
                            expectedLines.size() - 1,
                            expectedLines.get(expectedLines.size() - 1).columnLength(terminal));

            terminal.resizeScreen(newCols, rows);
            display.resize(terminal);

            assertEquals(expectedLines, display.oldLines);
            assertEquals(expectedCursor, display.cursorPos);
        }
    }

    @Test
    void testIntraLineSkipOptimization() throws IOException {
        int rows = 3;
        int cols = 40;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Frame 1: all rows filled with 'a' in default style
            List<AttributedString> frame1 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                for (int c = 0; c < cols; c++) sb.append('a');
                sb.append('\n');
                frame1.add(sb.toAttributedString());
            }
            display.update(frame1, 0);
            terminal.flush();

            // Start capturing output for the second update
            terminal.startCapture();

            // Frame 2: row 1 has red 'X' at col 5 and col 33
            // (27 unchanged 'a' chars between them — well above the skip threshold)
            List<AttributedString> frame2 = new ArrayList<>(frame1);
            AttributedStringBuilder sb = new AttributedStringBuilder();
            for (int c = 0; c < cols; c++) {
                if (c == 5 || c == 33) {
                    sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                    sb.append('X');
                    sb.style(AttributedStyle.DEFAULT);
                } else {
                    sb.append('a');
                }
            }
            sb.append('\n');
            frame2.set(1, sb.toAttributedString());
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // Verify optimization: no long run of 'a' chars in the output
            // (the 27 unchanged chars should be skipped with cursor movement)
            int maxConsecutiveA = 0;
            int run = 0;
            for (int i = 0; i < output.length(); i++) {
                if (output.charAt(i) == 'a') {
                    run++;
                    if (run > maxConsecutiveA) maxConsecutiveA = run;
                } else {
                    run = 0;
                }
            }
            assertTrue(
                    maxConsecutiveA < 10,
                    "Expected cursor movement to skip unchanged gap, but found "
                            + maxConsecutiveA + " consecutive 'a' chars in output: "
                            + output.replace("\u001b", "\\e"));

            // Verify screen correctness
            long[] screen = terminal.dump();
            assertEquals('X', (char) screen[5 + cols * 1], "col 5 row 1 should be X");
            assertEquals('X', (char) screen[33 + cols * 1], "col 33 row 1 should be X");
            assertEquals('a', (char) screen[10 + cols * 1], "col 10 row 1 should be unchanged");
            assertEquals('a', (char) screen[0 + cols * 1], "col 0 row 1 should be unchanged");
            assertEquals('a', (char) screen[39 + cols * 1], "col 39 row 1 should be unchanged");
        }
    }

    @Test
    void testUpdateImmutableList() throws IOException {
        int rows = 5;
        int cols = 40;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));
            List<AttributedString> lines = new ArrayList<>();
            lines.add(new AttributedString("test1"));
            lines.add(new AttributedString("test2"));
            lines.add(new AttributedString("test3"));

            assertDoesNotThrow(() -> display.update(List.copyOf(lines), 0));
            lines.set(2, new AttributedString("3test"));
            assertDoesNotThrow(() -> display.update(List.copyOf(lines), 0));
            lines.add(new AttributedString("test4"));
            assertDoesNotThrow(() -> display.update(List.copyOf(lines), 0));
            display.clear();
            lines.set(0, new AttributedString("non empty line"));
            assertDoesNotThrow(() -> display.update(List.copyOf(lines), 0));
        }
    }

    @Test
    void fallingBlockPreservesLeftBorderXterm() throws IOException {
        fallingBlockPreservesLeftBorder("xterm");
    }

    @Test
    void fallingBlockPreservesLeftBorderWindowsVtp() throws IOException {
        fallingBlockPreservesLeftBorder("windows-vtp");
    }

    void fallingBlockPreservesLeftBorder(String termType) throws IOException {
        int rows = 20;
        int cols = 20;
        try (VirtualTerminal terminal = new VirtualTerminal("test", termType, StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            AttributedStyle borderStyle = AttributedStyle.DEFAULT.foreground(96, 96, 96);
            AttributedStyle greenStyle = AttributedStyle.DEFAULT.foreground(0, 205, 0);
            AttributedStyle orangeStyle = AttributedStyle.DEFAULT.foreground(205, 102, 0);

            // Green block pattern (S-shape tetromino):
            // row 0: .GG
            // row 1: GG.
            // row 2: ...
            int[][] greenBlock = {{-1, 1, 1}, {1, 1, -1}, {-1, -1, -1}};

            // Orange block pattern:
            // row 0: ..O
            // row 1: OOO
            // row 2: ...
            int[][] orangeBlock = {{-1, -1, 2}, {2, 2, 2}, {-1, -1, -1}};

            int orangeCol = 10;
            int orangeRow = 10;

            for (int activeBlockRow = 0; activeBlockRow < rows - 3; activeBlockRow++) {
                List<AttributedString> lines = new ArrayList<>();
                for (int y = 0; y < rows; y++) {
                    AttributedStringBuilder sb = new AttributedStringBuilder(cols);
                    for (int x = 0; x < cols; x++) {
                        // Check green block
                        int blockType = -1;
                        int gy = y - activeBlockRow;
                        int gx = x - 4; // xPos = 4
                        if (gy >= 0 && gy < 3 && gx >= 0 && gx < 3) {
                            blockType = greenBlock[gy][gx];
                        }
                        // Check orange block
                        if (blockType < 0) {
                            int oy = y - orangeRow;
                            int ox = x - orangeCol;
                            if (oy >= 0 && oy < 3 && ox >= 0 && ox < 3) {
                                blockType = orangeBlock[oy][ox];
                            }
                        }
                        // Draw
                        if (x == 0) {
                            sb.style(borderStyle);
                            sb.append('█'); // █
                        } else if (blockType == 1) {
                            sb.style(greenStyle);
                            sb.append('█');
                        } else if (blockType == 2) {
                            sb.style(orangeStyle);
                            sb.append('█');
                        } else {
                            sb.style(AttributedStyle.DEFAULT);
                            sb.append(' ');
                        }
                    }
                    lines.add(sb.toAttributedString());
                }
                display.update(lines, 0);
                terminal.flush();

                // Verify the left border is intact on every row
                long[] screen = terminal.dump();
                StringBuilder failures = new StringBuilder();
                for (int y = 0; y < rows; y++) {
                    char ch = (char) screen[y * cols]; // column 0 of each row
                    if (ch != '█') {
                        failures.append("Row ")
                                .append(y)
                                .append(": '")
                                .append(ch)
                                .append("' ");
                    }
                }
                if (failures.length() > 0) {
                    // Dump full screen for debugging
                    StringBuilder screenDump = new StringBuilder();
                    screenDump.append("\nFrame ").append(activeBlockRow).append(" screen:\n");
                    for (int y = 0; y < rows; y++) {
                        for (int x = 0; x < cols; x++) {
                            screenDump.append((char) screen[y * cols + x]);
                        }
                        screenDump.append("|\n");
                    }
                    fail("Left border corruption at frame " + activeBlockRow + ": " + failures + screenDump);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Attributes savedAttributes = terminal.enterRawMode();
            terminal.puts(enter_ca_mode);
            int height = terminal.getRows();

            Display display = new Display(terminal, true);
            display.resize(terminal);

            // Build Strings to displayed
            List<AttributedString> lines1 = new ArrayList<>();
            for (int i = 1; i < height + 1; i++) {
                lines1.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            List<AttributedString> lines2 = new ArrayList<>();
            for (int i = 0; i < height; i++) {
                lines2.add(new AttributedString(String.format("%03d: %s", i, "Chaine de test...")));
            }

            // Display with tempo
            display.update(lines1, 0);
            Thread.sleep(3000);

            display.update(lines2, 0);
            Thread.sleep(3000);

            terminal.setAttributes(savedAttributes);
            terminal.puts(exit_ca_mode);
        }
    }
}
