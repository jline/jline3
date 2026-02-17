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
import org.jline.components.ui.Text;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LayoutEngineTest {

    @Test
    void testColumnLayout() {
        Text t1 = Text.builder().text("Line 1").build();
        Text t2 = Text.builder().text("Line 2").build();
        List<Component> children = Arrays.asList(t1, t2);

        Canvas canvas = Canvas.create(20, 5);
        LayoutEngine.layout(
                canvas, children, 20, 5, FlexDirection.COLUMN, FlexJustify.START, FlexAlign.START, 0, Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        assertTrue(lines.get(0).toString().startsWith("Line 1"));
        assertTrue(lines.get(1).toString().startsWith("Line 2"));
    }

    @Test
    void testRowLayout() {
        Text t1 = Text.builder().text("A").build();
        Text t2 = Text.builder().text("B").build();
        List<Component> children = Arrays.asList(t1, t2);

        Canvas canvas = Canvas.create(10, 1);
        LayoutEngine.layout(
                canvas, children, 10, 1, FlexDirection.ROW, FlexJustify.START, FlexAlign.START, 0, Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        String row = lines.get(0).toString();
        assertTrue(row.contains("A"));
        assertTrue(row.contains("B"));
    }

    @Test
    void testGap() {
        Text t1 = Text.builder().text("A").build();
        Text t2 = Text.builder().text("B").build();
        List<Component> children = Arrays.asList(t1, t2);

        Canvas canvas = Canvas.create(20, 5);
        LayoutEngine.layout(
                canvas, children, 20, 5, FlexDirection.COLUMN, FlexJustify.START, FlexAlign.START, 1, Insets.NONE);

        List<AttributedString> lines = canvas.toLines();
        assertTrue(lines.get(0).toString().startsWith("A"));
        // With gap=1, t2 should be on row 2 (row 1 is the gap)
        assertTrue(lines.get(2).toString().startsWith("B"));
    }

    @Test
    void testPadding() {
        Text t1 = Text.builder().text("X").build();
        List<Component> children = Arrays.asList(t1);

        Canvas canvas = Canvas.create(10, 5);
        LayoutEngine.layout(
                canvas,
                children,
                10,
                5,
                FlexDirection.COLUMN,
                FlexJustify.START,
                FlexAlign.START,
                0,
                new Insets(1, 2, 1, 2));

        List<AttributedString> lines = canvas.toLines();
        // Row 0 should be empty (top padding)
        assertEquals("          ", lines.get(0).toString());
        // Row 1 should have X at column 2 (left padding)
        assertEquals('X', lines.get(1).charAt(2));
    }

    @Test
    void testJustifySpaceAround() {
        // 3 children of width 1 in a 10-wide ROW with gap=0
        // Free space = 10 - 3 = 7
        // outerGap = 7 / (3*2) = 1
        // extraGap = 1 * 2 = 2
        // Positions: child0 at 1, child1 at 1+1+0+2=4, child2 at 4+1+0+2=7
        Text t1 = Text.builder().text("A").build();
        Text t2 = Text.builder().text("B").build();
        Text t3 = Text.builder().text("C").build();
        Canvas canvas = Canvas.create(10, 1);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t1, t2, t3),
                10,
                1,
                FlexDirection.ROW,
                FlexJustify.SPACE_AROUND,
                FlexAlign.START,
                0,
                Insets.NONE);

        String row = canvas.toLines().get(0).toString();
        int posA = row.indexOf('A');
        int posB = row.indexOf('B');
        int posC = row.indexOf('C');

        // Verify spacing is roughly equal around each child
        assertTrue(posA > 0, "A should not be at the edge");
        // Space before A should be ~equal to space between A-B / 2
        int spaceBefore = posA;
        int spaceBetweenAB = posB - posA - 1;
        int spaceBetweenBC = posC - posB - 1;
        int spaceAfter = 9 - posC;
        // Between-child gap should be ~2x the outer gap
        assertEquals(spaceBefore, spaceAfter, 1);
        assertEquals(spaceBetweenAB, spaceBetweenBC, 1);
    }

    @Test
    void testJustifySpaceBetweenSingleChild() {
        // Single child with SPACE_BETWEEN should align to START
        Text t = Text.builder().text("X").build();
        Canvas canvas = Canvas.create(10, 1);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(t),
                10,
                1,
                FlexDirection.ROW,
                FlexJustify.SPACE_BETWEEN,
                FlexAlign.START,
                0,
                Insets.NONE);

        String row = canvas.toLines().get(0).toString();
        assertEquals('X', row.charAt(0));
    }

    @Test
    void testEmptyChildren() {
        Canvas canvas = Canvas.create(10, 5);
        LayoutEngine.layout(
                canvas,
                Arrays.asList(),
                10,
                5,
                FlexDirection.COLUMN,
                FlexJustify.START,
                FlexAlign.START,
                0,
                Insets.NONE);

        // Should not throw, canvas should remain blank
        List<AttributedString> lines = canvas.toLines();
        assertEquals(5, lines.size());
    }
}
