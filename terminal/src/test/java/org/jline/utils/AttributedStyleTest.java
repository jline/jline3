/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributedStyleTest {

    /**
     * Verifies that chained RGB and indexed color updates produce the expected final styles.
     */
    @Test
    void testMixedColors() {
        assertEquals(
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE),
                AttributedStyle.DEFAULT.foregroundRgb(0xf00ff0).foreground(AttributedStyle.BLUE));
        assertEquals(
                AttributedStyle.DEFAULT.foregroundRgb(0x0ff00f),
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).foregroundRgb(0x0ff00f));
        assertEquals(
                AttributedStyle.DEFAULT.background(AttributedStyle.BLUE),
                AttributedStyle.DEFAULT.backgroundRgb(0xf00ff0).background(AttributedStyle.BLUE));
        assertEquals(
                AttributedStyle.DEFAULT.backgroundRgb(0x0ff00f),
                AttributedStyle.DEFAULT.background(AttributedStyle.BLUE).backgroundRgb(0x0ff00f));

        AttributedString fgChained =
                createColoredTestString(AttributedStyle::foreground, AttributedStyle::foregroundRgb);
        AttributedString fgDefault = createColoredTestString(
                (style, color) -> AttributedStyle.DEFAULT.foreground(color),
                (style, rgb) -> AttributedStyle.DEFAULT.foregroundRgb(rgb));
        AttributedString bgChained =
                createColoredTestString(AttributedStyle::background, AttributedStyle::backgroundRgb);
        AttributedString bgDefault = createColoredTestString(
                (style, color) -> AttributedStyle.DEFAULT.background(color),
                (style, rgb) -> AttributedStyle.DEFAULT.backgroundRgb(rgb));

        assertEquals(fgChained, fgDefault);
        assertEquals(bgChained, bgDefault);
    }

    /**
     * Builds a sample attributed string that alternates indexed and RGB color changes.
     *
     * @param color the style update to apply for indexed colors
     * @param rgbColor the style update to apply for RGB colors
     * @return a test string with mixed color segments
     */
    private static AttributedString createColoredTestString(
            BiFunction<AttributedStyle, Integer, AttributedStyle> color,
            BiFunction<AttributedStyle, Integer, AttributedStyle> rgbColor) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("a")
                .style(style -> color.apply(style, AttributedStyle.GREEN))
                .append("b")
                .style(style -> rgbColor.apply(style, 0xffff00))
                .append("c")
                .style(style -> color.apply(style, AttributedStyle.RED))
                .append("d")
                .style(style -> rgbColor.apply(style, 0x00ffff))
                .append("e")
                .style(style -> color.apply(style, AttributedStyle.YELLOW))
                .append("f")
                .style(style -> rgbColor.apply(style, 0xff0000))
                .append("g")
                .style(style -> color.apply(style, AttributedStyle.GREEN))
                .append("h");
        return builder.toAttributedString();
    }
}
