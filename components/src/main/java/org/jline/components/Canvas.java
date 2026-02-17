/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.List;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A character grid buffer for rendering components.
 * Similar to curses VirtualScreen but lighter weight.
 */
public interface Canvas {

    /**
     * Creates a new canvas with the given dimensions.
     */
    static Canvas create(int width, int height) {
        return new org.jline.components.impl.DefaultCanvas(width, height);
    }

    /**
     * Returns the width of this canvas in character cells.
     */
    int getWidth();

    /**
     * Returns the height of this canvas in character cells.
     */
    int getHeight();

    /**
     * Write styled text at the given position.
     * Characters beyond the canvas bounds are clipped.
     */
    void text(int col, int row, AttributedString text);

    /**
     * Write a single character with style at the given position.
     */
    void put(int col, int row, char ch, AttributedStyle style);

    /**
     * Fill a rectangular region with a character and style.
     */
    void fill(int col, int row, int width, int height, char ch, AttributedStyle style);

    /**
     * Create a sub-region canvas with coordinate translation and clipping.
     * Drawing operations on the returned canvas are offset by (col, row)
     * and clipped to (width, height).
     */
    Canvas subRegion(int col, int row, int width, int height);

    /**
     * Convert the canvas contents to a list of AttributedStrings,
     * compatible with {@link org.jline.utils.Display#update(List, int)}.
     */
    List<AttributedString> toLines();
}
