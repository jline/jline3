/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import static org.jline.utils.AttributedStyle.BG_COLOR;
import static org.jline.utils.AttributedStyle.BG_COLOR_EXP;
import static org.jline.utils.AttributedStyle.FG_COLOR;
import static org.jline.utils.AttributedStyle.FG_COLOR_EXP;
import static org.jline.utils.AttributedStyle.F_BACKGROUND;
import static org.jline.utils.AttributedStyle.F_BLINK;
import static org.jline.utils.AttributedStyle.F_BOLD;
import static org.jline.utils.AttributedStyle.F_CONCEAL;
import static org.jline.utils.AttributedStyle.F_CROSSED_OUT;
import static org.jline.utils.AttributedStyle.F_FAINT;
import static org.jline.utils.AttributedStyle.F_FOREGROUND;
import static org.jline.utils.AttributedStyle.F_INVERSE;
import static org.jline.utils.AttributedStyle.F_ITALIC;
import static org.jline.utils.AttributedStyle.F_UNDERLINE;
import static org.jline.utils.AttributedStyle.F_HIDDEN;
import static org.jline.utils.AttributedStyle.MASK;

public abstract class AttributedCharSequence implements CharSequence {

    public String toAnsi() {
        return toAnsi(null);
    }

    public String toAnsi(Terminal terminal) {
        if (terminal != null && Terminal.TYPE_DUMB.equals(terminal.getType())) {
            return toString();
        }
        StringBuilder sb = new StringBuilder();
        int style = 0;
        int foreground = -1;
        int background = -1;
        int colors = 8;
        if (terminal != null) {
            Integer max_colors = terminal.getNumericCapability(Capability.max_colors);
            if (max_colors != null) {
                colors = max_colors;
            }
        }
        for (int i = 0; i < length(); i++) {
            char c = charAt(i);
            int  s = styleCodeAt(i) & ~F_HIDDEN; // The hidden flag does not change the ansi styles
            if (style != s) {
                int  d = (style ^ s) & MASK;
                int fg = (s & F_FOREGROUND) != 0 ? (s & FG_COLOR) >>> FG_COLOR_EXP : -1;
                int bg = (s & F_BACKGROUND) != 0 ? (s & BG_COLOR) >>> BG_COLOR_EXP : -1;
                if (s == 0) {
                    sb.append("\033[0m");
                    foreground = background = -1;
                } else {
                    sb.append("\033[");
                    boolean first = true;
                    if ((d & (F_BOLD | F_FAINT)) != 0) {
                        first = attr(sb, (s & F_BOLD) != 0 ? "1" : (s & F_FAINT) != 0 ? "2" : "22", first);
                    }
                    if ((d & F_ITALIC) != 0) {
                        first = attr(sb, (s & F_ITALIC) != 0 ? "3" : "23", first);
                    }
                    if ((d & F_UNDERLINE) != 0) {
                        first = attr(sb, (s & F_UNDERLINE) != 0 ? "4" : "24", first);
                    }
                    if ((d & F_BLINK) != 0) {
                        first = attr(sb, (s & F_BLINK) != 0 ? "5" : "25", first);
                    }
                    if ((d & F_INVERSE) != 0) {
                        first = attr(sb, (s & F_INVERSE) != 0 ? "7" : "27", first);
                    }
                    if ((d & F_CONCEAL) != 0) {
                        first = attr(sb, (s & F_CONCEAL) != 0 ? "8" : "28", first);
                    }
                    if ((d & F_CROSSED_OUT) != 0) {
                        first = attr(sb, (s & F_CROSSED_OUT) != 0 ? "9" : "29", first);
                    }
                    if (foreground != fg) {
                        if (fg >= 0) {
                            int rounded = roundColor(fg, colors);
                            if (rounded < 8) {
                                first = attr(sb, "3" + Integer.toString(rounded), first);
                            } else if (rounded < 16) {
                                first = attr(sb, "9" + Integer.toString(rounded - 8), first);
                            } else {
                                first = attr(sb, "38;5;" + Integer.toString(rounded), first);
                            }
                        } else {
                            first = attr(sb, "39", first);
                        }
                        foreground = fg;
                    }
                    if (background != bg) {
                        if (bg >= 0) {
                            int rounded = roundColor(bg, colors);
                            if (rounded < 8) {
                                first = attr(sb, "4" + Integer.toString(rounded), first);
                            } else if (rounded < 16) {
                                first = attr(sb, "10" + Integer.toString(rounded - 8), first);
                            } else {
                                first = attr(sb, "48;5;" + Integer.toString(rounded), first);
                            }
                        } else {
                            first = attr(sb, "49", first);
                        }
                        background = bg;
                    }
                    sb.append("m");
                }
                style = s;
            }
            sb.append(c);
        }
        if (style != 0) {
            sb.append("\033[0m");
        }
        return sb.toString();
    }

    private static final int[] COLORS_256 = {
            0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
            0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff,

            0x000000, 0x00005f, 0x000087, 0x0000af, 0x0000d7, 0x0000ff, 0x005f00, 0x005f5f,
            0x005f87, 0x005faf, 0x005fd7, 0x005fff, 0x008700, 0x00875f, 0x008787, 0x0087af,
            0x0087d7, 0x0087ff, 0x00af00, 0x00af5f, 0x00af87, 0x00afaf, 0x00afd7, 0x00afff,
            0x00d700, 0x00d75f, 0x00d787, 0x00d7af, 0x00d7d7, 0x00d7ff, 0x00ff00, 0x00ff5f,
            0x00ff87, 0x00ffaf, 0x00ffd7, 0x00ffff, 0x5f0000, 0x5f005f, 0x5f0087, 0x5f00af,
            0x5f00d7, 0x5f00ff, 0x5f5f00, 0x5f5f5f, 0x5f5f87, 0x5f5faf, 0x5f5fd7, 0x5f5fff,
            0x5f8700, 0x5f875f, 0x5f8787, 0x5f87af, 0x5f87d7, 0x5f87ff, 0x5faf00, 0x5faf5f,
            0x5faf87, 0x5fafaf, 0x5fafd7, 0x5fafff, 0x5fd700, 0x5fd75f, 0x5fd787, 0x5fd7af,
            0x5fd7d7, 0x5fd7ff, 0x5fff00, 0x5fff5f, 0x5fff87, 0x5fffaf, 0x5fffd7, 0x5fffff,
            0x870000, 0x87005f, 0x870087, 0x8700af, 0x8700d7, 0x8700ff, 0x875f00, 0x875f5f,
            0x875f87, 0x875faf, 0x875fd7, 0x875fff, 0x878700, 0x87875f, 0x878787, 0x8787af,
            0x8787d7, 0x8787ff, 0x87af00, 0x87af5f, 0x87af87, 0x87afaf, 0x87afd7, 0x87afff,
            0x87d700, 0x87d75f, 0x87d787, 0x87d7af, 0x87d7d7, 0x87d7ff, 0x87ff00, 0x87ff5f,
            0x87ff87, 0x87ffaf, 0x87ffd7, 0x87ffff, 0xaf0000, 0xaf005f, 0xaf0087, 0xaf00af,
            0xaf00d7, 0xaf00ff, 0xaf5f00, 0xaf5f5f, 0xaf5f87, 0xaf5faf, 0xaf5fd7, 0xaf5fff,
            0xaf8700, 0xaf875f, 0xaf8787, 0xaf87af, 0xaf87d7, 0xaf87ff, 0xafaf00, 0xafaf5f,
            0xafaf87, 0xafafaf, 0xafafd7, 0xafafff, 0xafd700, 0xafd75f, 0xafd787, 0xafd7af,
            0xafd7d7, 0xafd7ff, 0xafff00, 0xafff5f, 0xafff87, 0xafffaf, 0xafffd7, 0xafffff,
            0xd70000, 0xd7005f, 0xd70087, 0xd700af, 0xd700d7, 0xd700ff, 0xd75f00, 0xd75f5f,
            0xd75f87, 0xd75faf, 0xd75fd7, 0xd75fff, 0xd78700, 0xd7875f, 0xd78787, 0xd787af,
            0xd787d7, 0xd787ff, 0xd7af00, 0xd7af5f, 0xd7af87, 0xd7afaf, 0xd7afd7, 0xd7afff,
            0xd7d700, 0xd7d75f, 0xd7d787, 0xd7d7af, 0xd7d7d7, 0xd7d7ff, 0xd7ff00, 0xd7ff5f,
            0xd7ff87, 0xd7ffaf, 0xd7ffd7, 0xd7ffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af,
            0xff00d7, 0xff00ff, 0xff5f00, 0xff5f5f, 0xff5f87, 0xff5faf, 0xff5fd7, 0xff5fff,
            0xff8700, 0xff875f, 0xff8787, 0xff87af, 0xff87d7, 0xff87ff, 0xffaf00, 0xffaf5f,
            0xffaf87, 0xffafaf, 0xffafd7, 0xffafff, 0xffd700, 0xffd75f, 0xffd787, 0xffd7af,
            0xffd7d7, 0xffd7ff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf, 0xffffd7, 0xffffff,

            0x080808, 0x121212, 0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e,
            0x585858, 0x626262, 0x6c6c6c, 0x767676, 0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e,
            0xa8a8a8, 0xb2b2b2, 0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada, 0xe4e4e4, 0xeeeeee,
    };

    public static int rgbColor(int col) {
        return COLORS_256[col];
    }

    public static int roundColor(int col, int max) {
        if (col >= max) {
            int c = COLORS_256[col];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = (c >> 0) & 0xFF;
            col = roundColor(r, g, b, COLORS_256, max);
        }
        return col;
    }

    public static int roundRgbColor(int r, int g, int b, int max) {
        return roundColor(r, g, b, COLORS_256, max);
    }

    private static int roundColor(int r, int g, int b, int[] colors, int max) {
        int best_distance = Integer.MAX_VALUE;
        int best_index = Integer.MAX_VALUE;
        for (int idx = 0; idx < max; idx++) {
            int color = colors[idx];
            int test_r = (color >> 16) & 0xFF;
            int test_g = (color >> 8) & 0xFF;
            int test_b = (color >> 0) & 0xFF;
            int distance = 2 * sqr(r - test_r)
                         + 4 * sqr(g - test_g)
                         + 3 * sqr(b - test_b);
            if (distance <= best_distance) {
                best_index = idx;
                best_distance = distance;
            }
        }
        return best_index;
    }

    static int sqr(int d) {
        return d * d;
    }

    private static boolean attr(StringBuilder sb, String s, boolean first) {
        if (!first) {
            sb.append(";");
        }
        sb.append(s);
        return false;
    }

    public abstract AttributedStyle styleAt(int index);

    int styleCodeAt(int index) {
        return styleAt(index).getStyle();
    }

    public boolean isHidden(int index) {
        return (styleCodeAt(index) & F_HIDDEN) != 0;
    }

    public int runStart(int index) {
        AttributedStyle style = styleAt(index);
        while (index > 0 && styleAt(index - 1).equals(style)) {
            index--;
        }
        return index;
    }

    public int runLimit(int index) {
        AttributedStyle style = styleAt(index);
        while (index < length() - 1 && styleAt(index + 1).equals(style)) {
            index++;
        }
        return index + 1;
    }

    @Override
    public abstract AttributedString subSequence(int start, int end);

    public AttributedString substring(int start, int end) {
        return subSequence(start, end);
    }

    protected abstract char[] buffer();

    protected abstract int offset();

    @Override
    public char charAt(int index) {
        return buffer()[offset() + index];
    }

    public int codePointAt(int index) {
        return Character.codePointAt(buffer(), index + offset());
    }

    public boolean contains(char c) {
        for (int i = 0; i < length(); i++) {
            if (charAt(i) == c) {
                return true;
            }
        }
        return false;
    }

    public int codePointBefore(int index) {
        return Character.codePointBefore(buffer(), index + offset());
    }

    public int codePointCount(int index, int length) {
        return Character.codePointCount(buffer(), index + offset(), length);
    }

    public int columnLength() {
        int cols = 0;
        int len = length();
        for (int cur = 0; cur < len; ) {
            int cp = codePointAt(cur);
            if (!isHidden(cur))
                cols += WCWidth.wcwidth(cp);
            cur += Character.charCount(cp);
        }
        return cols;
    }

    public AttributedString columnSubSequence(int start, int stop) {
        int begin = 0;
        int col = 0;
        while (begin < this.length()) {
            int cp = codePointAt(begin);
            int w = isHidden(begin) ? 0 : WCWidth.wcwidth(cp);
            if (col + w > start) {
                break;
            }
            begin++;
            col += w;
        }
        int end = begin;
        while (end < this.length()) {
            int cp = codePointAt(end);
            if (cp == '\n')
                break;
            int w = isHidden(end) ? 0 : WCWidth.wcwidth(cp);
            if (col + w > stop) {
                break;
            }
            end++;
            col += w;
        }
        return subSequence(begin, end);
    }

    public List<AttributedString> columnSplitLength(int columns) {
        return columnSplitLength(columns, false, true);
    }

    public List<AttributedString> columnSplitLength(int columns, boolean includeNewlines, boolean delayLineWrap) {
        List<AttributedString> strings = new ArrayList<>();
        int cur = 0;
        int beg = cur;
        int col = 0;
        while (cur < length()) {
            int cp = codePointAt(cur);
            int w = isHidden(cur) ? 0 : WCWidth.wcwidth(cp);
            if (cp == '\n') {
                strings.add(subSequence(beg, includeNewlines ? cur+1 : cur));
                beg = cur + 1;
                col = 0;
            } else if ((col += w) > columns) {
                strings.add(subSequence(beg, cur));
                beg = cur;
                col = w;
            }
            cur += Character.charCount(cp);
        }
        strings.add(subSequence(beg, cur));
        return strings;
    }

    @Override
    public String toString() {
        return new String(buffer(), offset(), length());
    }

    public AttributedString toAttributedString() {
        return substring(0, length());
    }

}
