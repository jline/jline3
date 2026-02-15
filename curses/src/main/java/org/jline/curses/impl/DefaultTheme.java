/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.HashMap;
import java.util.Map;

import org.jline.curses.Curses;
import org.jline.curses.Screen;
import org.jline.curses.Theme;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

public class DefaultTheme implements Theme {

    private final Map<String, String> styles = new HashMap<>();
    private final StyleResolver resolver = new StyleResolver(styles::get);

    private static final int TOP_LEFT = 0;
    private static final int TOP = 1;
    private static final int TOP_RIGHT = 2;
    private static final int LEFT = 3;
    private static final int CENTER = 4;
    private static final int RIGHT = 5;
    private static final int BOTTOM_LEFT = 6;
    private static final int BOTTOM = 7;
    private static final int BOTTOM_RIGHT = 8;

    public DefaultTheme() {
        // Menu styles
        styles.put("menu.text.normal", "fg:black,bg:white");
        styles.put("menu.key.normal", "fg:!red,bg:white");
        styles.put("menu.text.selected", "fg:!white,bg:black");
        styles.put("menu.key.selected", "fg:!red,bg:black");
        styles.put("menu.border", "fg:black,bg:white");
        styles.put("menu.border.back", "fg:black,bg:white");
        styles.put("menu.border.light", "fg:white,bg:white");

        // Window and background styles
        styles.put("background", "fg:black,bg:blue");
        styles.put("window.border", "fg:black,bg:white");
        styles.put("window.border.focused", "fg:!white,bg:white");
        styles.put("window.border.back", "fg:black,bg:white");
        styles.put("window.border.light", "fg:white,bg:white");
        styles.put("window.title", "fg:black,bg:white");
        styles.put("window.title.focused", "fg:black,bg:white");
        styles.put("window.shadow", "fg:!black,bg:black");
        styles.put("window.close", "fg:black,bg:white");

        // Box styles
        styles.put("box.border", "fg:black,bg:white");
        styles.put("box.border.focused", "fg:!white,bg:white");
        styles.put("box.title", "fg:black,bg:white");
        styles.put("box.title.focused", "fg:black,bg:white");
        styles.put("box.key", "fg:!red,bg:white");

        // Input component
        styles.put("input.normal", "fg:black,bg:white");
        styles.put("input.focused", "fg:!black,bg:white");
        styles.put("input.placeholder", "fg:!black,bg:white");
        styles.put("input.selection", "fg:white,bg:blue");

        // List component
        styles.put("list.normal", "fg:black,bg:white");
        styles.put("list.selected", "fg:white,bg:blue");
        styles.put("list.focused", "fg:black,bg:white,inverse");
        styles.put("list.selected.focused", "fg:white,bg:blue,inverse");

        // Table component
        styles.put("table.normal", "fg:black,bg:white");
        styles.put("table.header", "fg:black,bg:white");
        styles.put("table.selected", "fg:white,bg:blue");
        styles.put("table.focused", "fg:black,bg:white,inverse");
        styles.put("table.selected.focused", "fg:white,bg:blue,inverse");

        // Tree component
        styles.put("tree.normal", "fg:black,bg:white");
        styles.put("tree.selected", "fg:white,bg:blue");
        styles.put("tree.focused", "fg:black,bg:white,inverse");
        styles.put("tree.selected.focused", "fg:white,bg:blue,inverse");

        // TextArea component
        styles.put("textarea.normal", "fg:black,bg:white");
        styles.put("textarea.focused", "fg:black,bg:white");
        styles.put("textarea.cursor", "fg:black,bg:white,inverse");
        styles.put("textarea.linenumber", "fg:!black,bg:white");
        styles.put("textarea.scrollbar.track", "fg:black,bg:white");
        styles.put("textarea.scrollbar.thumb", "fg:!white,bg:black");

        // Separator
        styles.put("separator", "fg:black,bg:white");

        // Checkbox
        styles.put("checkbox.normal", "fg:black,bg:white");
        styles.put("checkbox.focused", "fg:!white,bg:blue");

        // RadioButton
        styles.put("radio.normal", "fg:black,bg:white");
        styles.put("radio.focused", "fg:!white,bg:blue");

        // ProgressBar
        styles.put("progress.filled", "fg:!green,bg:green");
        styles.put("progress.empty", "fg:black,bg:white");
        styles.put("progress.text", "fg:black,bg:white");

        // ComboBox
        styles.put("combo.normal", "fg:black,bg:white");
        styles.put("combo.focused", "fg:!white,bg:blue");
        styles.put("combo.dropdown", "fg:black,bg:white");
        styles.put("combo.dropdown.selected", "fg:!white,bg:blue");

        // Label
        styles.put("label.normal", "fg:black,bg:white");

        // Button
        styles.put("button.normal", "fg:black,bg:white");
        styles.put("button.focused", "fg:!white,bg:blue");
        styles.put("button.pressed", "fg:!white,bg:!black");

        // Box drawing characters
        styles.put("box.chars.double", "╔═╗║ ║╚═╝");
        styles.put("box.chars.single", "┌─┐│ │└─┘");
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
    public void separatorH(
            Screen screen,
            int x,
            int y,
            int w,
            Curses.Border sepBorder,
            Curses.Border boxBorder,
            AttributedStyle style) {
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
        AttributedString top = createBoxString(chars, w, hst, TOP_LEFT, hst, TOP, nst, TOP_RIGHT);
        AttributedString mid = createBoxString(chars, w, hst, LEFT, bst, CENTER, nst, RIGHT);
        AttributedString bot = createBoxString(chars, w, hst, BOTTOM_LEFT, nst, BOTTOM, nst, BOTTOM_RIGHT);
        screen.text(x, y, top);
        for (int j = y + 1; j < y + h - 1; j++) {
            screen.text(x, j, mid);
        }
        screen.text(x, y + h - 1, bot);
    }

    private AttributedString createBoxString(
            String chars, int w, AttributedStyle s0, int c0, AttributedStyle s1, int c1, AttributedStyle s2, int c2) {
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
