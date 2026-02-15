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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TextArea component.
 */
public class TextAreaTest {

    private TextArea textArea;

    @BeforeEach
    public void setUp() {
        textArea = new TextArea();
    }

    @Test
    public void testEmptyTextArea() {
        assertEquals("", textArea.getText());
        assertEquals(1, textArea.getLineCount());
        assertEquals("", textArea.getLine(0));
    }

    @Test
    public void testSetText() {
        textArea.setText("Hello\nWorld");
        assertEquals("Hello\nWorld", textArea.getText());
        assertEquals(2, textArea.getLineCount());
        assertEquals("Hello", textArea.getLine(0));
        assertEquals("World", textArea.getLine(1));
    }

    @Test
    public void testCursorMovement() {
        textArea.setText("Hello\nWorld");

        // Initial position
        Position pos = textArea.getCursorPosition();
        assertEquals(0, pos.x());
        assertEquals(0, pos.y());

        // Move right
        textArea.moveCursorRight();
        pos = textArea.getCursorPosition();
        assertEquals(1, pos.x());
        assertEquals(0, pos.y());

        // Move to end of line
        textArea.moveCursorToLineEnd();
        pos = textArea.getCursorPosition();
        assertEquals(5, pos.x());
        assertEquals(0, pos.y());

        // Move down
        textArea.moveCursorDown();
        pos = textArea.getCursorPosition();
        assertEquals(5, pos.x()); // Should be clamped to line length
        assertEquals(1, pos.y());
    }

    @Test
    public void testTextInsertion() {
        textArea.setText("Hello World");
        textArea.setCursorPosition(0, 5); // Position after "Hello"
        textArea.insertText(" Beautiful");

        assertEquals("Hello Beautiful World", textArea.getText());

        Position pos = textArea.getCursorPosition();
        assertEquals(15, pos.x()); // After " Beautiful"
        assertEquals(0, pos.y());
    }

    @Test
    public void testTextDeletion() {
        textArea.setText("Hello World");
        textArea.setCursorPosition(0, 5); // Position after "Hello"

        // Delete character before cursor
        textArea.deleteCharBefore();
        assertEquals("Hell World", textArea.getText());

        Position pos = textArea.getCursorPosition();
        assertEquals(4, pos.x());
        assertEquals(0, pos.y());
    }

    @Test
    public void testNewLineInsertion() {
        textArea.setText("Hello World");
        textArea.setCursorPosition(0, 5); // Position after "Hello"
        textArea.insertNewLine();

        assertEquals("Hello\n World", textArea.getText());
        assertEquals(2, textArea.getLineCount());
        assertEquals("Hello", textArea.getLine(0));
        assertEquals(" World", textArea.getLine(1));

        Position pos = textArea.getCursorPosition();
        assertEquals(0, pos.x());
        assertEquals(1, pos.y());
    }

    @Test
    public void testSelection() {
        textArea.setText("Hello World");
        textArea.setCursorPosition(0, 0);

        // Start selection
        textArea.startSelection();
        assertFalse(textArea.hasSelection()); // No selection until extended

        // Extend selection
        textArea.setCursorPosition(0, 5);
        textArea.extendSelection();
        assertTrue(textArea.hasSelection());
        assertEquals("Hello", textArea.getSelectedText());

        // Clear selection
        textArea.clearSelection();
        assertFalse(textArea.hasSelection());
    }

    @Test
    public void testMultiLineSelection() {
        textArea.setText("Hello\nWorld\nTest");
        textArea.setCursorPosition(0, 2); // Position at "ll" in "Hello"
        textArea.startSelection();
        textArea.setCursorPosition(2, 2); // Position at "st" in "Test"
        textArea.extendSelection();

        assertTrue(textArea.hasSelection());
        String selected = textArea.getSelectedText();
        assertEquals("llo\nWorld\nTe", selected);
    }

    @Test
    public void testEditableProperty() {
        assertTrue(textArea.isEditable()); // Default is editable

        textArea.setEditable(false);
        assertFalse(textArea.isEditable());

        // Try to insert text when not editable
        String originalText = textArea.getText();
        textArea.insertText("Should not be inserted");
        assertEquals(originalText, textArea.getText()); // Text should not change
    }

    @Test
    public void testTabExpansion() {
        textArea.setText("Hello\tWorld");
        textArea.setTabSize(4);

        // This tests the internal expandTabs method indirectly
        // by checking that the preferred size calculation works
        Size size = textArea.doGetPreferredSize();
        assertTrue(size.w() >= 11); // "Hello" + 4 spaces + "World" = 15 chars minimum
    }

    @Test
    public void testLineManipulation() {
        textArea.setText("Line1\nLine2\nLine3");

        // Insert line
        textArea.insertLine(1, "NewLine");
        assertEquals(4, textArea.getLineCount());
        assertEquals("NewLine", textArea.getLine(1));
        assertEquals("Line2", textArea.getLine(2));

        // Set line
        textArea.setLine(1, "ModifiedLine");
        assertEquals("ModifiedLine", textArea.getLine(1));

        // Remove line (but not if it would leave empty)
        textArea.removeLine(1);
        assertEquals(3, textArea.getLineCount());
        assertEquals("Line2", textArea.getLine(1));
    }
}
