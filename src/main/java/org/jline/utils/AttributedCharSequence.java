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
        StringBuilder sb = new StringBuilder();
        int style = 0;
        Integer max_colors = terminal == null ? Integer.valueOf(8)
            : terminal.getNumericCapability(Capability.max_colors);
        boolean color8 = max_colors != null && max_colors >= 8;
        boolean color256 = max_colors != null && max_colors >= 256;
        for (int i = 0; i < length(); i++) {
            char c = charAt(i);
            int  s = styleCodeAt(i) & ~F_HIDDEN; // The hidden flag does not change the ansi styles
            int  d = (style ^ s) & MASK;
            if (d != 0 && (color8 || color256)) {
                if (s == 0) {
                    sb.append("\033[0m");
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
                    if ((d & F_FOREGROUND) != 0) {
                        if ((s & F_FOREGROUND) != 0) {
                            int col = (s & FG_COLOR) >>> FG_COLOR_EXP;
                            if (col < 8 || !color256) {
                                first = attr(sb, Integer.toString(30 + roundTo8Color(col)), first);
                            } else {
                                first = attr(sb, "38;5;" + Integer.toString(col), first);
                            }
                        } else {
                            first = attr(sb, "39", first);
                        }
                    }
                    if ((d & F_BACKGROUND) != 0) {
                        if ((s & F_BACKGROUND) != 0) {
                            int col = (s & BG_COLOR) >>> BG_COLOR_EXP;
                            if (col < 8 || !color256) {
                                first = attr(sb, Integer.toString(40 + roundTo8Color(col)), first);
                            } else {
                                first = attr(sb, "48;5;" + Integer.toString(col), first);
                            }
                        } else {
                            first = attr(sb, "49", first);
                        }
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

    private static int roundTo8Color(int col) {
        return col % 8;
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

    boolean isHidden(int index) {
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
        List<AttributedString> strings = new ArrayList<>();
        int cur = 0;
        int beg = cur;
        int col = 0;
        while (cur < length()) {
            int cp = codePointAt(cur);
            int w = isHidden(cur) ? 0 : WCWidth.wcwidth(cp);
            if (cp == '\n') {
                strings.add(subSequence(beg, cur));
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
