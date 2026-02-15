/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Screen interface for terminal-based user interfaces.
 *
 * <p>The Screen interface provides methods for drawing text and graphics
 * to a terminal screen buffer. It supports text rendering, area filling,
 * cursor management, and screen clearing operations.</p>
 *
 * <p>All drawing operations are performed on a back buffer and become
 * visible only after calling {@link #refresh()}. This allows for efficient
 * screen updates and flicker-free rendering.</p>
 *
 * @since 3.0
 */
public interface Screen {

    /**
     * Draws text at the specified position.
     *
     * @param x the column position (0-based)
     * @param y the row position (0-based)
     * @param s the attributed string to draw
     */
    void text(int x, int y, AttributedString s);

    /**
     * Fills a rectangular area with the specified style.
     *
     * @param x the starting column position (0-based)
     * @param y the starting row position (0-based)
     * @param w the width of the area
     * @param h the height of the area
     * @param style the style to fill with
     */
    void fill(int x, int y, int w, int h, AttributedStyle style);

    /**
     * Clears the entire screen.
     */
    void clear();

    /**
     * Refreshes the screen, making all pending changes visible.
     */
    void refresh();

    /**
     * Gets the size of the screen.
     *
     * @return the screen size
     */
    Size getSize();

    /**
     * Sets the cursor position.
     *
     * @param x the column position (0-based)
     * @param y the row position (0-based)
     */
    void setCursor(int x, int y);

    /**
     * Gets the current cursor position.
     *
     * @return the cursor position
     */
    Position getCursor();

    /**
     * Sets whether the cursor is visible.
     *
     * @param visible true to show the cursor, false to hide it
     */
    void setCursorVisible(boolean visible);

    /**
     * Darkens a rectangular area, preserving existing characters but applying
     * a dark style. This is used for window shadow effects where the content
     * below should still be partially visible.
     *
     * <p>The default implementation falls back to {@link #fill} with the given style.</p>
     *
     * @param x the starting column position (0-based)
     * @param y the starting row position (0-based)
     * @param w the width of the area
     * @param h the height of the area
     * @param style the dark style to apply (typically black background with dim foreground)
     */
    default void darken(int x, int y, int w, int h, AttributedStyle style) {
        fill(x, y, w, h, style);
    }

    /**
     * Registers an image to be displayed at the specified position.
     *
     * <p>The image data should be a pre-rendered escape sequence string
     * (e.g., from {@code TerminalGraphics.convertImage()}). The position
     * and size are specified in character cells.</p>
     *
     * <p>The default implementation is a no-op for screens that do not
     * support graphical output.</p>
     *
     * @param x the column position (0-based)
     * @param y the row position (0-based)
     * @param w the width in character cells
     * @param h the height in character cells
     * @param imageData the pre-rendered image escape sequence
     */
    default void image(int x, int y, int w, int h, String imageData) {
        // Default: no-op for non-graphical screens
    }
}
