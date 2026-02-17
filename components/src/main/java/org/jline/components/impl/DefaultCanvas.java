/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jline.components.Canvas;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Default canvas implementation backed by char[] and long[] arrays.
 * Stores both the style and mask from {@link AttributedStyle} to
 * ensure lossless round-trip through {@link #toLines()}.
 */
public class DefaultCanvas implements Canvas {

    private final int width;
    private final int height;
    private final char[] chars;
    private final long[] styles;
    private final long[] masks;

    public DefaultCanvas(int width, int height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.chars = new char[this.width * this.height];
        this.styles = new long[this.width * this.height];
        this.masks = new long[this.width * this.height];
        Arrays.fill(chars, ' ');
        // styles and masks default to 0 which is DEFAULT style's internal encoding
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void text(int col, int row, AttributedString text) {
        if (row < 0 || row >= height || col >= width) {
            return;
        }
        // When col < 0, we still enter the loop to render the visible portion
        // of the text (characters where col + i >= 0). This allows partial
        // rendering of text that starts off-screen to the left.
        int len = text.length();
        for (int i = 0; i < len && col + i < width; i++) {
            if (col + i < 0) continue;
            int idx = row * width + col + i;
            AttributedStyle s = text.styleAt(i);
            chars[idx] = text.charAt(i);
            styles[idx] = s.getStyle();
            masks[idx] = s.getMask();
        }
    }

    @Override
    public void put(int col, int row, char ch, AttributedStyle style) {
        if (col < 0 || col >= width || row < 0 || row >= height) {
            return;
        }
        int idx = row * width + col;
        chars[idx] = ch;
        styles[idx] = style.getStyle();
        masks[idx] = style.getMask();
    }

    @Override
    public void fill(int col, int row, int w, int h, char ch, AttributedStyle style) {
        long s = style.getStyle();
        long m = style.getMask();
        for (int r = row; r < row + h && r < height; r++) {
            if (r < 0) continue;
            for (int c = col; c < col + w && c < width; c++) {
                if (c < 0) continue;
                int idx = r * width + c;
                chars[idx] = ch;
                styles[idx] = s;
                masks[idx] = m;
            }
        }
    }

    @Override
    public Canvas subRegion(int col, int row, int w, int h) {
        return new SubCanvas(this, col, row, w, h);
    }

    @Override
    public List<AttributedString> toLines() {
        List<AttributedString> lines = new ArrayList<>(height);
        for (int r = 0; r < height; r++) {
            AttributedStringBuilder sb = new AttributedStringBuilder(width);
            for (int c = 0; c < width; c++) {
                int idx = r * width + c;
                sb.style(new AttributedStyle(styles[idx], masks[idx]));
                sb.append(chars[idx]);
            }
            lines.add(sb.toAttributedString());
        }
        return lines;
    }

    // Package-private accessors for SubCanvas
    void putDirect(int col, int row, char ch, long style, long mask) {
        if (col >= 0 && col < width && row >= 0 && row < height) {
            int idx = row * width + col;
            chars[idx] = ch;
            styles[idx] = style;
            masks[idx] = mask;
        }
    }

    char getChar(int col, int row) {
        if (col >= 0 && col < width && row >= 0 && row < height) {
            return chars[row * width + col];
        }
        return ' ';
    }

    long getStyle(int col, int row) {
        if (col >= 0 && col < width && row >= 0 && row < height) {
            return styles[row * width + col];
        }
        return 0;
    }

    long getMask(int col, int row) {
        if (col >= 0 && col < width && row >= 0 && row < height) {
            return masks[row * width + col];
        }
        return 0;
    }

    /**
     * Sub-region canvas with coordinate translation and clipping.
     */
    private static class SubCanvas implements Canvas {
        private final DefaultCanvas root;
        private final int offsetCol;
        private final int offsetRow;
        private final int width;
        private final int height;

        SubCanvas(DefaultCanvas root, int col, int row, int width, int height) {
            this.root = root;
            this.offsetCol = col;
            this.offsetRow = row;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void text(int col, int row, AttributedString text) {
            if (row < 0 || row >= height || col >= width) return;
            int len = text.length();
            for (int i = 0; i < len && col + i < width; i++) {
                if (col + i < 0) continue;
                AttributedStyle s = text.styleAt(i);
                root.putDirect(offsetCol + col + i, offsetRow + row, text.charAt(i), s.getStyle(), s.getMask());
            }
        }

        @Override
        public void put(int col, int row, char ch, AttributedStyle style) {
            if (col < 0 || col >= width || row < 0 || row >= height) return;
            root.putDirect(offsetCol + col, offsetRow + row, ch, style.getStyle(), style.getMask());
        }

        @Override
        public void fill(int col, int row, int w, int h, char ch, AttributedStyle style) {
            long s = style.getStyle();
            long m = style.getMask();
            for (int r = row; r < row + h && r < height; r++) {
                if (r < 0) continue;
                for (int c = col; c < col + w && c < width; c++) {
                    if (c < 0) continue;
                    root.putDirect(offsetCol + c, offsetRow + r, ch, s, m);
                }
            }
        }

        @Override
        public Canvas subRegion(int col, int row, int w, int h) {
            // Clip to this sub-canvas bounds
            int nc = Math.max(0, col);
            int nr = Math.max(0, row);
            int nw = Math.min(w, width - nc);
            int nh = Math.min(h, height - nr);
            return new SubCanvas(root, offsetCol + nc, offsetRow + nr, nw, nh);
        }

        @Override
        public List<AttributedString> toLines() {
            // Create a temporary canvas and copy our region
            DefaultCanvas tmp = new DefaultCanvas(width, height);
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    tmp.putDirect(
                            c,
                            r,
                            root.getChar(offsetCol + c, offsetRow + r),
                            root.getStyle(offsetCol + c, offsetRow + r),
                            root.getMask(offsetCol + c, offsetRow + r));
                }
            }
            return tmp.toLines();
        }
    }
}
