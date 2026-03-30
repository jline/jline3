/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ScreenTerminal} class.
 */
public class ScreenTerminalTest {

    // Helper: get cursor position as [x, y]
    private int[] getCursor(ScreenTerminal terminal) {
        int w = terminal.getWidth();
        int h = terminal.getHeight();
        long[] screen = new long[w * h];
        int[] cursor = new int[2];
        terminal.dump(screen, cursor);
        return cursor;
    }

    // Helper: get the character at a screen position from the raw dump
    private int getChar(ScreenTerminal terminal, int row, int col) {
        int w = terminal.getWidth();
        int h = terminal.getHeight();
        long[] screen = new long[w * h];
        terminal.dump(screen, null);
        return (int) (screen[row * w + col] & 0xffffffffL);
    }

    // Helper: get the attribute at a screen position from the raw dump
    private long getAttr(ScreenTerminal terminal, int row, int col) {
        int w = terminal.getWidth();
        int h = terminal.getHeight();
        long[] screen = new long[w * h];
        terminal.dump(screen, null);
        return screen[row * w + col] >>> 32;
    }

    // -----------------------------------------------------------------------
    // Bug fix tests
    // -----------------------------------------------------------------------

    /**
     * fill() with a single cell (x0 == x1-1) should not be skipped.
     * Regression: csi_EL("1") at column 0 was a no-op because fill()
     * used {@code x0 < x1 - 1} instead of {@code x0 < x1}.
     */
    @Test
    public void testFillSingleCell() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        terminal.write("A");
        // Move cursor back to column 0
        terminal.write("\033[1G");
        // EL 1 = erase from beginning of line to cursor (inclusive)
        terminal.write("\033[1K");

        assertEquals(' ', getChar(terminal, 0, 0));
    }

    /**
     * fill() must not overwrite the first row before x0.
     * Regression: the middle-row loop started at y0, clobbering the
     * partial fill of the first row.
     */
    @Test
    public void testFillPreservesPartialFirstRow() {
        ScreenTerminal terminal = new ScreenTerminal(10, 4);
        // Fill first row with 'A'
        terminal.write("AAAAAAAAAA");
        // Move to (row 0, col 5) and erase from cursor to end of display
        terminal.write("\033[1;6H"); // CUP row=1 col=6 (1-based)
        terminal.write("\033[0J"); // ED 0 = erase from cursor to end

        // Columns 0-4 should still be 'A'
        for (int x = 0; x < 5; x++) {
            assertEquals('A', getChar(terminal, 0, x), "Column " + x + " should be preserved");
        }
        // Columns 5-9 should be spaces
        for (int x = 5; x < 10; x++) {
            assertEquals(' ', getChar(terminal, 0, x), "Column " + x + " should be erased");
        }
    }

    /**
     * CHA (cursor horizontal absolute) with a value beyond terminal width
     * must clamp to width-1, not cause ArrayIndexOutOfBoundsException.
     */
    @Test
    public void testCursorClampedToWidth() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // CHA 999 = move cursor to column 999 (1-based)
        terminal.write("\033[999G");

        int[] cursor = getCursor(terminal);
        assertEquals(79, cursor[0], "Cursor X should be clamped to width-1");

        // Writing a character should not throw
        terminal.write("X");
    }

    /**
     * Backspace at column 0 must not move the cursor to a negative position.
     * Regression: (cx-1)/width produced -1 with Java's remainder operator.
     */
    @Test
    public void testBackspaceAtColumnZero() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Cursor starts at (0,0); send backspace
        terminal.write("\010");

        int[] cursor = getCursor(terminal);
        assertEquals(0, cursor[0], "Cursor X should stay at 0");
        assertEquals(0, cursor[1], "Cursor Y should stay at 0");
    }

    /**
     * scroll_area_up with n > 1 must fill all new rows with spaces.
     * Regression: only the last new row was filled (y1-1 instead of y1-i).
     */
    @Test
    public void testScrollAreaUpMultipleLines() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        // Fill all rows
        for (int i = 0; i < 5; i++) {
            terminal.write("XXXXXXXXXX");
            if (i < 4) terminal.write("\n");
        }
        // Scroll up 3 lines via CSI 3S
        terminal.write("\033[3S");

        // The bottom 3 rows (2,3,4) should be filled with spaces, not null
        String content = terminal.toString();
        String[] lines = content.split("\n", -1);
        for (int row = 2; row < 5; row++) {
            for (int x = 0; x < 10; x++) {
                assertEquals(' ', lines[row].charAt(x), "Row " + row + " col " + x + " should be space");
            }
        }
    }

    /**
     * setSize() width shrink must truncate rows to the new width.
     * Regression: only width increases were handled (length < w guard).
     */
    @Test
    public void testSetSizeWidthShrink() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        terminal.write("Hello, World!");
        terminal.setSize(40, 24);

        // Should not throw on any subsequent operation
        String content = terminal.toString();
        assertNotNull(content);

        // Write at the new width boundary
        terminal.write("\033[1;40H"); // Move to column 40
        terminal.write("X");

        int[] cursor = getCursor(terminal);
        assertTrue(cursor[0] < 40, "Cursor should be within new width");
    }

    /**
     * DECRC must clamp restored cursor to current terminal dimensions.
     * Regression: cursor was restored without bounds checking after resize.
     */
    @Test
    public void testDECRCClampsAfterResize() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Move cursor to (row 20, col 70), 1-based
        terminal.write("\033[21;71H");
        // DECSC - save cursor
        terminal.write("\0337");

        // Shrink terminal
        terminal.setSize(40, 12);

        // DECRC - restore cursor
        terminal.write("\0338");

        int[] cursor = getCursor(terminal);
        assertEquals(39, cursor[0], "Restored cx should be clamped to new width-1");
        assertEquals(11, cursor[1], "Restored cy should be clamped to new height-1");
    }

    /**
     * CSI u (RCP) must clamp restored cursor to current terminal dimensions.
     * Same class of bug as DECRC, but via the SCP/RCP mechanism.
     */
    @Test
    public void testCSI_RCP_ClampsAfterResize() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Move cursor to (row 20, col 70)
        terminal.write("\033[21;71H");
        // CSI s - save cursor position
        terminal.write("\033[s");

        // Shrink terminal
        terminal.setSize(40, 12);

        // CSI u - restore cursor position
        terminal.write("\033[u");

        int[] cursor = getCursor(terminal);
        assertEquals(39, cursor[0], "Restored cx should be clamped to new width-1");
        assertEquals(11, cursor[1], "Restored cy should be clamped to new height-1");
    }

    // -----------------------------------------------------------------------
    // Feature tests
    // -----------------------------------------------------------------------

    /**
     * True-color SGR sequences (38;2;r;g;b and 48;2;r;g;b) must be parsed
     * and reduce 8-bit channels to the 4-bit encoding.
     */
    @Test
    public void testTrueColorForeground() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Set foreground to RGB(255, 128, 0) = orange
        terminal.write("\033[38;2;255;128;0mX\033[0m");

        // 255>>4=15, 128>>4=8, 0>>4=0 → expanded to #ff8800
        String dump = terminal.dump(0, true);
        assertTrue(dump.contains("color:#ff8800;"), "Foreground should be #ff8800, got: " + dump);
    }

    @Test
    public void testTrueColorBackground() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Set background to RGB(0, 255, 128)
        terminal.write("\033[48;2;0;255;128mX\033[0m");

        // 0>>4=0, 255>>4=15, 128>>4=8 → expanded to #00ff88
        String dump = terminal.dump(0, true);
        assertTrue(dump.contains("background-color:#00ff88;"), "Background should be #00ff88, got: " + dump);
    }

    /**
     * SGR 2 (dim/faint) must halve the foreground RGB values instead of
     * using opacity.
     */
    @Test
    public void testDimAttribute() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Set dim mode — default foreground is white (#ffffff = 0xfff in 12-bit)
        terminal.write("\033[2mX\033[0m");

        String dump = terminal.dump(0, true);
        // White (#fff) halved: each 4-bit component 0xf>>1=7 → #777777
        assertTrue(dump.contains("color:#777777;"), "Dim foreground should be halved, got: " + dump);
        assertFalse(dump.contains("opacity"), "Should not use opacity for dim");
    }

    /**
     * SGR 3 (italic) must render as font-style:italic.
     */
    @Test
    public void testItalicAttribute() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        terminal.write("\033[3mX\033[0m");

        String dump = terminal.dump(0, true);
        assertTrue(dump.contains("font-style:italic;"), "Should contain italic style");
    }

    /**
     * SGR 22 (normal intensity) must reset both bold and dim per ECMA-48.
     */
    @Test
    public void testSGR22ResetsNormalIntensity() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Set bold + dim, then reset with SGR 22, then write
        terminal.write("\033[1;2m\033[22mX\033[0m");

        String dump = terminal.dump(0, true);
        assertFalse(dump.contains("font-weight:bold;"), "Bold should be cleared by SGR 22");
        // Foreground should be full white (not halved), since dim was cleared
        assertTrue(dump.contains("color:#ffffff;"), "Dim should be cleared by SGR 22");
    }

    /**
     * SGR 23 must reset italic without affecting other attributes.
     */
    @Test
    public void testSGR23ResetsItalicOnly() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Set bold + italic, then reset italic only
        terminal.write("\033[1;3m\033[23mX\033[0m");

        String dump = terminal.dump(0, true);
        assertTrue(dump.contains("font-weight:bold;"), "Bold should be preserved");
        assertFalse(dump.contains("font-style:italic;"), "Italic should be cleared by SGR 23");
    }

    /**
     * CSI 3J must clear the scrollback buffer.
     */
    @Test
    public void testCSI3JClearsScrollback() throws InterruptedException {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        // Write enough lines to push content into scrollback
        for (int i = 0; i < 10; i++) {
            terminal.write("line " + i + "\n");
        }

        // Clear scrollback
        terminal.write("\033[3J");

        // Shrink terminal to try pulling from scrollback
        terminal.setSize(10, 3);
        // Grow terminal back — if scrollback is empty, new lines should be blank
        terminal.setSize(10, 8);

        String content = terminal.toString();
        String[] lines = content.split("\n", -1);
        // The top lines (pulled from history) should be spaces if history was cleared
        // With empty scrollback, growing height adds blank lines at the bottom, not top
        // Just verify the content is consistent and no crash
        assertNotNull(content);
        assertFalse(content.contains("line 0"), "Scrollback content should have been cleared");
    }

    // -----------------------------------------------------------------------
    // pipe() tests
    // -----------------------------------------------------------------------

    /**
     * pipe() must produce identical output for arrow keys regardless of
     * cursor key mode, except for the prefix (\033O vs \033[).
     * Regression guard for the switch deduplication refactoring.
     */
    @Test
    public void testPipeArrowKeysNormalMode() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Normal mode (default): arrows use CSI (\033[)
        String result = terminal.pipe("~A~B~C~D~F~H");
        assertEquals("\u001b[A\u001b[B\u001b[C\u001b[D\u001b[F\u001b[H", result);
    }

    @Test
    public void testPipeArrowKeysCursorKeyMode() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Enable cursor key mode via DECCKM
        terminal.write("\033[?1h");
        String result = terminal.pipe("~A~B~C~D~F~H");
        assertEquals("\u001bOA\u001bOB\u001bOC\u001bOD\u001bOF\u001bOH", result);
    }

    @Test
    public void testPipeFunctionKeys() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // Function keys use the same sequences in both modes
        String result = terminal.pipe("~1~2~3~4~a~b~c~d~e~f~g~h~i~j~k~l");
        assertEquals(
                "\u001b[5~\u001b[6~\u001b[2~\u001b[3~"
                        + "\u001bOP\u001bOQ\u001bOR\u001bOS"
                        + "\u001b[15~\u001b[17~\u001b[18~\u001b[19~\u001b[20~\u001b[21~\u001b[23~\u001b[24~",
                result);
    }

    @Test
    public void testPipeTildeEscape() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);
        // ~~ should produce a single ~
        String result = terminal.pipe("~~");
        assertEquals("~", result);
    }

    // -----------------------------------------------------------------------
    // Alt-screen tests
    // -----------------------------------------------------------------------

    /**
     * Switching to alt-screen must clear it, and switching back must
     * restore the main screen content.
     */
    @Test
    public void testAltScreenSwitchPreservesMainScreen() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        // Write content on main screen
        terminal.write("MAIN");

        // Switch to alt-screen (CSI ?1049h)
        terminal.write("\033[?1049h");

        // Alt-screen should be clear
        assertEquals(' ', getChar(terminal, 0, 0), "Alt-screen should be cleared on entry");

        // Write something on alt-screen
        terminal.write("ALT");

        // Switch back to main screen (CSI ?1049l)
        terminal.write("\033[?1049l");

        // Main screen content should be preserved
        assertEquals('M', getChar(terminal, 0, 0), "Main screen should be restored");
        assertEquals('A', getChar(terminal, 0, 1));
        assertEquals('I', getChar(terminal, 0, 2));
        assertEquals('N', getChar(terminal, 0, 3));
    }

    /**
     * Resizing while on the alt-screen must not corrupt the main screen.
     */
    @Test
    public void testResizeOnAltScreen() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        terminal.write("MAIN");

        // Switch to alt-screen
        terminal.write("\033[?1049h");
        terminal.write("ALT");

        // Resize while on alt-screen
        terminal.setSize(20, 10);

        // Switch back
        terminal.write("\033[?1049l");

        // Main screen content should still be there
        assertEquals('M', getChar(terminal, 0, 0));
        assertEquals('A', getChar(terminal, 0, 1));
        assertEquals('I', getChar(terminal, 0, 2));
        assertEquals('N', getChar(terminal, 0, 3));
    }

    // -----------------------------------------------------------------------
    // scroll_line_right / scroll_line_left tests (ICH / DCH)
    // -----------------------------------------------------------------------

    /**
     * CSI n @ (ICH - Insert Character) triggers scroll_line_right.
     * Inserting blanks at a mid-line cursor position must shift existing
     * characters to the right.
     */
    @Test
    public void testICHInsertCharacterShiftsRight() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        // Write "ABCDEFGHIJ" filling the entire first row
        terminal.write("ABCDEFGHIJ");
        // Move cursor to column 3 (1-based col 4)
        terminal.write("\033[1;4H");
        // Insert 2 blank characters at cursor position (CSI 2 @)
        terminal.write("\033[2@");

        // Expected row: A B C _ _ D E F G H (J dropped off the right)
        assertEquals('A', getChar(terminal, 0, 0));
        assertEquals('B', getChar(terminal, 0, 1));
        assertEquals('C', getChar(terminal, 0, 2));
        assertEquals(' ', getChar(terminal, 0, 3), "Inserted blank at cursor position");
        assertEquals(' ', getChar(terminal, 0, 4), "Inserted blank at cursor+1");
        assertEquals('D', getChar(terminal, 0, 5), "Original D shifted right by 2");
        assertEquals('E', getChar(terminal, 0, 6));
        assertEquals('F', getChar(terminal, 0, 7));
        assertEquals('G', getChar(terminal, 0, 8));
        assertEquals('H', getChar(terminal, 0, 9));
    }

    /**
     * CSI n P (DCH - Delete Character) triggers scroll_line_left.
     * Deleting characters at a mid-line cursor position must shift
     * remaining characters to the left and fill the right edge with blanks.
     */
    @Test
    public void testDCHDeleteCharacterShiftsLeft() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        // Write "ABCDEFGHIJ" filling the entire first row
        terminal.write("ABCDEFGHIJ");
        // Move cursor to column 3 (1-based col 4)
        terminal.write("\033[1;4H");
        // Delete 2 characters at cursor position (CSI 2 P)
        terminal.write("\033[2P");

        // Expected row: A B C F G H I J _ _
        assertEquals('A', getChar(terminal, 0, 0));
        assertEquals('B', getChar(terminal, 0, 1));
        assertEquals('C', getChar(terminal, 0, 2));
        assertEquals('F', getChar(terminal, 0, 3), "F shifted left to cursor position");
        assertEquals('G', getChar(terminal, 0, 4));
        assertEquals('H', getChar(terminal, 0, 5));
        assertEquals('I', getChar(terminal, 0, 6));
        assertEquals('J', getChar(terminal, 0, 7));
        assertEquals(' ', getChar(terminal, 0, 8), "Right edge filled with blank");
        assertEquals(' ', getChar(terminal, 0, 9), "Right edge filled with blank");
    }

    /**
     * ICH at the last column must clamp n to 1 (only one cell available).
     * The row is fully filled so that ICH must actually push content off screen
     * rather than acting on an already-blank cell.
     */
    @Test
    public void testICHAtLastColumn() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        terminal.write("ABCDEFGHIJ");
        // Move cursor to column 9 (1-based col 10)
        terminal.write("\033[1;10H");
        // Insert 5 characters - should be clamped to 1 (only 1 cell at the edge)
        // 'J' at column 9 is pushed off screen, replaced by a blank
        terminal.write("\033[5@");

        // Characters 0-8 should be unchanged, col 9 should be blank (J pushed off)
        assertEquals('A', getChar(terminal, 0, 0));
        assertEquals('B', getChar(terminal, 0, 1));
        assertEquals('C', getChar(terminal, 0, 2));
        assertEquals('D', getChar(terminal, 0, 3));
        assertEquals('E', getChar(terminal, 0, 4));
        assertEquals('F', getChar(terminal, 0, 5));
        assertEquals('G', getChar(terminal, 0, 6));
        assertEquals('H', getChar(terminal, 0, 7));
        assertEquals('I', getChar(terminal, 0, 8));
        assertEquals(' ', getChar(terminal, 0, 9), "Last column should be blank after insert");
    }

    /**
     * DCH at the last column must clamp n to 1 (only one cell to delete).
     */
    @Test
    public void testDCHAtLastColumn() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        terminal.write("ABCDEFGHIJ");
        // Move cursor to column 9 (1-based col 10)
        terminal.write("\033[1;10H");
        // Delete 5 characters - should be clamped to 1
        terminal.write("\033[5P");

        // Characters 0-8 unchanged, col 9 blanked
        assertEquals('A', getChar(terminal, 0, 0));
        assertEquals('B', getChar(terminal, 0, 1));
        assertEquals('C', getChar(terminal, 0, 2));
        assertEquals('D', getChar(terminal, 0, 3));
        assertEquals('E', getChar(terminal, 0, 4));
        assertEquals('F', getChar(terminal, 0, 5));
        assertEquals('G', getChar(terminal, 0, 6));
        assertEquals('H', getChar(terminal, 0, 7));
        assertEquals('I', getChar(terminal, 0, 8));
        assertEquals(' ', getChar(terminal, 0, 9), "Last column should be blank after delete");
    }

    /**
     * Insert mode (SM 4) uses scroll_line_right internally when typing.
     * Verify that inserting a character in insert mode shifts content right.
     */
    @Test
    public void testInsertModeShiftsRight() {
        ScreenTerminal terminal = new ScreenTerminal(10, 5);
        terminal.write("ABCDEFGHIJ");
        // Move cursor to column 2 (1-based col 3)
        terminal.write("\033[1;3H");
        // Enable insert mode (CSI 4 h)
        terminal.write("\033[4h");
        // Type a character - should insert, not overwrite
        terminal.write("X");

        // Expected: A B X C D E F G H I (J dropped)
        assertEquals('A', getChar(terminal, 0, 0));
        assertEquals('B', getChar(terminal, 0, 1));
        assertEquals('X', getChar(terminal, 0, 2), "Inserted character");
        assertEquals('C', getChar(terminal, 0, 3), "Original C shifted right");
        assertEquals('D', getChar(terminal, 0, 4));
        assertEquals('E', getChar(terminal, 0, 5));
        assertEquals('F', getChar(terminal, 0, 6));
        assertEquals('G', getChar(terminal, 0, 7));
        assertEquals('H', getChar(terminal, 0, 8));
        assertEquals('I', getChar(terminal, 0, 9));
    }

    // -----------------------------------------------------------------------
    // Dirty flag tests
    // -----------------------------------------------------------------------

    /**
     * isDirty() returns true after construction (initial dirty state),
     * then false on subsequent call, then true again after write.
     */
    @Test
    public void testDirtyFlag() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);

        assertTrue(terminal.isDirty(), "Should be dirty after construction");
        assertFalse(terminal.isDirty(), "Should not be dirty after consuming");

        terminal.write("X");
        assertTrue(terminal.isDirty(), "Should be dirty after write");
        assertFalse(terminal.isDirty(), "Should not be dirty after consuming again");
    }

    // -----------------------------------------------------------------------
    // Existing tests (preserved)
    // -----------------------------------------------------------------------

    /**
     * Test for issue #1206: Missing history length check in ScreenTerminal
     * This test verifies that when the terminal is resized, history lines are properly
     * adjusted to match the new width.
     */
    @Test
    public void testHistoryLinesWidthAdjustmentOnResize() throws InterruptedException {
        // Create a terminal with initial size
        int initialWidth = 80;
        int initialHeight = 24;
        ScreenTerminal terminal = new ScreenTerminal(initialWidth, initialHeight);

        // Fill the terminal with some content
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < initialWidth; i++) {
            sb.append('X');
        }
        String line = sb.toString();

        // Write enough content to push some lines into history
        for (int i = 0; i < initialHeight + 5; i++) {
            terminal.write(line + "\n");
        }

        // Reduce the height to push more lines into history
        int reducedHeight = 10;
        terminal.setSize(initialWidth, reducedHeight);

        // Now increase the width and height
        int newWidth = 100;
        int newHeight = 20;
        terminal.setSize(newWidth, newHeight);

        // The test passes if no exception is thrown during rendering
        // We can verify this by dumping the terminal content
        String dump = terminal.dump(0, true);
        assertNotNull(dump);
    }

    /**
     * Test for issue #1231: Missing space-filling in ScreenTerminal
     * This test verifies that when the terminal width is increased, screen lines are properly
     * filled with spaces rather than null characters.
     */
    @Test
    public void testScreenLinesSpaceFillingOnWidthIncrease() throws InterruptedException {
        // Create a terminal with initial size
        int initialWidth = 80;
        int initialHeight = 24;
        ScreenTerminal terminal = new ScreenTerminal(initialWidth, initialHeight);

        // Fill the terminal with some content
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < initialWidth; i++) {
            sb.append('X');
        }
        String line = sb.toString();

        // Write content to fill the screen
        for (int i = 0; i < initialHeight; i++) {
            terminal.write(line + "\n");
        }

        // Increase the width
        int newWidth = 100;
        terminal.setSize(newWidth, initialHeight);

        // Dump the terminal content
        String dump = terminal.dump(0, true);
        assertNotNull(dump);

        // Verify the content doesn't contain null characters
        // The dump method converts the screen to HTML, so we'll use toString() instead
        String content = terminal.toString();
        for (int i = 0; i < content.length(); i++) {
            assertNotEquals('\0', content.charAt(i), "Found null character at position " + i);
        }
    }

    /**
     * Test for PR #1725: Issues with long storage format
     * This test verifies that the terminal HTML dump method correctly outputs properties like fg/bg colors.
     */
    @Test
    public void testHTMLDump() throws InterruptedException {
        // Create a terminal with initial size
        int initialWidth = 80;
        int initialHeight = 24;
        ScreenTerminal terminal = new ScreenTerminal(initialWidth, initialHeight);

        String line = "X".repeat(initialWidth);
        String clearAnsi = "\033[0m";

        // Dump the terminal content
        String dump = terminal.dump(0, true);
        assertNotNull(dump);
        assertTrue(dump.contains("<span style='color:#000000;background-color:#ffffff;'> </span>")); // The cursor

        terminal = new ScreenTerminal(initialWidth, initialHeight);
        terminal.write(clearAnsi + "\033[31m" + line + clearAnsi + "\n");

        dump = terminal.dump(0, true);
        assertNotNull(dump);
        assertTrue(dump.contains(
                "<span style='color:#880000;background-color:#000000;'>" + line + "\n</span>")); // Text FG

        terminal = new ScreenTerminal(initialWidth, initialHeight);
        terminal.write(clearAnsi + "\033[44m" + line + clearAnsi + "\n");

        dump = terminal.dump(0, true);
        assertNotNull(dump);
        assertTrue(dump.contains("background-color:#000088;'>" + line + "\n</span>")); // Text BG
    }
}
