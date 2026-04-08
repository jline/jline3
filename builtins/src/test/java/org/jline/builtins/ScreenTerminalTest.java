/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ScreenTerminal} class.
 */
public class ScreenTerminalTest {

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

    /**
     * dump(timeout, forceDump=true) must still wait for the timeout before dumping
     * when the screen is not dirty. This prevents busy-loop spinning when callers
     * use forceDump in a loop.
     * Regression: #1768
     */
    @Test
    void testForceDumpWaitsForTimeout() {
        ScreenTerminal terminal = new ScreenTerminal(10, 3);
        terminal.write("Hello");

        // Consume the dirty flag
        terminal.isDirty();

        long minWaitMs = 200;
        long start = System.nanoTime();
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            terminal.dump(minWaitMs, true);
        });
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(
                elapsedMs >= minWaitMs / 2,
                "forceDump should still wait for timeout when not dirty, but returned in " + elapsedMs + "ms");
    }

    /**
     * waitDirty(0) must return immediately rather than blocking indefinitely.
     * Regression: Object.wait(0) waits forever, so timeout==0 must skip the wait.
     */
    @Test
    void testWaitDirtyZeroTimeoutReturnsImmediately() {
        ScreenTerminal terminal = new ScreenTerminal(80, 24);

        // Consume the initial dirty flag
        terminal.isDirty();

        // waitDirty(0) on a non-dirty screen must return false immediately
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            boolean result = terminal.waitDirty(0);
            assertFalse(result, "Non-dirty screen should return false");
        });
    }

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
}
