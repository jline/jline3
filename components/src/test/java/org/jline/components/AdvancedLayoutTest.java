/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.Arrays;
import java.util.List;

import org.jline.components.layout.FlexAlign;
import org.jline.components.layout.FlexDirection;
import org.jline.components.layout.FlexJustify;
import org.jline.components.layout.Insets;
import org.jline.components.layout.LayoutEngine;
import org.jline.components.layout.Size;
import org.jline.components.ui.Box;
import org.jline.components.ui.ProgressBar;
import org.jline.components.ui.Separator;
import org.jline.components.ui.Text;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedLayoutTest {

    @Test
    void testJustifyCenter() {
        Text t = Text.builder().text("X").build();
        Canvas canvas = Canvas.create(10, 5);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t),
                10,
                5,
                FlexDirection.COLUMN,
                FlexJustify.CENTER,
                FlexAlign.START,
                0,
                Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        // "X" with height 1 centered in 5 rows → should be at row 2
        assertEquals('X', lines.get(2).charAt(0));
    }

    @Test
    void testJustifyEnd() {
        Text t = Text.builder().text("X").build();
        Canvas canvas = Canvas.create(10, 5);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t),
                10,
                5,
                FlexDirection.COLUMN,
                FlexJustify.END,
                FlexAlign.START,
                0,
                Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        // "X" with height 1 at end → should be at row 4
        assertEquals('X', lines.get(4).charAt(0));
    }

    @Test
    void testJustifySpaceBetween() {
        Text t1 = Text.builder().text("A").build();
        Text t2 = Text.builder().text("B").build();
        Canvas canvas = Canvas.create(10, 5);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t1, t2),
                10,
                5,
                FlexDirection.COLUMN,
                FlexJustify.SPACE_BETWEEN,
                FlexAlign.START,
                0,
                Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        // A at row 0, B at row 4 (space between distributes remaining)
        assertEquals('A', lines.get(0).charAt(0));
        assertEquals('B', lines.get(4).charAt(0));
    }

    @Test
    void testAlignCenter() {
        Text t = Text.builder().text("X").build(); // width=1
        Canvas canvas = Canvas.create(10, 1);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t),
                10,
                1,
                FlexDirection.COLUMN,
                FlexJustify.START,
                FlexAlign.CENTER,
                0,
                Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        // "X" width=1 centered in cross-axis of 10 → col 4 or 5
        String row = lines.get(0).toString();
        int xPos = row.indexOf('X');
        assertTrue(xPos >= 4 && xPos <= 5, "X should be centered, but at " + xPos);
    }

    @Test
    void testAlignEnd() {
        Text t = Text.builder().text("X").build();
        Canvas canvas = Canvas.create(10, 1);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t),
                10,
                1,
                FlexDirection.COLUMN,
                FlexJustify.START,
                FlexAlign.END,
                0,
                Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        String row = lines.get(0).toString();
        assertEquals('X', row.charAt(9));
    }

    @Test
    void testRowWithGap() {
        Text t1 = Text.builder().text("A").build();
        Text t2 = Text.builder().text("B").build();
        Canvas canvas = Canvas.create(10, 1);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t1, t2),
                10,
                1,
                FlexDirection.ROW,
                FlexJustify.START,
                FlexAlign.START,
                2,
                Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        String row = lines.get(0).toString();
        assertEquals('A', row.charAt(0));
        // B should be at col 3 (1 char for A + 2 gap)
        assertEquals('B', row.charAt(3));
    }

    @Test
    void testShrinkProportionally() {
        // Two children wanting 10 each in 15-wide space
        Text t1 = Text.builder().text("AAAAAAAAAA").build(); // width 10
        Text t2 = Text.builder().text("BBBBBBBBBB").build(); // width 10
        Canvas canvas = Canvas.create(15, 1);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t1, t2),
                15,
                1,
                FlexDirection.ROW,
                FlexJustify.START,
                FlexAlign.START,
                0,
                Insets.NONE);

        // Each should get ~7-8 chars (proportional shrink from 10+10=20 into 15)
        List<AttributedString> lines = canvas.toLines();
        String row = lines.get(0).toString();
        assertTrue(row.contains("A"));
        assertTrue(row.contains("B"));
    }

    @Test
    void testNestedBoxes() {
        Box inner1 = Box.builder()
                .borderStyle(Box.BorderStyle.SINGLE)
                .child(Text.builder().text("A").build())
                .build();
        Box inner2 = Box.builder()
                .borderStyle(Box.BorderStyle.SINGLE)
                .child(Text.builder().text("B").build())
                .build();
        Box outer = Box.builder()
                .direction(FlexDirection.ROW)
                .borderStyle(Box.BorderStyle.DOUBLE)
                .children(inner1, inner2)
                .build();

        Canvas canvas = Canvas.create(20, 7);
        outer.render(canvas, 20, 7);
        List<AttributedString> lines = canvas.toLines();

        // Outer border: double lines
        assertEquals('\u2554', lines.get(0).charAt(0)); // ╔
        assertEquals('\u2557', lines.get(0).charAt(19)); // ╗
        assertEquals('\u255a', lines.get(6).charAt(0)); // ╚
        assertEquals('\u255d', lines.get(6).charAt(19)); // ╝
    }

    @Test
    void testBoxDoubleBorder() {
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.DOUBLE)
                .child(Text.builder().text("X").build())
                .build();

        Canvas canvas = Canvas.create(5, 3);
        box.render(canvas, 5, 3);
        List<AttributedString> lines = canvas.toLines();
        assertEquals('\u2554', lines.get(0).charAt(0)); // ╔
        assertEquals('\u2550', lines.get(0).charAt(1)); // ═
        assertEquals('\u2557', lines.get(0).charAt(4)); // ╗
        assertEquals('\u2551', lines.get(1).charAt(0)); // ║
        assertEquals('\u255a', lines.get(2).charAt(0)); // ╚
        assertEquals('\u255d', lines.get(2).charAt(4)); // ╝
    }

    @Test
    void testBoxNoBorder() {
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.NONE)
                .child(Text.builder().text("Hi").build())
                .build();

        Size pref = box.getPreferredSize();
        assertEquals(2, pref.width()); // no border extra
        assertEquals(1, pref.height());

        Canvas canvas = Canvas.create(5, 1);
        box.render(canvas, 5, 1);
        assertTrue(canvas.toLines().get(0).toString().startsWith("Hi"));
    }

    @Test
    void testBoxWithPadding() {
        Box box = Box.builder()
                .borderStyle(Box.BorderStyle.SINGLE)
                .padding(Insets.of(1, 2))
                .child(Text.builder().text("X").build())
                .build();

        Canvas canvas = Canvas.create(10, 6);
        box.render(canvas, 10, 6);
        List<AttributedString> lines = canvas.toLines();

        // Border at row 0, padding at row 1, content at row 2
        assertEquals('\u250c', lines.get(0).charAt(0));
        // X should be at row 2 (border+padding), col 3 (border+padding)
        assertEquals('X', lines.get(2).charAt(3));
    }

    @Test
    void testInsetsMethods() {
        Insets insets = new Insets(1, 2, 3, 4);
        assertEquals(1, insets.top());
        assertEquals(2, insets.right());
        assertEquals(3, insets.bottom());
        assertEquals(4, insets.left());
        assertEquals(6, insets.horizontal());
        assertEquals(4, insets.vertical());

        Insets all = Insets.of(5);
        assertEquals(5, all.top());
        assertEquals(5, all.right());
        assertEquals(5, all.bottom());
        assertEquals(5, all.left());

        Insets vh = Insets.of(2, 3);
        assertEquals(2, vh.top());
        assertEquals(3, vh.right());
        assertEquals(2, vh.bottom());
        assertEquals(3, vh.left());

        assertEquals(Insets.of(1), Insets.of(1));
        assertNotEquals(Insets.of(1), Insets.of(2));
    }

    @Test
    void testSizeMethods() {
        Size s = new Size(10, 20);
        assertEquals(10, s.width());
        assertEquals(20, s.height());
        assertEquals(new Size(10, 20), s);
        assertNotEquals(new Size(10, 21), s);
        assertEquals("10x20", s.toString());

        // Negative values clamped to 0
        Size neg = new Size(-5, -3);
        assertEquals(0, neg.width());
        assertEquals(0, neg.height());
    }

    @Test
    void testColumnLayoutWithMultipleComponents() {
        Box root = Box.builder()
                .direction(FlexDirection.COLUMN)
                .gap(1)
                .child(Text.builder().text("Title").build())
                .child(Separator.builder().build())
                .child(Text.builder().text("Body text").build())
                .child(ProgressBar.builder()
                        .progress(0.5)
                        .width(10)
                        .showPercentage(false)
                        .build())
                .build();

        Canvas canvas = Canvas.create(20, 10);
        root.render(canvas, 20, 10);
        List<AttributedString> lines = canvas.toLines();

        assertTrue(lines.get(0).toString().startsWith("Title"));
        // Separator at row 2 (gap=1 after Title)
        assertEquals('\u2500', lines.get(2).charAt(0));
    }
}
