/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * A virtual screen implementation that maintains a character buffer.
 *
 * <p>This implementation stores screen content in memory using character
 * and style arrays. It provides efficient access to screen content and
 * supports all basic screen operations.</p>
 */
public class VirtualScreen implements Screen {

    private final int width;
    private final int height;
    private final char[] chars;
    private final long[] styles;
    private int cursorX;
    private int cursorY;
    private boolean cursorVisible = true;
    private boolean dirty = false;

    public VirtualScreen(int width, int height) {
        this.width = width;
        this.height = height;
        this.chars = new char[width * height];
        this.styles = new long[width * height];
        clear();
    }

    @Override
    public void text(int x, int y, AttributedString s) {
        if (y < 0 || y >= height || x < 0) {
            return;
        }

        int p = y * width + x;
        int maxLen = Math.min(s.length(), width - x);

        for (int i = 0; i < maxLen; i++, p++) {
            chars[p] = s.charAt(i);
            styles[p] = s.styleAt(i).getStyle();
        }
        dirty = true;
    }

    @Override
    public void fill(int x, int y, int w, int h, AttributedStyle style) {
        if (y < 0 || y >= height || x < 0 || x >= width) {
            return;
        }

        int maxW = Math.min(w, width - x);
        int maxH = Math.min(h, height - y);
        long s = style.getStyle();

        for (int j = 0; j < maxH; j++) {
            int p = (y + j) * width + x;
            for (int i = 0; i < maxW; i++, p++) {
                chars[p] = ' ';
                styles[p] = s;
            }
        }
        dirty = true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ' ';
            styles[i] = AttributedStyle.DEFAULT.getStyle();
        }
        cursorX = 0;
        cursorY = 0;
        dirty = true;
    }

    @Override
    public void refresh() {
        // In a real implementation, this would flush to the terminal
        // For now, just mark as clean
        dirty = false;
    }

    @Override
    public Size getSize() {
        return new Size(width, height);
    }

    @Override
    public void setCursor(int x, int y) {
        this.cursorX = Math.max(0, Math.min(x, width - 1));
        this.cursorY = Math.max(0, Math.min(y, height - 1));
    }

    @Override
    public Position getCursor() {
        return new Position(cursorX, cursorY);
    }

    @Override
    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
    }

    /**
     * Gets the character at the specified position.
     *
     * @param x the column position
     * @param y the row position
     * @return the character at the position
     */
    public char getChar(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return ' ';
        }
        return chars[y * width + x];
    }

    /**
     * Gets the style at the specified position.
     *
     * @param x the column position
     * @param y the row position
     * @return the style at the position
     */
    public AttributedStyle getStyle(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return AttributedStyle.DEFAULT;
        }
        return new AttributedStyle(styles[y * width + x], 0);
    }

    @Override
    public void darken(int x, int y, int w, int h, AttributedStyle style) {
        if (y < 0 || y >= height || x < 0 || x >= width) {
            return;
        }

        int maxW = Math.min(w, width - x);
        int maxH = Math.min(h, height - y);
        long darkStyle = style.getStyle();

        for (int j = 0; j < maxH; j++) {
            int p = (y + j) * width + x;
            for (int i = 0; i < maxW; i++, p++) {
                // Preserve existing character, only change the style
                styles[p] = darkStyle;
            }
        }
        dirty = true;
    }

    /**
     * Checks if the screen has been modified since the last refresh.
     *
     * @return true if the screen is dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Gets whether the cursor is visible.
     *
     * @return true if the cursor is visible
     */
    public boolean isCursorVisible() {
        return cursorVisible;
    }

    public List<AttributedString> lines() {
        List<AttributedString> lines = new ArrayList<>(height);
        AttributedStringBuilder sb = new AttributedStringBuilder(width);
        int p = 0;
        for (int j = 0; j < height; j++) {
            sb.setLength(0);
            for (int i = 0; i < width; i++) {
                sb.style(new AttributedStyle(styles[p], 0xFFFFFFFF));
                sb.append(chars[p]);
                p++;
            }
            lines.add(sb.toAttributedString());
        }
        return lines;
    }
}
