/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses.impl;

import org.jline.curses.Curses;
import org.jline.curses.Screen;
import org.jline.curses.Theme;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

import java.util.HashMap;
import java.util.Map;

public class DefaultTheme implements Theme {

    private Map<String, String> styles = new HashMap<>();
    private StyleResolver resolver = new StyleResolver(styles::get);
    private Map<Curses.Border, String> boxChars = new HashMap<>();

    private static final int TOP_LEFT       = 0;
    private static final int TOP            = 1;
    private static final int TOP_RIGHT      = 2;
    private static final int LEFT           = 3;
    private static final int CENTER         = 4;
    private static final int RIGHT          = 5;
    private static final int BOTTOM_LEFT    = 6;
    private static final int BOTTOM         = 7;
    private static final int BOTTOM_RIGHT   = 8;

    public DefaultTheme() {
        styles.put("menu.text.normal",      "fg:!white,bg:cyan");
        styles.put("menu.key.normal",       "fg:!yellow,bg:cyan");
        styles.put("menu.text.selected",    "fg:!white,bg:black");
        styles.put("menu.key.selected",     "fg:!yellow,bg:black");
        styles.put("menu.border",           "fg:!white,bg:cyan");
        styles.put("background",            "bg:blue,fg:black");
        styles.put("window.border",         "bg:white,fg:black");
        styles.put("window.border.back",    "bg:white,fg:black");
        styles.put("window.border.light",   "bg:white,fg:white");
        styles.put("window.title",          "bg:white,fg:black");
        styles.put("window.shadow",         "bg:black,fg:black");
        styles.put("window.close",          "bg:white,fg:black");
        styles.put("box.chars.double",      "╔═╗║ ║╚═╝");
        styles.put("box.chars.single",      "┌─┐│ │└─┘");
        styles.put("sep.chars.horz.double.double", "╠═╣");
        styles.put("sep.chars.horz.double.single", "╞═╡");
        styles.put("sep.chars.horz.single.single", "├─┤");
        styles.put("sep.chars.horz.single.double", "╟─╢");
    }

    @Override
    public AttributedStyle getStyle(String spec) {
        return resolver.resolve(spec);
    }

    @Override
    public void separatorH(Screen screen, int x, int y, int w, Curses.Border sepBorder, Curses.Border boxBorder, AttributedStyle style) {
        String chars = styles.get("sep.chars.horz." + sord(sepBorder) + "." + sord(boxBorder));
        AttributedString sb = createBoxString(chars, w, style, 0, style, 1, style, 2);
        screen.text(x, y, sb);
    }

    String sord(Curses.Border border) {
        switch (border) {
            case Double:
            case DoubleBevel:
                return "double";
            default:
                return "single";
        }
    }

    @SuppressWarnings("fallthrough")
    @Override
    public void box(Screen screen, int x, int y, int w, int h, Curses.Border border, String style) {
        if (w <= 0 || h <= 0) {
            return;
        }
        AttributedStyle nst = getStyle(style);
        AttributedStyle bst = getStyle(style + ".back");
        AttributedStyle hst = nst;
        String chars;
        switch (border) {
            case DoubleBevel:
                hst = getStyle(style + ".light");
            case Double:
                chars = styles.get("box.chars.double");
                break;
            case SingleBevel:
                hst = getStyle(style + ".light");
            case Single:
                chars = styles.get("box.chars.single");
                break;
            default:
                throw new IllegalStateException();
        }
        AttributedString top = createBoxString(chars, w, hst, TOP_LEFT,    hst, TOP,    nst, TOP_RIGHT);
        AttributedString mid = createBoxString(chars, w, hst, LEFT,        bst, CENTER, nst, RIGHT);
        AttributedString bot = createBoxString(chars, w, hst, BOTTOM_LEFT, nst, BOTTOM, nst, BOTTOM_RIGHT);
        screen.text(x, y, top);
        for (int j = y + 1; j < y + h - 1; j++) {
            screen.text(x, j, mid);
        }
        screen.text(x, y + h - 1, bot);
    }

    private AttributedString createBoxString(String chars, int w,
                                             AttributedStyle s0, int c0,
                                             AttributedStyle s1, int c1,
                                             AttributedStyle s2, int c2) {
        AttributedStringBuilder sb = new AttributedStringBuilder(w);
        sb.style(s0);
        sb.append(chars.charAt(c0));
        sb.style(s1);
        for (int i = 0; i < w - 2; i++) {
            sb.append(chars.charAt(c1));
        }
        sb.style(s2);
        sb.append(chars.charAt(c2));
        return sb.toAttributedString();
    }

}
