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
import org.jline.terminal.KeyEvent;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A clickable button component.
 *
 * <p>Button provides a clickable UI element with support for:
 * <ul>
 * <li>Text labels</li>
 * <li>Click event handling</li>
 * <li>Visual states (normal, focused, pressed)</li>
 * <li>Keyboard activation (Enter/Space)</li>
 * </ul>
 * </p>
 */
public class Button extends AbstractComponent {

    private String text;
    private boolean pressed = false;
    private List<Runnable> clickListeners = new ArrayList<>();

    // Styling
    private AttributedStyle normalStyle = AttributedStyle.DEFAULT;
    private AttributedStyle focusedStyle = AttributedStyle.DEFAULT.inverse();
    private AttributedStyle pressedStyle =
            AttributedStyle.DEFAULT.background(AttributedStyle.BRIGHT).foreground(AttributedStyle.BLACK);

    public Button() {
        this("Button");
    }

    public Button(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Gets the button text.
     *
     * @return the button text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the button text.
     *
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Adds a click listener to the button.
     *
     * @param listener the listener to add
     */
    public void addClickListener(Runnable listener) {
        if (listener != null) {
            clickListeners.add(listener);
        }
    }

    /**
     * Removes a click listener from the button.
     *
     * @param listener the listener to remove
     */
    public void removeClickListener(Runnable listener) {
        clickListeners.remove(listener);
    }

    /**
     * Simulates a button click, firing all click listeners.
     */
    public void click() {
        for (Runnable listener : clickListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // Log error but continue with other listeners
                System.err.println("Error in button click listener: " + e.getMessage());
            }
        }
    }

    /**
     * Sets the pressed state of the button.
     *
     * @param pressed true if the button should appear pressed
     */
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    /**
     * Gets whether the button is currently pressed.
     *
     * @return true if the button is pressed
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Gets the normal style for the button.
     *
     * @return the normal style
     */
    public AttributedStyle getNormalStyle() {
        return normalStyle;
    }

    /**
     * Sets the normal style for the button.
     *
     * @param normalStyle the style to set
     */
    public void setNormalStyle(AttributedStyle normalStyle) {
        this.normalStyle = normalStyle != null ? normalStyle : AttributedStyle.DEFAULT;
    }

    /**
     * Gets the focused style for the button.
     *
     * @return the focused style
     */
    public AttributedStyle getFocusedStyle() {
        return focusedStyle;
    }

    /**
     * Sets the focused style for the button.
     *
     * @param focusedStyle the style to set
     */
    public void setFocusedStyle(AttributedStyle focusedStyle) {
        this.focusedStyle = focusedStyle != null ? focusedStyle : AttributedStyle.DEFAULT.inverse();
    }

    /**
     * Gets the pressed style for the button.
     *
     * @return the pressed style
     */
    public AttributedStyle getPressedStyle() {
        return pressedStyle;
    }

    /**
     * Sets the pressed style for the button.
     *
     * @param pressedStyle the style to set
     */
    public void setPressedStyle(AttributedStyle pressedStyle) {
        this.pressedStyle =
                pressedStyle != null ? pressedStyle : AttributedStyle.DEFAULT.background(AttributedStyle.BRIGHT);
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        if (event.getType() == KeyEvent.Type.Special) {
            if (event.getSpecial() == KeyEvent.Special.Enter) {
                click();
                return true;
            }
        } else if (event.getType() == KeyEvent.Type.Character) {
            if (event.getCharacter() == ' ') {
                click();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        Position pos = getScreenPosition();
        if (size == null || pos == null) {
            return;
        }

        int ox = pos.x();
        int oy = pos.y();
        int width = size.w();
        int height = size.h();

        // Determine the style to use
        AttributedStyle style;
        if (pressed) {
            style = pressedStyle;
        } else if (isFocused()) {
            style = focusedStyle;
        } else {
            style = normalStyle;
        }

        // Fill the button area with the background style
        screen.fill(ox, oy, width, height, style);

        // Draw the button text centered
        if (!text.isEmpty() && width > 2 && height > 0) {
            int textWidth = text.length();
            int textX = Math.max(0, (width - textWidth) / 2);
            int textY = Math.max(0, (height - 1) / 2);

            // Ensure text fits within button bounds
            String displayText = text;
            if (textWidth > width - 2) {
                displayText = text.substring(0, Math.max(0, width - 5)) + "...";
                textX = 1;
            }

            AttributedString buttonText = new AttributedString(displayText, style);
            screen.text(ox + textX, oy + textY, buttonText);
        }

        // Draw button border (simple box)
        if (width > 1 && height > 1) {
            drawBorder(screen, pos, style);
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        // Calculate preferred size based on text
        int textWidth = text.length();
        int preferredWidth = Math.max(textWidth + 4, 8); // Text + padding, minimum 8
        int preferredHeight = 3; // Standard button height

        return new Size(preferredWidth, preferredHeight);
    }

    /**
     * Draws a simple border around the button.
     *
     * @param screen the screen to draw on
     * @param style the style to use for the border
     */
    private void drawBorder(Screen screen, Position pos, AttributedStyle style) {
        Size size = getSize();
        if (size == null) {
            return;
        }

        int ox = pos.x();
        int oy = pos.y();
        int width = size.w();
        int height = size.h();

        // Draw corners and edges
        String topLeft = "\u250c";
        String topRight = "\u2510";
        String bottomLeft = "\u2514";
        String bottomRight = "\u2518";
        String horizontal = "\u2500";
        String vertical = "\u2502";

        // Top border
        screen.text(ox, oy, new AttributedString(topLeft, style));
        for (int x = 1; x < width - 1; x++) {
            screen.text(ox + x, oy, new AttributedString(horizontal, style));
        }
        screen.text(ox + width - 1, oy, new AttributedString(topRight, style));

        // Side borders
        for (int y = 1; y < height - 1; y++) {
            screen.text(ox, oy + y, new AttributedString(vertical, style));
            screen.text(ox + width - 1, oy + y, new AttributedString(vertical, style));
        }

        // Bottom border
        if (height > 1) {
            screen.text(ox, oy + height - 1, new AttributedString(bottomLeft, style));
            for (int x = 1; x < width - 1; x++) {
                screen.text(ox + x, oy + height - 1, new AttributedString(horizontal, style));
            }
            screen.text(ox + width - 1, oy + height - 1, new AttributedString(bottomRight, style));
        }
    }
}
