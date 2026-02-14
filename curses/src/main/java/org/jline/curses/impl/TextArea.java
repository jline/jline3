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

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.curses.Theme;
import org.jline.terminal.KeyEvent;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * A multi-line text editing component.
 *
 * <p>TextArea provides a scrollable, editable text area with support for:
 * <ul>
 * <li>Multi-line text editing</li>
 * <li>Cursor positioning and navigation</li>
 * <li>Text selection</li>
 * <li>Scrolling for large content</li>
 * <li>Word wrapping (optional)</li>
 * </ul>
 * </p>
 */
public class TextArea extends AbstractComponent {

    private final List<String> lines;
    private int cursorRow;
    private int cursorCol;
    private int scrollRow;
    private int scrollCol;
    private boolean editable = true;
    private boolean wordWrap = false;
    private int tabSize = 4;
    // Styling - will be initialized from theme in setTheme()
    private AttributedStyle normalStyle = AttributedStyle.DEFAULT;
    private AttributedStyle cursorStyle = AttributedStyle.DEFAULT.inverse();
    private AttributedStyle selectionStyle = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);

    // Selection state
    private Position selectionStart;
    private Position selectionEnd;

    public TextArea() {
        this("");
    }

    public TextArea(String text) {
        this.lines = new ArrayList<>();
        setText(text);
    }

    @Override
    public void setTheme(Theme theme) {
        super.setTheme(theme);
        if (theme != null) {
            // Initialize styles from theme
            normalStyle = theme.getStyle(".textarea.normal");
            cursorStyle = theme.getStyle(".textarea.cursor");
            selectionStyle = theme.getStyle(".input.selection"); // Reuse input selection style
        }
    }

    /**
     * Sets the text content of the text area.
     *
     * @param text the text to set
     */
    public void setText(String text) {
        lines.clear();
        if (text == null || text.isEmpty()) {
            lines.add("");
        } else {
            String[] textLines = text.split("\n", -1);
            for (String line : textLines) {
                lines.add(line);
            }
        }
        cursorRow = 0;
        cursorCol = 0;
        scrollRow = 0;
        scrollCol = 0;
        clearSelection();
    }

    /**
     * Gets the text content of the text area.
     *
     * @return the text content
     */
    public String getText() {
        return String.join("\n", lines);
    }

    /**
     * Gets the number of lines in the text area.
     *
     * @return the number of lines
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Gets the text of a specific line.
     *
     * @param lineIndex the line index (0-based)
     * @return the line text, or empty string if index is out of bounds
     */
    public String getLine(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return "";
        }
        return lines.get(lineIndex);
    }

    /**
     * Sets the text of a specific line.
     *
     * @param lineIndex the line index (0-based)
     * @param text the text to set
     */
    public void setLine(int lineIndex, String text) {
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            lines.set(lineIndex, text != null ? text : "");
        }
    }

    /**
     * Inserts a line at the specified index.
     *
     * @param lineIndex the line index (0-based)
     * @param text the text to insert
     */
    public void insertLine(int lineIndex, String text) {
        if (lineIndex >= 0 && lineIndex <= lines.size()) {
            lines.add(lineIndex, text != null ? text : "");
        }
    }

    /**
     * Removes a line at the specified index.
     *
     * @param lineIndex the line index (0-based)
     */
    public void removeLine(int lineIndex) {
        if (lineIndex >= 0 && lineIndex < lines.size() && lines.size() > 1) {
            lines.remove(lineIndex);
            if (cursorRow >= lines.size()) {
                cursorRow = lines.size() - 1;
            }
        }
    }

    /**
     * Gets the current cursor position.
     *
     * @return the cursor position
     */
    public Position getCursorPosition() {
        return new Position(cursorCol, cursorRow);
    }

    /**
     * Sets the cursor position.
     *
     * @param row the row position (0-based)
     * @param col the column position (0-based)
     */
    public void setCursorPosition(int row, int col) {
        cursorRow = Math.max(0, Math.min(row, lines.size() - 1));
        String currentLine = getLine(cursorRow);
        cursorCol = Math.max(0, Math.min(col, currentLine.length()));
        ensureCursorVisible();
    }

    /**
     * Moves the cursor up by one line.
     */
    public void moveCursorUp() {
        if (cursorRow > 0) {
            cursorRow--;
            String currentLine = getLine(cursorRow);
            cursorCol = Math.min(cursorCol, currentLine.length());
            ensureCursorVisible();
        }
    }

    /**
     * Moves the cursor down by one line.
     */
    public void moveCursorDown() {
        if (cursorRow < lines.size() - 1) {
            cursorRow++;
            String currentLine = getLine(cursorRow);
            cursorCol = Math.min(cursorCol, currentLine.length());
            ensureCursorVisible();
        }
    }

    /**
     * Moves the cursor left by one character.
     */
    public void moveCursorLeft() {
        if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorRow > 0) {
            cursorRow--;
            cursorCol = getLine(cursorRow).length();
        }
        ensureCursorVisible();
    }

    /**
     * Moves the cursor right by one character.
     */
    public void moveCursorRight() {
        String currentLine = getLine(cursorRow);
        if (cursorCol < currentLine.length()) {
            cursorCol++;
        } else if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = 0;
        }
        ensureCursorVisible();
    }

    /**
     * Moves the cursor to the beginning of the current line.
     */
    public void moveCursorToLineStart() {
        cursorCol = 0;
        ensureCursorVisible();
    }

    /**
     * Moves the cursor to the end of the current line.
     */
    public void moveCursorToLineEnd() {
        cursorCol = getLine(cursorRow).length();
        ensureCursorVisible();
    }

    /**
     * Inserts text at the current cursor position.
     *
     * @param text the text to insert
     */
    public void insertText(String text) {
        if (!editable || text == null) {
            return;
        }

        if (hasSelection()) {
            deleteSelection();
        }

        String currentLine = getLine(cursorRow);
        String before = currentLine.substring(0, cursorCol);
        String after = currentLine.substring(cursorCol);

        if (text.contains("\n")) {
            // Multi-line insertion
            String[] textLines = text.split("\n", -1);

            // Update current line with first part
            setLine(cursorRow, before + textLines[0]);

            // Insert middle lines
            for (int i = 1; i < textLines.length - 1; i++) {
                insertLine(cursorRow + i, textLines[i]);
            }

            // Insert last line with remaining text
            if (textLines.length > 1) {
                insertLine(cursorRow + textLines.length - 1, textLines[textLines.length - 1] + after);
                cursorRow += textLines.length - 1;
                cursorCol = textLines[textLines.length - 1].length();
            } else {
                cursorCol += textLines[0].length();
            }
        } else {
            // Single line insertion
            setLine(cursorRow, before + text + after);
            cursorCol += text.length();
        }

        ensureCursorVisible();
    }

    /**
     * Inserts a character at the current cursor position.
     *
     * @param ch the character to insert
     */
    public void insertChar(char ch) {
        insertText(String.valueOf(ch));
    }

    /**
     * Deletes the character before the cursor (backspace).
     */
    public void deleteCharBefore() {
        if (!editable) {
            return;
        }

        if (hasSelection()) {
            deleteSelection();
            return;
        }

        if (cursorCol > 0) {
            // Delete character in current line
            String currentLine = getLine(cursorRow);
            String newLine = currentLine.substring(0, cursorCol - 1) + currentLine.substring(cursorCol);
            setLine(cursorRow, newLine);
            cursorCol--;
        } else if (cursorRow > 0) {
            // Join with previous line
            String currentLine = getLine(cursorRow);
            String previousLine = getLine(cursorRow - 1);
            setLine(cursorRow - 1, previousLine + currentLine);
            removeLine(cursorRow);
            cursorRow--;
            cursorCol = previousLine.length();
        }

        ensureCursorVisible();
    }

    /**
     * Deletes the character after the cursor (delete).
     */
    public void deleteCharAfter() {
        if (!editable) {
            return;
        }

        if (hasSelection()) {
            deleteSelection();
            return;
        }

        String currentLine = getLine(cursorRow);
        if (cursorCol < currentLine.length()) {
            // Delete character in current line
            String newLine = currentLine.substring(0, cursorCol) + currentLine.substring(cursorCol + 1);
            setLine(cursorRow, newLine);
        } else if (cursorRow < lines.size() - 1) {
            // Join with next line
            String nextLine = getLine(cursorRow + 1);
            setLine(cursorRow, currentLine + nextLine);
            removeLine(cursorRow + 1);
        }

        ensureCursorVisible();
    }

    /**
     * Inserts a new line at the current cursor position.
     */
    public void insertNewLine() {
        if (!editable) {
            return;
        }

        if (hasSelection()) {
            deleteSelection();
        }

        String currentLine = getLine(cursorRow);
        String before = currentLine.substring(0, cursorCol);
        String after = currentLine.substring(cursorCol);

        setLine(cursorRow, before);
        insertLine(cursorRow + 1, after);
        cursorRow++;
        cursorCol = 0;

        ensureCursorVisible();
    }

    // Selection methods

    /**
     * Starts a selection at the current cursor position.
     */
    public void startSelection() {
        selectionStart = new Position(cursorCol, cursorRow);
        selectionEnd = null;
    }

    /**
     * Extends the selection to the current cursor position.
     */
    public void extendSelection() {
        if (selectionStart != null) {
            selectionEnd = new Position(cursorCol, cursorRow);
        }
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        selectionStart = null;
        selectionEnd = null;
    }

    /**
     * Checks if there is an active selection.
     *
     * @return true if there is a selection
     */
    public boolean hasSelection() {
        return selectionStart != null && selectionEnd != null && !selectionStart.equals(selectionEnd);
    }

    /**
     * Gets the selected text.
     *
     * @return the selected text, or empty string if no selection
     */
    public String getSelectedText() {
        if (!hasSelection()) {
            return "";
        }

        Position start = getSelectionStart();
        Position end = getSelectionEnd();

        if (start.y() == end.y()) {
            // Single line selection
            String line = getLine(start.y());
            return line.substring(start.x(), end.x());
        } else {
            // Multi-line selection
            StringBuilder sb = new StringBuilder();

            // First line
            String firstLine = getLine(start.y());
            sb.append(firstLine.substring(start.x()));

            // Middle lines
            for (int i = start.y() + 1; i < end.y(); i++) {
                sb.append("\n").append(getLine(i));
            }

            // Last line
            if (end.y() < lines.size()) {
                String lastLine = getLine(end.y());
                sb.append("\n").append(lastLine.substring(0, end.x()));
            }

            return sb.toString();
        }
    }

    /**
     * Deletes the selected text.
     */
    public void deleteSelection() {
        if (!hasSelection() || !editable) {
            return;
        }

        Position start = getSelectionStart();
        Position end = getSelectionEnd();

        if (start.y() == end.y()) {
            // Single line deletion
            String line = getLine(start.y());
            String newLine = line.substring(0, start.x()) + line.substring(end.x());
            setLine(start.y(), newLine);
        } else {
            // Multi-line deletion
            String firstLine = getLine(start.y());
            String lastLine = getLine(end.y());
            String newLine = firstLine.substring(0, start.x()) + lastLine.substring(end.x());

            // Remove lines in between
            for (int i = end.y(); i > start.y(); i--) {
                removeLine(i);
            }

            setLine(start.y(), newLine);
        }

        setCursorPosition(start.y(), start.x());
        clearSelection();
    }

    private Position getSelectionStart() {
        if (selectionStart == null || selectionEnd == null) {
            return selectionStart;
        }

        if (selectionStart.y() < selectionEnd.y()
                || (selectionStart.y() == selectionEnd.y() && selectionStart.x() < selectionEnd.x())) {
            return selectionStart;
        } else {
            return selectionEnd;
        }
    }

    private Position getSelectionEnd() {
        if (selectionStart == null || selectionEnd == null) {
            return selectionEnd;
        }

        if (selectionStart.y() > selectionEnd.y()
                || (selectionStart.y() == selectionEnd.y() && selectionStart.x() > selectionEnd.x())) {
            return selectionStart;
        } else {
            return selectionEnd;
        }
    }

    // Scrolling methods

    /**
     * Ensures the cursor is visible by adjusting scroll position if necessary.
     */
    private void ensureCursorVisible() {
        Size size = getSize();
        if (size == null) {
            return;
        }

        int viewHeight = size.h();
        int viewWidth = size.w();

        // Vertical scrolling
        if (cursorRow < scrollRow) {
            scrollRow = cursorRow;
        } else if (cursorRow >= scrollRow + viewHeight) {
            scrollRow = cursorRow - viewHeight + 1;
        }

        // Horizontal scrolling
        if (cursorCol < scrollCol) {
            scrollCol = cursorCol;
        } else if (cursorCol >= scrollCol + viewWidth) {
            scrollCol = cursorCol - viewWidth + 1;
        }
    }

    /**
     * Scrolls the view up by the specified number of lines.
     *
     * @param lines the number of lines to scroll up
     */
    public void scrollUp(int lines) {
        scrollRow = Math.max(0, scrollRow - lines);
    }

    /**
     * Scrolls the view down by the specified number of lines.
     *
     * @param lines the number of lines to scroll down
     */
    public void scrollDown(int lines) {
        int maxScroll =
                Math.max(0, this.lines.size() - (getSize() != null ? getSize().h() : 1));
        scrollRow = Math.min(maxScroll, scrollRow + lines);
    }

    /**
     * Scrolls the view left by the specified number of columns.
     *
     * @param cols the number of columns to scroll left
     */
    public void scrollLeft(int cols) {
        scrollCol = Math.max(0, scrollCol - cols);
    }

    /**
     * Scrolls the view right by the specified number of columns.
     *
     * @param cols the number of columns to scroll right
     */
    public void scrollRight(int cols) {
        scrollCol += cols;
    }

    // Property getters and setters

    /**
     * Gets whether the text area is editable.
     *
     * @return true if editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets whether the text area is editable.
     *
     * @param editable true to make editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Gets whether word wrap is enabled.
     *
     * @return true if word wrap is enabled
     */
    public boolean isWordWrap() {
        return wordWrap;
    }

    /**
     * Sets whether word wrap is enabled.
     *
     * @param wordWrap true to enable word wrap
     */
    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    /**
     * Gets the tab size.
     *
     * @return the tab size in characters
     */
    public int getTabSize() {
        return tabSize;
    }

    /**
     * Sets the tab size.
     *
     * @param tabSize the tab size in characters
     */
    public void setTabSize(int tabSize) {
        this.tabSize = Math.max(1, tabSize);
    }

    // Component implementation

    @Override
    public boolean handleKey(KeyEvent event) {
        if (event.getType() == KeyEvent.Type.Character && !event.hasModifier(KeyEvent.Modifier.Alt)) {
            if (event.hasModifier(KeyEvent.Modifier.Control)) {
                switch (event.getCharacter()) {
                    case 'a':
                        moveCursorToLineStart();
                        return true;
                    case 'e':
                        moveCursorToLineEnd();
                        return true;
                }
            } else if (editable) {
                if (hasSelection()) {
                    deleteSelection();
                }
                insertChar(event.getCharacter());
                return true;
            }
        } else if (event.getType() == KeyEvent.Type.Arrow) {
            switch (event.getArrow()) {
                case Up:
                    moveCursorUp();
                    return true;
                case Down:
                    moveCursorDown();
                    return true;
                case Left:
                    moveCursorLeft();
                    return true;
                case Right:
                    moveCursorRight();
                    return true;
            }
        } else if (event.getType() == KeyEvent.Type.Special) {
            switch (event.getSpecial()) {
                case Enter:
                    if (editable) {
                        insertNewLine();
                        return true;
                    }
                    break;
                case Backspace:
                    if (editable) {
                        if (hasSelection()) {
                            deleteSelection();
                        } else {
                            deleteCharBefore();
                        }
                        return true;
                    }
                    break;
                case Delete:
                    if (editable) {
                        if (hasSelection()) {
                            deleteSelection();
                        } else {
                            deleteCharAfter();
                        }
                        return true;
                    }
                    break;
                case Home:
                    moveCursorToLineStart();
                    return true;
                case End:
                    moveCursorToLineEnd();
                    return true;
                case PageUp:
                    scrollUp(getSize() != null ? getSize().h() : 10);
                    return true;
                case PageDown:
                    scrollDown(getSize() != null ? getSize().h() : 10);
                    return true;
            }
        }
        return false;
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

        // Clear the area
        screen.fill(pos.x(), pos.y(), width, height, normalStyle);

        // Draw text lines
        for (int row = 0; row < height && (scrollRow + row) < lines.size(); row++) {
            String line = getLine(scrollRow + row);
            if (line.length() > scrollCol) {
                String visiblePart = line.substring(scrollCol);
                if (visiblePart.length() > width) {
                    visiblePart = visiblePart.substring(0, width);
                }

                // Expand tabs
                visiblePart = expandTabs(visiblePart);

                // Apply selection highlighting
                AttributedStringBuilder asb = new AttributedStringBuilder();
                for (int col = 0; col < visiblePart.length(); col++) {
                    int actualRow = scrollRow + row;
                    int actualCol = scrollCol + col;

                    AttributedStyle style = normalStyle;
                    if (isPositionSelected(actualRow, actualCol)) {
                        style = selectionStyle;
                    }

                    asb.style(style);
                    asb.append(visiblePart.charAt(col));
                }

                screen.text(pos.x(), pos.y() + row, asb.toAttributedString());
            }
        }

        // Draw cursor if focused and within visible area
        if (isFocused()
                && cursorRow >= scrollRow
                && cursorRow < scrollRow + height
                && cursorCol >= scrollCol
                && cursorCol < scrollCol + width) {

            int screenRow = cursorRow - scrollRow;
            int screenCol = cursorCol - scrollCol;

            // Get character at cursor position or use space
            String line = getLine(cursorRow);
            char cursorChar = (cursorCol < line.length()) ? line.charAt(cursorCol) : ' ';

            // Draw cursor
            AttributedString cursorStr = new AttributedString(String.valueOf(cursorChar), cursorStyle);
            screen.text(pos.x() + screenCol, pos.y() + screenRow, cursorStr);
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        // Calculate preferred size based on content
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, expandTabs(line).length());
        }

        return new Size(Math.max(20, Math.min(80, maxWidth)), Math.max(3, Math.min(25, lines.size())));
    }

    /**
     * Expands tabs in a string to spaces.
     *
     * @param text the text to expand
     * @return the text with tabs expanded to spaces
     */
    private String expandTabs(String text) {
        if (!text.contains("\t")) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\t') {
                int spaces = tabSize - (sb.length() % tabSize);
                for (int j = 0; j < spaces; j++) {
                    sb.append(' ');
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Checks if a position is within the current selection.
     *
     * @param row the row position
     * @param col the column position
     * @return true if the position is selected
     */
    private boolean isPositionSelected(int row, int col) {
        if (!hasSelection()) {
            return false;
        }

        Position start = getSelectionStart();
        Position end = getSelectionEnd();

        if (row < start.y() || row > end.y()) {
            return false;
        }

        if (row == start.y() && row == end.y()) {
            return col >= start.x() && col < end.x();
        } else if (row == start.y()) {
            return col >= start.x();
        } else if (row == end.y()) {
            return col < end.x();
        } else {
            return true; // Middle rows are fully selected
        }
    }
}
