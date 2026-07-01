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

            assertEquals(expectedLines, display.oldLines());
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
        int contentRows = 20;
        int cols = 20;
        // On non-xenl terminals (e.g. windows-vtp), writing the bottom-right corner
        // character causes the screen to scroll.  Add an extra terminal row so the
        // content never reaches the bottom-right corner.
        boolean xenl = "xterm".equals(termType);
        int rows = xenl ? contentRows : contentRows + 1;
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

            for (int activeBlockRow = 0; activeBlockRow < contentRows - 3; activeBlockRow++) {
                List<AttributedString> lines = new ArrayList<>();
                for (int y = 0; y < contentRows; y++) {
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

                // Verify the left border is intact on every content row
                long[] screen = terminal.dump();
                StringBuilder failures = new StringBuilder();
                for (int y = 0; y < contentRows; y++) {
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
                    for (int y = 0; y < contentRows; y++) {
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

    // ====================================================================
    // Wrap handling tests: verify CUP is used instead of rawPrint(' ')
    // ====================================================================

    /**
     * On windows-vtp (am but no xenl), when the cursor is at the right margin
     * and the next line starts with non-empty content, the wrapAtEol path should
     * use CUP to move to the next line instead of emitting a visible space.
     * Verifies both: no space+cursor_left in the output AND correct screen state.
     */
    @Test
    void wrapAtEolUsesCupOnWindowsVtp() throws IOException {
        int contentRows = 3;
        int cols = 10;
        // On non-xenl terminals, writing the bottom-right corner scrolls the screen.
        // Add an extra terminal row so the content never hits the bottom-right corner.
        int termRows = contentRows + 1;
        try (VirtualTerminal terminal =
                new VirtualTerminal("test", "windows-vtp", StandardCharsets.UTF_8, cols, termRows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, termRows));

            // Frame 1: fill content rows (no trailing \n = wrap)
            List<AttributedString> frame1 = new ArrayList<>();
            for (int r = 0; r < contentRows; r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < cols; c++) sb.append((char) ('A' + r));
                frame1.add(new AttributedString(sb.toString()));
            }
            display.update(frame1, 0);
            terminal.flush();

            // Frame 2: change row 1 so it triggers a diff after wrap-at-eol from row 0
            terminal.startCapture();
            List<AttributedString> frame2 = new ArrayList<>(frame1);
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) sb.append('X');
            frame2.set(1, new AttributedString(sb.toString()));
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // Verify: output must NOT contain a bare space followed by cursor_left (\e[D)
            assertFalse(
                    output.contains(" \033[D"),
                    "Should use CUP instead of space+cursor_left, but found it in: " + output.replace("\033", "\\e"));

            // Verify screen state: row 0 = AAAAAAAAAA, row 1 = XXXXXXXXXX
            long[] screen = terminal.dump();
            for (int c = 0; c < cols; c++) {
                assertEquals('A', (char) screen[0 * cols + c], "Row 0 col " + c);
                assertEquals('X', (char) screen[1 * cols + c], "Row 1 col " + c);
            }
        }
    }

    /**
     * On xterm (am + xenl), the wrapNeeded path handles delayed wrap.
     * When the next line is empty, the fix should use CUP (since xterm has
     * cursor_address) instead of rawPrint(' ') + cursor_left.
     */
    @Test
    void wrapNeededUsesCupOnXterm() throws IOException {
        int rows = 4;
        int cols = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Frame 1: fill row 0 full-width (no trailing \n = wraps), rows 1-3 empty
            List<AttributedString> frame1 = new ArrayList<>();
            StringBuilder full = new StringBuilder();
            for (int c = 0; c < cols; c++) full.append('A');
            frame1.add(new AttributedString(full.toString())); // wraps
            for (int r = 1; r < rows; r++) {
                frame1.add(new AttributedString(""));
            }
            display.update(frame1, 0);
            terminal.flush();

            // Frame 2: row 0 unchanged (still wraps), row 1 stays empty so the
            // delayed-wrap path must use CUP to move past it, row 2 changes to
            // force output.
            terminal.startCapture();
            List<AttributedString> frame2 = new ArrayList<>(frame1);
            frame2.set(2, new AttributedString("hello"));
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // Row 0 was unchanged so cursor stays at right margin with delayed wrap.
            // wrapNeeded triggers: since row 1 is empty, Display must use CUP
            // (cursor_address) to move to the next line. There should be no
            // space+cursor_left sequence.
            assertFalse(
                    output.contains(" \033[D"),
                    "Should use CUP for delayed wrap over empty line, not space+cursor_left, output: "
                            + output.replace("\033", "\\e"));

            // Verify screen: row 0 = AAAAAAAAAA, row 2 = hello(spaces)
            long[] screen = terminal.dump();
            for (int c = 0; c < cols; c++) {
                assertEquals('A', (char) screen[0 * cols + c], "Row 0 col " + c);
            }
            assertEquals('h', (char) screen[2 * cols + 0]);
            assertEquals('e', (char) screen[2 * cols + 1]);
            assertEquals('l', (char) screen[2 * cols + 2]);
            assertEquals('l', (char) screen[2 * cols + 3]);
            assertEquals('o', (char) screen[2 * cols + 4]);
        }
    }

    // ====================================================================
    // Scroll optimization toggle tests
    // ====================================================================

    /**
     * When scroll optimization is enabled (default), content shifts should produce
     * insert_line/delete_line sequences. When disabled, they should not.
     */
    @Test
    void scrollOptimizationToggle() throws IOException {
        int rows = 6;
        int cols = 20;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Frame 1: rows with numbers
            List<AttributedString> frame1 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                frame1.add(new AttributedString("Line " + (r + 1)));
            }
            display.update(frame1, 0);
            terminal.flush();

            // Frame 2: shift all content down by 1 (insert at top, remove last)
            List<AttributedString> frame2 = new ArrayList<>();
            frame2.add(new AttributedString("Line 0")); // new line at top
            for (int r = 0; r < rows - 1; r++) {
                frame2.add(frame1.get(r)); // shifted down
            }

            // Test with scroll optimization ON (default)
            terminal.startCapture();
            display.update(frame2, 0);
            terminal.flush();
            byte[] capturedOn = terminal.stopCapture();
            String outputOn = new String(capturedOn, StandardCharsets.UTF_8);

            // insert_line for xterm is \e[L (single) or \e[<n>L (parametric)
            assertTrue(
                    outputOn.contains("\033[L") || outputOn.matches("(?s).*\033\\[\\d+L.*"),
                    "Scroll optimization ON should emit insert_line, output: " + outputOn.replace("\033", "\\e"));

            // Reset display and redo with scroll optimization OFF
            display.clear();
            display.update(frame1, 0);
            terminal.flush();

            display.setScrollOptimization(false);
            terminal.startCapture();
            display.update(frame2, 0);
            terminal.flush();
            byte[] capturedOff = terminal.stopCapture();
            String outputOff = new String(capturedOff, StandardCharsets.UTF_8);

            assertFalse(
                    outputOff.contains("\033[L") || outputOff.contains("\033[M"),
                    "Scroll optimization OFF should NOT emit insert/delete line, output: "
                            + outputOff.replace("\033", "\\e"));

            // Both should produce the same screen state
            long[] screen = terminal.dump();
            assertEquals('L', (char) screen[0 * cols + 0]);
            assertEquals('0', (char) screen[0 * cols + 5]);
            assertEquals('L', (char) screen[1 * cols + 0]);
            assertEquals('1', (char) screen[1 * cols + 5]);
        }
    }

    // ====================================================================
    // Synchronized output (mode 2026) tests
    // ====================================================================

    /**
     * Full-screen updates should be wrapped in BSU/ESU (mode 2026) brackets.
     * BSU = \e[?2026h, ESU = \e[?2026l
     */
    @Test
    void syncOutputBracketsFullScreen() throws IOException {
        int rows = 3;
        int cols = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Initial frame
            List<AttributedString> frame1 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                frame1.add(new AttributedString("row" + r));
            }
            display.update(frame1, 0);
            terminal.flush();

            // Second frame — capture its output
            terminal.startCapture();
            List<AttributedString> frame2 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                frame2.add(new AttributedString("ROW" + r));
            }
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // Must start with BSU and end with ESU (possibly followed by flush bytes)
            assertTrue(
                    output.contains("\033[?2026h"),
                    "Full-screen update should contain BSU (\\e[?2026h), output: " + output.replace("\033", "\\e"));
            assertTrue(
                    output.contains("\033[?2026l"),
                    "Full-screen update should contain ESU (\\e[?2026l), output: " + output.replace("\033", "\\e"));

            // BSU must come before ESU
            int bsuPos = output.indexOf("\033[?2026h");
            int esuPos = output.indexOf("\033[?2026l");
            assertTrue(bsuPos < esuPos, "BSU must come before ESU");
        }
    }

    /**
     * Non-full-screen (inline) Display should NOT emit mode 2026 sequences.
     */
    @Test
    void syncOutputNotEmittedForInlineDisplay() throws IOException {
        int rows = 3;
        int cols = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();

            Display display = new Display(terminal, false); // inline mode
            display.resize(Size.of(cols, rows));

            List<AttributedString> frame1 = new ArrayList<>();
            frame1.add(new AttributedString("hello"));
            display.update(frame1, 0);
            terminal.flush();

            terminal.startCapture();
            List<AttributedString> frame2 = new ArrayList<>();
            frame2.add(new AttributedString("world"));
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            assertFalse(
                    output.contains("\033[?2026"),
                    "Inline display should NOT emit mode 2026 sequences, output: " + output.replace("\033", "\\e"));
        }
    }

    // ====================================================================
    // Screen state validation tests for both terminal types
    // ====================================================================

    /**
     * Verify that an unchanged full-width line is NOT re-emitted.
     * The output for the second frame should not contain the unchanged line's content.
     */
    @Test
    void unchangedFullWidthLineNotReEmitted() throws IOException {
        int rows = 3;
        int cols = 15;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            List<AttributedString> frame1 = new ArrayList<>();
            frame1.add(new AttributedString("UNCHANGED_LINE\n"));
            frame1.add(new AttributedString("old_text\n"));
            frame1.add(new AttributedString("footer\n"));
            display.update(frame1, 0);
            terminal.flush();

            terminal.startCapture();
            List<AttributedString> frame2 = new ArrayList<>();
            frame2.add(new AttributedString("UNCHANGED_LINE\n"));
            frame2.add(new AttributedString("new_text\n"));
            frame2.add(new AttributedString("footer\n"));
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // The word "UNCHANGED" should NOT appear in the diff output
            assertFalse(
                    output.contains("UNCHANGED"),
                    "Unchanged line should not be re-emitted, output: " + output.replace("\033", "\\e"));

            // The changed portion should appear in the output.
            // The diff engine may skip a common suffix (e.g. "_text"), but the
            // changed prefix "new" (vs "old") must be emitted.
            assertTrue(
                    output.contains("new"),
                    "Changed content should appear in output, output: " + output.replace("\033", "\\e"));

            // Also verify "UNCHANGED" and "footer" are NOT in the output
            assertFalse(
                    output.contains("footer"),
                    "Unchanged footer should not be re-emitted, output: " + output.replace("\033", "\\e"));

            // Verify screen state
            long[] screen = terminal.dump();
            assertEquals('U', (char) screen[0 * cols + 0]); // row 0 unchanged
            assertEquals('n', (char) screen[1 * cols + 0]); // row 1 updated
            assertEquals('f', (char) screen[2 * cols + 0]); // row 2 unchanged
        }
    }

    /**
     * Verify that windows-vtp (no xenl) correctly renders a full-screen update
     * where every row is full-width (no newline). The ScreenTerminal's immediate
     * wrap behavior should produce correct screen content.
     */
    @Test
    void fullWidthRowsWindowsVtp() throws IOException {
        int contentRows = 3;
        int cols = 5;
        // Extra terminal row to avoid bottom-right corner scroll on non-xenl terminal.
        int termRows = contentRows + 1;
        try (VirtualTerminal terminal =
                new VirtualTerminal("test", "windows-vtp", StandardCharsets.UTF_8, cols, termRows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, termRows));

            // All content rows full-width, no trailing newline
            List<AttributedString> frame = new ArrayList<>();
            frame.add(new AttributedString("AAAAA"));
            frame.add(new AttributedString("BBBBB"));
            frame.add(new AttributedString("CCCCC"));
            display.update(frame, 0);
            terminal.flush();

            long[] screen = terminal.dump();
            for (int c = 0; c < cols; c++) {
                assertEquals('A', (char) screen[0 * cols + c], "Row 0 col " + c);
                assertEquals('B', (char) screen[1 * cols + c], "Row 1 col " + c);
                assertEquals('C', (char) screen[2 * cols + c], "Row 2 col " + c);
            }
        }
    }

    /**
     * When blocks are adjacent (content shift of only 1 line with LCS < 2),
     * the scroll optimization should not trigger even if enabled.
     * This is the natural behavior of the LCS algorithm — verify the screen
     * stays correct.
     */
    @Test
    void singleRowShiftNoScroll() throws IOException {
        int rows = 6;
        int cols = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Frame 1: single unique row at position 2 (1-row "block")
            List<AttributedString> frame1 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                if (r == 2) {
                    frame1.add(new AttributedString("##########"));
                } else {
                    frame1.add(new AttributedString(".........."));
                }
            }
            display.update(frame1, 0);
            terminal.flush();

            // Frame 2: that single row shifted down by 1 to position 3
            List<AttributedString> frame2 = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                if (r == 3) {
                    frame2.add(new AttributedString("##########"));
                } else {
                    frame2.add(new AttributedString(".........."));
                }
            }

            terminal.startCapture();
            display.update(frame2, 0);
            terminal.flush();
            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // Verify screen state is correct
            long[] screen = terminal.dump();
            for (int r = 0; r < rows; r++) {
                char expected = (r == 3) ? '#' : '.';
                assertEquals(expected, (char) screen[r * cols + 0], "Row " + r + " col 0");
                assertEquals(expected, (char) screen[r * cols + 5], "Row " + r + " col 5");
            }

            // With only 1 common line shifted (the '##' row), LCS finds sl=1.
            // The scroll optimization requires sl > 1 (at least 2 common lines),
            // so no insert/delete line should be emitted.
            assertFalse(
                    output.contains("\033[L") || output.contains("\033[M"),
                    "Single-row shift (sl=1) should not trigger scroll, output: " + output.replace("\033", "\\e"));
        }
    }

    /**
     * Verify the scroll optimization getter/setter API.
     */
    @Test
    void scrollOptimizationGetterSetter() throws IOException {
        int rows = 3;
        int cols = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            Display display = new Display(terminal, true);
            // Default should be true
            assertTrue(display.scrollOptimization(), "Default should be enabled");
            display.setScrollOptimization(false);
            assertFalse(display.scrollOptimization(), "Should be disabled after set");
            display.setScrollOptimization(true);
            assertTrue(display.scrollOptimization(), "Should be enabled after re-enable");
        }
    }

    // ====================================================================
    // Resize reflow edge case tests
    // ====================================================================

    @Test
    void resizeEmptyDisplay() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, 80, 10)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(80, 10));

            // Resize without any prior update — oldLines is empty, but columnSplitLength
            // always produces at least one entry (an empty string)
            terminal.resizeScreen(40, 10);
            display.resize(terminal);

            assertTrue(display.oldLines().size() <= 1);
            assertEquals(0, display.cursorPos);
        }
    }

    @Test
    void resizeCursorAtOrigin() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, 80, 10)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(80, 10));

            List<AttributedString> lines = new ArrayList<>();
            lines.add(new AttributedString("hello world\n"));
            lines.add(new AttributedString("second line\n"));
            display.update(lines, 0); // cursor at position 0

            terminal.resizeScreen(40, 10);
            display.resize(terminal);

            assertEquals(0, display.cursorPos);
        }
    }

    @Test
    void resizeAggressiveNarrow() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, 80, 10)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(80, 10));

            // "hello world" is 11 chars, will need multiple lines at width 3
            AttributedString rendered = new AttributedString("hello world\nsecond\n");
            List<AttributedString> oldLines = rendered.columnSplitLength(terminal, 80, true, display.delayLineWrap());
            int cursor = Size.of(80, 10)
                    .cursorPos(
                            oldLines.size() - 1,
                            oldLines.get(oldLines.size() - 1).columnLength(terminal));
            display.update(oldLines, cursor);

            // Resize to 3 columns — content will reflow significantly
            terminal.resizeScreen(3, 10);
            display.resize(terminal);

            List<AttributedString> expected = rendered.columnSplitLength(terminal, 3, true, display.delayLineWrap());
            assertEquals(expected, display.oldLines());
        }
    }

    @Test
    void resizePreservesHardNewlines() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, 40, 10)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(40, 10));

            // Content with multiple hard newlines including empty lines
            AttributedString rendered = new AttributedString("line1\n\n\nline4\n");
            List<AttributedString> lines = rendered.columnSplitLength(terminal, 40, true, display.delayLineWrap());
            int cursor = Size.of(40, 10)
                    .cursorPos(lines.size() - 1, lines.get(lines.size() - 1).columnLength(terminal));
            display.update(lines, cursor);

            terminal.resizeScreen(20, 10);
            display.resize(terminal);

            List<AttributedString> expected = rendered.columnSplitLength(terminal, 20, true, display.delayLineWrap());
            assertEquals(expected, display.oldLines());
        }
    }

    @Test
    void resizeLineExactlyAtColumnWidth() throws IOException {
        int cols = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, 10)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(cols, 10));

            // Exactly 10 chars + newline, then another line
            AttributedString rendered = new AttributedString("1234567890\nabcde\n");
            List<AttributedString> lines = rendered.columnSplitLength(terminal, cols, true, display.delayLineWrap());
            int cursor = Size.of(cols, 10)
                    .cursorPos(lines.size() - 1, lines.get(lines.size() - 1).columnLength(terminal));
            display.update(lines, cursor);

            // Resize to 5 columns — the 10-char line wraps into two 5-char lines
            terminal.resizeScreen(5, 10);
            display.resize(terminal);

            List<AttributedString> expected = rendered.columnSplitLength(terminal, 5, true, display.delayLineWrap());
            assertEquals(expected, display.oldLines());
            int expectedCursor = Size.of(5, 10)
                    .cursorPos(
                            expected.size() - 1,
                            expected.get(expected.size() - 1).columnLength(terminal));
            assertEquals(expectedCursor, display.cursorPos);
        }
    }

    @Test
    void resizeGrowingColumns() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, 20, 10)) {
            Display display = new Display(terminal, false);
            display.resize(Size.of(20, 10));

            // Content that wraps at 20 cols but fits in 40
            AttributedString rendered = new AttributedString("this is a longer line of text\n");
            List<AttributedString> lines = rendered.columnSplitLength(terminal, 20, true, display.delayLineWrap());
            int cursor = Size.of(20, 10)
                    .cursorPos(lines.size() - 1, lines.get(lines.size() - 1).columnLength(terminal));
            display.update(lines, cursor);

            // Grow to 40 columns — content should unwrap
            terminal.resizeScreen(40, 10);
            display.resize(terminal);

            List<AttributedString> expected = rendered.columnSplitLength(terminal, 40, true, display.delayLineWrap());
            assertEquals(expected, display.oldLines());
        }
    }

    // ====================================================================
    // Wide character (CJK) wrap tests
    // ====================================================================

    @Test
    void wideCharacterAtWrapBoundary() throws IOException {
        int cols = 5;
        int rows = 4;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // CJK character (U+4E16 '世') takes 2 columns.
            // At col 4 of a 5-wide terminal, a 2-col char doesn't fit and should wrap.
            // "abcd世e" = a(1) b(1) c(1) d(1) 世(2) e(1) = 7 columns
            List<AttributedString> frame = new ArrayList<>();
            frame.add(new AttributedString("abcd世e\n"));
            frame.add(new AttributedString("test\n"));
            display.update(frame, 0);
            terminal.flush();

            long[] screen = terminal.dump();
            // Row 0: "abcd " (wide char doesn't fit, so padding space or just abcd)
            // The exact behavior depends on columnSplitLength, but the screen
            // should not be corrupted
            assertEquals('a', (char) screen[0 * cols + 0]);
            assertEquals('b', (char) screen[0 * cols + 1]);
            assertEquals('c', (char) screen[0 * cols + 2]);
            assertEquals('d', (char) screen[0 * cols + 3]);
        }
    }

    @Test
    void wideCharacterFullWidthRow() throws IOException {
        int cols = 6;
        int rows = 3;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // 3 CJK chars = 6 columns = exactly full width
            List<AttributedString> frame = new ArrayList<>();
            frame.add(new AttributedString("世界你\n")); // 2+2+2 = 6 cols
            frame.add(new AttributedString("hello\n"));
            display.update(frame, 0);
            terminal.flush();

            long[] screen = terminal.dump();
            assertEquals('h', (char) screen[1 * cols + 0]);
            assertEquals('e', (char) screen[1 * cols + 1]);
        }
    }

    // ====================================================================
    // Attribute/style preservation at wrap boundary tests
    // ====================================================================

    @Test
    void stylePreservedAcrossWrapBoundary() throws IOException {
        int cols = 10;
        int rows = 4;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.puts(enter_ca_mode);

            Display display = new Display(terminal, true);
            display.resize(Size.of(cols, rows));

            // Full-width styled line followed by a styled line
            AttributedStringBuilder sb1 = new AttributedStringBuilder();
            sb1.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            for (int i = 0; i < cols; i++) sb1.append('R');
            // No \n = wraps to next line

            AttributedStringBuilder sb2 = new AttributedStringBuilder();
            sb2.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
            for (int i = 0; i < cols; i++) sb2.append('G');
            sb2.append('\n');

            List<AttributedString> frame1 = new ArrayList<>();
            frame1.add(sb1.toAttributedString());
            frame1.add(sb2.toAttributedString());
            display.update(frame1, 0);
            terminal.flush();

            // Verify screen characters are correct
            long[] screen = terminal.dump();
            for (int c = 0; c < cols; c++) {
                assertEquals('R', (char) screen[0 * cols + c], "Row 0 col " + c);
                assertEquals('G', (char) screen[1 * cols + c], "Row 1 col " + c);
            }

            // Now update: change only the second line's content
            terminal.startCapture();
            AttributedStringBuilder sb3 = new AttributedStringBuilder();
            sb3.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
            for (int i = 0; i < cols; i++) sb3.append('B');
            sb3.append('\n');

            List<AttributedString> frame2 = new ArrayList<>();
            frame2.add(sb1.toAttributedString()); // unchanged
            frame2.add(sb3.toAttributedString()); // changed
            display.update(frame2, 0);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            // First line is unchanged — its styled content should NOT be re-emitted
            assertFalse(
                    output.contains("RRRR"),
                    "Unchanged styled line should not be re-emitted, output: " + output.replace("\033", "\\e"));

            // Verify final screen state
            screen = terminal.dump();
            for (int c = 0; c < cols; c++) {
                assertEquals('R', (char) screen[0 * cols + c], "Row 0 col " + c + " should still be R");
                assertEquals('B', (char) screen[1 * cols + c], "Row 1 col " + c + " should be B");
            }
        }
    }

    // ====================================================================
    // Inline display wrap tests
    // ====================================================================

    @Test
    void inlineDisplayMultipleLines() throws IOException {
        int cols = 10;
        int rows = 5;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();

            Display display = new Display(terminal, false); // inline mode
            display.resize(Size.of(cols, rows));

            List<AttributedString> frame1 = new ArrayList<>();
            frame1.add(new AttributedString("line1\n"));
            frame1.add(new AttributedString("line2\n"));
            display.update(frame1, 0);
            terminal.flush();

            // Update with different content
            List<AttributedString> frame2 = new ArrayList<>();
            frame2.add(new AttributedString("AAAA\n"));
            frame2.add(new AttributedString("BBBB\n"));
            display.update(frame2, 0);
            terminal.flush();

            long[] screen = terminal.dump();
            assertEquals('A', (char) screen[0 * cols + 0]);
            assertEquals('B', (char) screen[1 * cols + 0]);
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

    // ====================================================================
    // Terminal synchronized output API tests
    // ====================================================================

    /**
     * Terminal.beginSynchronizedUpdate/endSynchronizedUpdate should emit
     * BSU/ESU sequences to the terminal output.
     */
    @Test
    void terminalSynchronizedUpdateApi() throws IOException {
        int cols = 40;
        int rows = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.startCapture();

            terminal.beginSynchronizedUpdate();
            terminal.writer().print("Hello");
            terminal.endSynchronizedUpdate();
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            assertTrue(
                    output.contains("\033[?2026h"),
                    "beginSynchronizedUpdate should emit BSU, output: " + output.replace("\033", "\\e"));
            assertTrue(
                    output.contains("\033[?2026l"),
                    "endSynchronizedUpdate should emit ESU, output: " + output.replace("\033", "\\e"));

            int bsuPos = output.indexOf("\033[?2026h");
            int esuPos = output.indexOf("\033[?2026l");
            int helloPos = output.indexOf("Hello");
            assertTrue(bsuPos < helloPos, "BSU should come before content");
            assertTrue(helloPos < esuPos, "Content should come before ESU");
        }
    }

    /**
     * Terminal.synchronizedUpdate(Runnable) should bracket the action in BSU/ESU.
     */
    @Test
    void terminalSynchronizedUpdateLambda() throws IOException {
        int cols = 40;
        int rows = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.startCapture();

            terminal.synchronizedUpdate(() -> {
                terminal.writer().print("Atomic");
            });
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            assertTrue(output.contains("\033[?2026h"), "Lambda wrapper should emit BSU");
            assertTrue(output.contains("\033[?2026l"), "Lambda wrapper should emit ESU");
        }
    }

    /**
     * Terminal.synchronizedUpdate should guarantee ESU is sent even if the action throws.
     */
    @Test
    void terminalSynchronizedUpdateEsuOnException() throws IOException {
        int cols = 40;
        int rows = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();
            terminal.startCapture();

            try {
                terminal.synchronizedUpdate(() -> {
                    terminal.writer().print("partial");
                    throw new RuntimeException("test error");
                });
            } catch (RuntimeException e) {
                assertEquals("test error", e.getMessage());
            }
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            assertTrue(output.contains("\033[?2026h"), "BSU should be emitted");
            assertTrue(output.contains("\033[?2026l"), "ESU should be emitted even after exception");
        }
    }

    // ====================================================================
    // Status bar synchronized output integration tests
    // ====================================================================

    /**
     * Status.update() should wrap its output in BSU/ESU brackets.
     */
    @Test
    void statusUpdateEmitsSyncBrackets() throws IOException {
        int cols = 40;
        int rows = 10;
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, cols, rows)) {
            terminal.enterRawMode();

            Status status = new Status(terminal);

            // First update to initialize
            List<AttributedString> statusLines = new ArrayList<>();
            statusLines.add(new AttributedString("status line 1"));
            status.update(statusLines);
            terminal.flush();

            // Second update — capture its output
            terminal.startCapture();
            List<AttributedString> statusLines2 = new ArrayList<>();
            statusLines2.add(new AttributedString("updated status"));
            status.update(statusLines2);
            terminal.flush();

            byte[] captured = terminal.stopCapture();
            String output = new String(captured, StandardCharsets.UTF_8);

            assertTrue(
                    output.contains("\033[?2026h"),
                    "Status update should contain BSU, output: " + output.replace("\033", "\\e"));
            assertTrue(
                    output.contains("\033[?2026l"),
                    "Status update should contain ESU, output: " + output.replace("\033", "\\e"));

            int bsuPos = output.indexOf("\033[?2026h");
            int esuPos = output.indexOf("\033[?2026l");
            assertTrue(bsuPos < esuPos, "BSU must come before ESU in status update");
        }
    }
}
