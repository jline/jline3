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

import org.jline.curses.Screen;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class VirtualScreen implements Screen {

    private final int width;
    private final int height;
    private final char[] chars;
    private final long[] styles;

    public VirtualScreen(int width, int height) {
        this.width = width;
        this.height = height;
        this.chars = new char[width * height];
        this.styles = new long[width * height];
    }

    @Override
    public void text(int x, int y, AttributedString s) {
        int p = y * width + x;
        for (int i = 0; i < s.length(); i++, p++) {
            chars[p] = s.charAt(i);
            styles[p] = s.styleAt(i).getStyle();
        }
    }

    @Override
    public void fill(int x, int y, int w, int h, AttributedStyle style) {
        long s = style.getStyle();
        for (int j = 0; j < h; j++) {
            int p = (y + j) * width + x;
            for (int i = 0; i < w; i++, p++) {
                chars[p] = ' ';
                styles[p] = s;
            }
        }
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
