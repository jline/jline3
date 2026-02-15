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
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A label component for displaying static text.
 *
 * <p>Label provides a simple text display component with support for:
 * <ul>
 * <li>Single and multi-line text display</li>
 * <li>Text alignment (left, center, right)</li>
 * <li>Text wrapping</li>
 * <li>Custom styling</li>
 * </ul>
 * </p>
 */
public class Label extends AbstractComponent {

    /**
     * Text alignment options.
     */
    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    private String text;
    private Alignment alignment = Alignment.LEFT;
    private boolean wordWrap = false;
    private AttributedStyle style = AttributedStyle.DEFAULT;

    public Label() {
        this("");
    }

    public Label(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Gets the label text.
     *
     * @return the label text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the label text.
     *
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Gets the text alignment.
     *
     * @return the text alignment
     */
    public Alignment getAlignment() {
        return alignment;
    }

    /**
     * Sets the text alignment.
     *
     * @param alignment the alignment to set
     */
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment != null ? alignment : Alignment.LEFT;
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
     * Gets the text style.
     *
     * @return the text style
     */
    public AttributedStyle getStyle() {
        return style;
    }

    /**
     * Sets the text style.
     *
     * @param style the style to set
     */
    public void setStyle(AttributedStyle style) {
        this.style = style != null ? style : AttributedStyle.DEFAULT;
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        if (size == null || text.isEmpty()) {
            return;
        }

        Position pos = getScreenPosition();
        if (pos == null) {
            return;
        }

        AttributedStyle drawStyle = resolveStyle(".label.normal", style);

        int width = size.w();
        int height = size.h();

        // Fill the entire label area with the background style
        screen.fill(pos.x(), pos.y(), width, height, drawStyle);

        String[] lines = getDisplayLines(width);

        for (int i = 0; i < Math.min(lines.length, height); i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }

            int x = calculateXPosition(line, width);
            // Clip line to component width
            int maxLen = width - x;
            if (line.length() > maxLen) {
                line = line.substring(0, Math.max(0, maxLen));
            }
            AttributedString attributedLine = new AttributedString(line, drawStyle);
            screen.text(pos.x() + x, pos.y() + i, attributedLine);
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        if (text.isEmpty()) {
            return new Size(1, 1);
        }

        String[] lines = text.split("\n", -1);
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, line.length());
        }

        return new Size(maxWidth, lines.length);
    }

    /**
     * Gets the lines to display, handling word wrap if enabled.
     *
     * @param width the available width
     * @return the lines to display
     */
    private String[] getDisplayLines(int width) {
        if (!wordWrap || width <= 0) {
            return text.split("\n", -1);
        }

        String[] originalLines = text.split("\n", -1);
        java.util.List<String> wrappedLines = new java.util.ArrayList<>();

        for (String line : originalLines) {
            if (line.length() <= width) {
                wrappedLines.add(line);
            } else {
                // Simple word wrapping
                int start = 0;
                while (start < line.length()) {
                    int end = Math.min(start + width, line.length());

                    // Try to break at word boundary
                    if (end < line.length()) {
                        int lastSpace = line.lastIndexOf(' ', end);
                        if (lastSpace > start) {
                            end = lastSpace;
                        }
                    }

                    wrappedLines.add(line.substring(start, end));
                    start = end;
                    if (start < line.length() && line.charAt(start) == ' ') {
                        start++; // Skip the space
                    }
                }
            }
        }

        return wrappedLines.toArray(new String[0]);
    }

    /**
     * Calculates the X position for a line based on alignment.
     *
     * @param line the line text
     * @param width the available width
     * @return the X position
     */
    private int calculateXPosition(String line, int width) {
        switch (alignment) {
            case CENTER:
                return Math.max(0, (width - line.length()) / 2);
            case RIGHT:
                return Math.max(0, width - line.length());
            case LEFT:
            default:
                return 0;
        }
    }
}
