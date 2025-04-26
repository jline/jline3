/*
 * Copyright (c) 2002-2024, the original author(s).
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
}
