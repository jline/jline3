/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import org.jline.curses.Position;
import org.jline.curses.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for component rendering to VirtualScreen.
 */
public class ComponentRenderingTest {

    @Test
    public void testLabelLeftAlignedRendering() {
        Label label = new Label("Hello");
        label.setPosition(new Position(0, 0));
        label.setSize(new Size(20, 1));
        label.setAlignment(Label.Alignment.LEFT);

        VirtualScreen screen = new VirtualScreen(20, 1);
        label.doDraw(screen);

        assertEquals('H', screen.getChar(0, 0));
        assertEquals('e', screen.getChar(1, 0));
        assertEquals('l', screen.getChar(2, 0));
        assertEquals('l', screen.getChar(3, 0));
        assertEquals('o', screen.getChar(4, 0));
    }

    @Test
    public void testLabelCenterAlignedRendering() {
        Label label = new Label("Hi");
        label.setPosition(new Position(0, 0));
        label.setSize(new Size(10, 1));
        label.setAlignment(Label.Alignment.CENTER);

        VirtualScreen screen = new VirtualScreen(10, 1);
        label.doDraw(screen);

        // "Hi" centered in 10 chars -> offset = (10-2)/2 = 4
        assertEquals('H', screen.getChar(4, 0));
        assertEquals('i', screen.getChar(5, 0));
        // Before the text should be spaces
        assertEquals(' ', screen.getChar(3, 0));
    }

    @Test
    public void testLabelRightAlignedRendering() {
        Label label = new Label("AB");
        label.setPosition(new Position(0, 0));
        label.setSize(new Size(10, 1));
        label.setAlignment(Label.Alignment.RIGHT);

        VirtualScreen screen = new VirtualScreen(10, 1);
        label.doDraw(screen);

        // "AB" right-aligned in 10 chars -> offset = 10-2 = 8
        assertEquals('A', screen.getChar(8, 0));
        assertEquals('B', screen.getChar(9, 0));
    }

    @Test
    public void testLabelMultilineRendering() {
        Label label = new Label("Line1\nLine2");
        label.setPosition(new Position(0, 0));
        label.setSize(new Size(10, 2));

        VirtualScreen screen = new VirtualScreen(10, 2);
        label.doDraw(screen);

        assertEquals('L', screen.getChar(0, 0));
        assertEquals('L', screen.getChar(0, 1));
        assertEquals('2', screen.getChar(4, 1));
    }

    @Test
    public void testLabelEmptyText() {
        Label label = new Label("");
        label.setPosition(new Position(0, 0));
        label.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        label.doDraw(screen);

        // All spaces when empty
        assertEquals(' ', screen.getChar(0, 0));
    }

    @Test
    public void testLabelPreferredSize() {
        Label label = new Label("Hello World");
        Size size = label.doGetPreferredSize();
        assertEquals(11, size.w());
        assertEquals(1, size.h());
    }

    @Test
    public void testLabelMultilinePreferredSize() {
        Label label = new Label("Hello\nWorld!");
        Size size = label.doGetPreferredSize();
        assertEquals(6, size.w()); // "World!" is the longest line
        assertEquals(2, size.h());
    }

    @Test
    public void testInputPreferredSize() {
        Input input = new Input("test");
        Size size = input.doGetPreferredSize();
        assertTrue(size.w() > 0);
        assertEquals(1, size.h());
    }

    @Test
    public void testInputTextManipulation() {
        Input input = new Input("Hello");
        assertEquals("Hello", input.getText());
        assertEquals(5, input.getCursorPosition());

        input.setCursorPosition(5);
        input.insertText(" World");
        assertEquals("Hello World", input.getText());
    }

    @Test
    public void testInputDeleteCharBefore() {
        Input input = new Input("Hello");
        input.setCursorPosition(5);
        input.deleteCharBefore();
        assertEquals("Hell", input.getText());
        assertEquals(4, input.getCursorPosition());
    }

    @Test
    public void testInputDeleteCharAfter() {
        Input input = new Input("Hello");
        input.setCursorPosition(0);
        input.deleteCharAfter();
        assertEquals("ello", input.getText());
        assertEquals(0, input.getCursorPosition());
    }

    @Test
    public void testInputCursorMovement() {
        Input input = new Input("Hello");
        input.setCursorPosition(2);

        input.moveCursorLeft();
        assertEquals(1, input.getCursorPosition());

        input.moveCursorRight();
        assertEquals(2, input.getCursorPosition());

        input.moveCursorToStart();
        assertEquals(0, input.getCursorPosition());

        input.moveCursorToEnd();
        assertEquals(5, input.getCursorPosition());
    }

    @Test
    public void testInputSelection() {
        Input input = new Input("Hello World");
        input.selectAll();
        assertTrue(input.hasSelection());
        assertEquals("Hello World", input.getSelectedText());

        input.clearSelection();
        assertFalse(input.hasSelection());
    }

    @Test
    public void testInputPasswordMode() {
        Input input = new Input("secret");
        input.setPasswordMode(true);
        assertTrue(input.isPasswordMode());
        assertEquals('*', input.getPasswordChar());

        input.setPasswordChar('#');
        assertEquals('#', input.getPasswordChar());
    }

    @Test
    public void testInputPlaceholder() {
        Input input = new Input();
        input.setPlaceholder("Enter text...");
        assertEquals("Enter text...", input.getPlaceholder());
    }

    @Test
    public void testInputChangeListener() {
        Input input = new Input("Hello");
        boolean[] changed = {false};
        input.addChangeListener(() -> changed[0] = true);
        input.insertText("!");
        assertTrue(changed[0]);
    }

    @Test
    public void testButtonPreferredSize() {
        Button button = new Button("OK");
        Size size = button.doGetPreferredSize();
        assertTrue(size.w() >= 6); // "OK" + padding
        assertEquals(3, size.h());
    }

    @Test
    public void testButtonClickListener() {
        Button button = new Button("Click Me");
        boolean[] clicked = {false};
        button.addClickListener(() -> clicked[0] = true);
        button.click();
        assertTrue(clicked[0]);
    }

    @Test
    public void testButtonRemoveClickListener() {
        Button button = new Button("Click Me");
        int[] count = {0};
        Runnable listener = () -> count[0]++;
        button.addClickListener(listener);
        button.click();
        assertEquals(1, count[0]);

        button.removeClickListener(listener);
        button.click();
        assertEquals(1, count[0]); // Still 1 since listener was removed
    }

    @Test
    public void testButtonRendering() {
        Button button = new Button("OK");
        button.setPosition(new Position(0, 0));
        button.setSize(new Size(10, 3));

        VirtualScreen screen = new VirtualScreen(10, 3);
        button.doDraw(screen);

        // Border corners should be drawn
        assertEquals('\u250c', screen.getChar(0, 0)); // top-left corner
        assertEquals('\u2510', screen.getChar(9, 0)); // top-right corner
        assertEquals('\u2514', screen.getChar(0, 2)); // bottom-left corner
        assertEquals('\u2518', screen.getChar(9, 2)); // bottom-right corner
    }

    @Test
    public void testButtonPressedState() {
        Button button = new Button("OK");
        assertFalse(button.isPressed());
        button.setPressed(true);
        assertTrue(button.isPressed());
    }
}
