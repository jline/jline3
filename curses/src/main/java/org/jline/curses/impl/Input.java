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
import java.util.function.Predicate;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.curses.Theme;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A single-line text input component.
 *
 * <p>Input provides a text input field with support for:
 * <ul>
 * <li>Single-line text editing</li>
 * <li>Cursor positioning and navigation</li>
 * <li>Text selection</li>
 * <li>Input validation</li>
 * <li>Placeholder text</li>
 * <li>Password mode</li>
 * </ul>
 * </p>
 */
public class Input extends AbstractComponent {

    private String text = "";
    private String placeholder = "";
    private int cursorPosition = 0;
    private int scrollOffset = 0;
    private boolean passwordMode = false;
    private char passwordChar = '*';
    private boolean editable = true;

    // Selection
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean isSelecting = false;

    // Validation
    private Predicate<String> validator;
    private List<Runnable> changeListeners = new ArrayList<>();

    // Styling - will be initialized from theme in setTheme()
    private AttributedStyle normalStyle = AttributedStyle.DEFAULT;
    private AttributedStyle focusedStyle = AttributedStyle.DEFAULT.inverse();
    private AttributedStyle placeholderStyle =
            AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK + AttributedStyle.BRIGHT);
    private AttributedStyle selectionStyle = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);

    public Input() {}

    public Input(String text) {
        this.text = text != null ? text : "";
        this.cursorPosition = this.text.length();
    }

    @Override
    public void setTheme(Theme theme) {
        super.setTheme(theme);
        if (theme != null) {
            // Initialize styles from theme
            normalStyle = theme.getStyle(".input.normal");
            focusedStyle = theme.getStyle(".input.focused");
            placeholderStyle = theme.getStyle(".input.placeholder");
            selectionStyle = theme.getStyle(".input.selection");
        }
    }

    /**
     * Gets the input text.
     *
     * @return the input text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the input text.
     *
     * @param text the text to set
     */
    public void setText(String text) {
        String newText = text != null ? text : "";
        if (!newText.equals(this.text)) {
            this.text = newText;
            this.cursorPosition = Math.min(cursorPosition, this.text.length());
            clearSelection();
            ensureCursorVisible();
            notifyChangeListeners();
        }
    }

    /**
     * Gets the placeholder text.
     *
     * @return the placeholder text
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the placeholder text.
     *
     * @param placeholder the placeholder text to set
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
    }

    /**
     * Gets the cursor position.
     *
     * @return the cursor position
     */
    public int getCursorPosition() {
        return cursorPosition;
    }

    /**
     * Sets the cursor position.
     *
     * @param position the position to set
     */
    public void setCursorPosition(int position) {
        this.cursorPosition = Math.max(0, Math.min(position, text.length()));
        if (!isSelecting) {
            clearSelection();
        }
        ensureCursorVisible();
    }

    /**
     * Gets whether password mode is enabled.
     *
     * @return true if password mode is enabled
     */
    public boolean isPasswordMode() {
        return passwordMode;
    }

    /**
     * Sets whether password mode is enabled.
     *
     * @param passwordMode true to enable password mode
     */
    public void setPasswordMode(boolean passwordMode) {
        this.passwordMode = passwordMode;
    }

    /**
     * Gets the password character.
     *
     * @return the password character
     */
    public char getPasswordChar() {
        return passwordChar;
    }

    /**
     * Sets the password character.
     *
     * @param passwordChar the password character to set
     */
    public void setPasswordChar(char passwordChar) {
        this.passwordChar = passwordChar;
    }

    /**
     * Gets whether the input is editable.
     *
     * @return true if editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets whether the input is editable.
     *
     * @param editable true to make editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Sets the input validator.
     *
     * @param validator the validator to set
     */
    public void setValidator(Predicate<String> validator) {
        this.validator = validator;
    }

    /**
     * Adds a change listener.
     *
     * @param listener the listener to add
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * Removes a change listener.
     *
     * @param listener the listener to remove
     */
    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    /**
     * Inserts text at the current cursor position.
     *
     * @param insertText the text to insert
     */
    public void insertText(String insertText) {
        if (!editable || insertText == null) {
            return;
        }

        // Remove any newlines for single-line input
        insertText = insertText.replace("\n", "").replace("\r", "");

        if (hasSelection()) {
            deleteSelection();
        }

        String newText = text.substring(0, cursorPosition) + insertText + text.substring(cursorPosition);

        if (validator == null || validator.test(newText)) {
            text = newText;
            cursorPosition += insertText.length();
            ensureCursorVisible();
            notifyChangeListeners();
        }
    }

    /**
     * Deletes the character before the cursor.
     */
    public void deleteCharBefore() {
        if (!editable) {
            return;
        }

        if (hasSelection()) {
            deleteSelection();
        } else if (cursorPosition > 0) {
            String newText = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
            if (validator == null || validator.test(newText)) {
                text = newText;
                cursorPosition--;
                ensureCursorVisible();
                notifyChangeListeners();
            }
        }
    }

    /**
     * Deletes the character after the cursor.
     */
    public void deleteCharAfter() {
        if (!editable) {
            return;
        }

        if (hasSelection()) {
            deleteSelection();
        } else if (cursorPosition < text.length()) {
            String newText = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            if (validator == null || validator.test(newText)) {
                text = newText;
                notifyChangeListeners();
            }
        }
    }

    /**
     * Moves the cursor left.
     */
    public void moveCursorLeft() {
        if (cursorPosition > 0) {
            cursorPosition--;
            ensureCursorVisible();
        }
    }

    /**
     * Moves the cursor right.
     */
    public void moveCursorRight() {
        if (cursorPosition < text.length()) {
            cursorPosition++;
            ensureCursorVisible();
        }
    }

    /**
     * Moves the cursor to the beginning.
     */
    public void moveCursorToStart() {
        cursorPosition = 0;
        ensureCursorVisible();
    }

    /**
     * Moves the cursor to the end.
     */
    public void moveCursorToEnd() {
        cursorPosition = text.length();
        ensureCursorVisible();
    }

    /**
     * Starts a selection at the current cursor position.
     */
    public void startSelection() {
        selectionStart = cursorPosition;
        selectionEnd = cursorPosition;
        isSelecting = true;
    }

    /**
     * Extends the selection to the current cursor position.
     */
    public void extendSelection() {
        if (selectionStart != -1) {
            selectionEnd = cursorPosition;
            isSelecting = false; // End selection mode
        }
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        isSelecting = false;
    }

    /**
     * Checks if there is an active selection.
     *
     * @return true if there is a selection
     */
    public boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    /**
     * Gets the selected text.
     *
     * @return the selected text
     */
    public String getSelectedText() {
        if (!hasSelection()) {
            return "";
        }

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return text.substring(start, end);
    }

    /**
     * Deletes the selected text.
     */
    public void deleteSelection() {
        if (!hasSelection() || !editable) {
            return;
        }

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        String newText = text.substring(0, start) + text.substring(end);
        if (validator == null || validator.test(newText)) {
            text = newText;
            cursorPosition = start;
            clearSelection();
            ensureCursorVisible();
            notifyChangeListeners();
        }
    }

    /**
     * Selects all text.
     */
    public void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
    }

    /**
     * Ensures the cursor is visible by adjusting scroll offset.
     */
    private void ensureCursorVisible() {
        Size size = getSize();
        if (size == null) {
            return;
        }

        int width = size.w();
        if (width <= 0) {
            return;
        }

        // Adjust scroll offset to keep cursor visible
        if (cursorPosition < scrollOffset) {
            scrollOffset = cursorPosition;
        } else if (cursorPosition >= scrollOffset + width) {
            scrollOffset = cursorPosition - width + 1;
        }
    }

    /**
     * Notifies all change listeners.
     */
    private void notifyChangeListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("Error in input change listener: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        if (size == null) {
            return;
        }

        Position pos = getScreenPosition();
        if (pos == null) {
            return;
        }

        int width = size.w();
        int height = size.h();

        // Clear the input area
        AttributedStyle baseStyle = isFocused() ? focusedStyle : normalStyle;
        screen.fill(pos.x(), pos.y(), width, height, baseStyle);

        // Determine what text to display
        String displayText;
        if (text.isEmpty() && !placeholder.isEmpty() && !isFocused()) {
            displayText = placeholder;
            baseStyle = placeholderStyle;
        } else if (passwordMode) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                sb.append(passwordChar);
            }
            displayText = sb.toString();
        } else {
            displayText = text;
        }

        // Draw visible portion of text
        if (!displayText.isEmpty() && width > 0) {
            int visibleStart = Math.max(0, scrollOffset);
            int visibleEnd = Math.min(displayText.length(), scrollOffset + width);

            if (visibleStart < visibleEnd) {
                String visibleText = displayText.substring(visibleStart, visibleEnd);

                // Apply selection highlighting
                if (hasSelection() && !passwordMode) {
                    drawTextWithSelection(screen, visibleText, visibleStart, baseStyle, pos);
                } else {
                    AttributedString attributedText = new AttributedString(visibleText, baseStyle);
                    screen.text(pos.x(), pos.y(), attributedText);
                }
            }
        }

        // Draw cursor if focused
        if (isFocused() && cursorPosition >= scrollOffset && cursorPosition < scrollOffset + width) {
            int cursorX = cursorPosition - scrollOffset;
            char cursorChar = ' ';
            if (cursorPosition < displayText.length()) {
                cursorChar = displayText.charAt(cursorPosition);
            }

            AttributedString cursorStr = new AttributedString(String.valueOf(cursorChar), baseStyle.inverse());
            screen.text(pos.x() + cursorX, pos.y(), cursorStr);
        }
    }

    /**
     * Draws text with selection highlighting.
     */
    private void drawTextWithSelection(
            Screen screen, String visibleText, int visibleStart, AttributedStyle baseStyle, Position pos) {
        int selStart = Math.min(selectionStart, selectionEnd);
        int selEnd = Math.max(selectionStart, selectionEnd);

        for (int i = 0; i < visibleText.length(); i++) {
            int textPos = visibleStart + i;
            char ch = visibleText.charAt(i);

            AttributedStyle style = (textPos >= selStart && textPos < selEnd) ? selectionStyle : baseStyle;
            AttributedString charStr = new AttributedString(String.valueOf(ch), style);
            screen.text(pos.x() + i, pos.y(), charStr);
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        return new Size(20, 1); // Standard single-line input size
    }
}
