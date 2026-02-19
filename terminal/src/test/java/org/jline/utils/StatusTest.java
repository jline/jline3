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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.DisplayTest.VirtualTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatusTest {

    private static final int COLS = 40;
    private static final int ROWS = 10;

    private String getRow(VirtualTerminal terminal, int row) {
        Size size = terminal.getSize();
        long[] screen = terminal.dump();
        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < size.getColumns(); col++) {
            sb.append((char) screen[col + size.getColumns() * row]);
        }
        return sb.toString();
    }

    private String[] getAllRows(VirtualTerminal terminal) {
        Size size = terminal.getSize();
        String[] rows = new String[size.getRows()];
        for (int i = 0; i < size.getRows(); i++) {
            rows[i] = getRow(terminal, i);
        }
        return rows;
    }

    private List<AttributedString> statusLine(String text) {
        return Collections.singletonList(new AttributedString(text));
    }

    @Test
    public void testStatusBarRendersAtBottom() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            status.update(statusLine("status line"));
            terminal.flush();

            assertEquals("status line", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testStatusBarWithBorder() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            status.setBorder(true);
            status.update(statusLine("status line"));
            terminal.flush();

            // Border row should not be empty
            String borderRow = getRow(terminal, ROWS - 2).trim();
            assertTrue(borderRow.length() > 0, "Expected border row to be non-empty");

            assertEquals("status line", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testStatusBarMultipleLines() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            List<AttributedString> lines =
                    Arrays.asList(new AttributedString("line 1"), new AttributedString("line 2"));
            status.update(lines);
            terminal.flush();

            assertEquals("line 1", getRow(terminal, ROWS - 2).trim());
            assertEquals("line 2", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testStatusBarHide() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            status.update(statusLine("visible"));
            terminal.flush();
            assertEquals("visible", getRow(terminal, ROWS - 1).trim());

            status.hide();
            terminal.flush();

            assertEquals("", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testVerticalGrowPreservesContent() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);
            status.update(statusLine("my status"));
            terminal.flush();

            assertEquals("my status", getRow(terminal, ROWS - 1).trim());

            // Grow terminal vertically
            int newRows = ROWS + 5;
            terminal.resizeScreen(COLS, newRows);
            status.resize(terminal.getSize());
            status.redraw();
            terminal.flush();

            assertEquals("my status", getRow(terminal, newRows - 1).trim());
        }
    }

    @Test
    public void testVerticalShrinkPreservesContent() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);
            status.update(statusLine("my status"));
            terminal.flush();

            // Shrink terminal vertically
            int newRows = ROWS - 3;
            terminal.resizeScreen(COLS, newRows);
            status.resize(terminal.getSize());
            status.redraw();
            terminal.flush();

            assertEquals("my status", getRow(terminal, newRows - 1).trim());
        }
    }

    @Test
    public void testHorizontalResizePreservesStatus() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);
            status.update(statusLine("my status"));
            terminal.flush();

            // Shrink columns
            int newCols = COLS - 10;
            terminal.resizeScreen(newCols, ROWS);
            status.resize(terminal.getSize());
            status.redraw();
            terminal.flush();

            assertEquals("my status", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testNoOpResizeDoesNotScroll() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);
            status.update(statusLine("status"));
            terminal.flush();

            String[] before = getAllRows(terminal);

            // Resize to the same size (no-op)
            status.resize(terminal.getSize());
            status.redraw();
            terminal.flush();

            String[] after = getAllRows(terminal);
            for (int i = 0; i < ROWS; i++) {
                assertEquals(before[i], after[i], "Row " + i + " should not change on no-op resize");
            }
        }
    }

    @Test
    public void testSuspendAndRestore() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);
            status.update(statusLine("visible"));
            terminal.flush();
            assertEquals("visible", getRow(terminal, ROWS - 1).trim());

            // Suspend prevents updates
            status.suspend();
            status.update(statusLine("updated while suspended"));
            terminal.flush();

            // Restore redraws with last-set content
            status.restore();
            terminal.flush();
            assertEquals("updated while suspended", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testStatusUpdateChangesContent() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            status.update(statusLine("first"));
            terminal.flush();
            assertEquals("first", getRow(terminal, ROWS - 1).trim());

            status.update(statusLine("second"));
            terminal.flush();
            assertEquals("second", getRow(terminal, ROWS - 1).trim());
        }
    }

    @Test
    public void testWinchSignalTriggersResize() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);
            status.update(statusLine("my status"));
            terminal.flush();

            assertEquals("my status", getRow(terminal, ROWS - 1).trim());

            // Simulate resize and WINCH signal
            int newRows = ROWS + 3;
            terminal.resizeScreen(COLS, newRows);
            terminal.raise(Terminal.Signal.WINCH);
            status.redraw();
            terminal.flush();

            assertEquals("my status", getRow(terminal, newRows - 1).trim());
        }
    }

    @Test
    public void testStatusLineTruncation() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, 20, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            status.update(statusLine("this is a very long status line that exceeds width"));
            terminal.flush();

            String lastRow = getRow(terminal, ROWS - 1);
            assertEquals(20, lastRow.length());
            assertTrue(lastRow.endsWith("…"), "Expected ellipsis at end, got: " + lastRow);
        }
    }

    @Test
    public void testGrowThenShrinkStatusLines() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("test", "xterm", StandardCharsets.UTF_8, COLS, ROWS)) {
            Status status = Status.getStatus(terminal);
            assertNotNull(status);

            // Start with 2 lines
            List<AttributedString> twoLines =
                    Arrays.asList(new AttributedString("line 1"), new AttributedString("line 2"));
            status.update(twoLines);
            terminal.flush();
            assertEquals("line 1", getRow(terminal, ROWS - 2).trim());
            assertEquals("line 2", getRow(terminal, ROWS - 1).trim());

            // Shrink to 1 line
            status.update(statusLine("only line"));
            terminal.flush();
            assertEquals("only line", getRow(terminal, ROWS - 1).trim());

            // The row previously used by "line 1" should be cleared
            assertEquals("", getRow(terminal, ROWS - 2).trim());
        }
    }
}
