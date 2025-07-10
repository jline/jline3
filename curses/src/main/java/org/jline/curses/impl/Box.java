/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.Collection;
import java.util.Collections;

import org.jline.curses.*;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class Box extends AbstractComponent implements Container {

    private final String title;
    private final Curses.Border border;
    private final Component component;
    private final String shortcutKey;

    public Box(String title, Curses.Border border, Component component) {
        this(title, border, component, null);
    }

    public Box(String title, Curses.Border border, Component component, String shortcutKey) {
        this.title = title;
        this.border = border;
        this.component = component;
        this.shortcutKey = shortcutKey;
        // Set parent after all fields are initialized to avoid this-escape warning
        initializeComponent();
    }

    private void initializeComponent() {
        ((AbstractComponent) component).setParent(this);
    }

    @Override
    public BoxRenderer getRenderer() {
        return (BoxRenderer) super.getRenderer();
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer((BoxRenderer) renderer);
    }

    public String getTitle() {
        return title;
    }

    public Curses.Border getBorder() {
        return border;
    }

    public Component getComponent() {
        return component;
    }

    @Override
    public String getShortcutKey() {
        return shortcutKey;
    }

    @Override
    public void focus() {
        // Delegate focus to the inner component
        if (component != null) {
            component.focus();
        } else {
            // Fallback to default behavior if no inner component
            super.focus();
        }
    }

    @Override
    public void setSize(Size size) {
        super.setSize(size);
        component.setPosition(getRenderer().getComponentOffset());
        component.setSize(getRenderer().getComponentSize(size));
    }

    @Override
    public void setPosition(Position position) {
        super.setPosition(position);
        // Update the contained component's position when the box position changes
        if (component != null && getSize() != null) {
            component.setPosition(getRenderer().getComponentOffset());
        }
    }

    @Override
    public Collection<Component> getComponents() {
        return Collections.singleton(component);
    }

    @Override
    protected void doDraw(Screen screen) {
        Position pos = getScreenPosition();
        Size size = getSize();
        if (pos == null || size == null) {
            return;
        }

        if (!getBehaviors().contains(Behavior.NoDecoration)) {
            // Draw border with focus indication
            boolean focused = component != null && component.isFocused();
            String borderStyleName = focused ? ".box.border.focused" : ".box.border";
            AttributedStyle borderStyle = getTheme().getStyle(borderStyleName);
            drawBorder(screen, pos, size, border, borderStyle);

            // Draw title if present
            if (title != null && !title.isEmpty()) {
                AttributedString titleString = createTitleWithShortcut(focused);
                screen.text(pos.x() + 2, pos.y(), titleString);
            }
        }

        // Draw the contained component
        if (component != null) {
            component.draw(screen);
        }
    }

    private void drawBorder(Screen screen, Position pos, Size size, Curses.Border border, AttributedStyle style) {
        int x = pos.x();
        int y = pos.y();
        int w = size.w();
        int h = size.h();

        if (w < 2 || h < 2) {
            return;
        }

        // Choose border characters based on border type
        String topLeft, topRight, bottomLeft, bottomRight, horizontal, vertical;
        switch (border) {
            case Double:
                topLeft = "╔";
                topRight = "╗";
                bottomLeft = "╚";
                bottomRight = "╝";
                horizontal = "═";
                vertical = "║";
                break;
            case SingleBevel:
                topLeft = "┌";
                topRight = "┐";
                bottomLeft = "└";
                bottomRight = "┘";
                horizontal = "─";
                vertical = "│";
                break;
            case DoubleBevel:
                topLeft = "╔";
                topRight = "╗";
                bottomLeft = "╚";
                bottomRight = "╝";
                horizontal = "═";
                vertical = "║";
                break;
            default: // Single
                topLeft = "┌";
                topRight = "┐";
                bottomLeft = "└";
                bottomRight = "┘";
                horizontal = "─";
                vertical = "│";
                break;
        }

        // Draw corners
        screen.text(x, y, new AttributedString(topLeft, style));
        screen.text(x + w - 1, y, new AttributedString(topRight, style));
        screen.text(x, y + h - 1, new AttributedString(bottomLeft, style));
        screen.text(x + w - 1, y + h - 1, new AttributedString(bottomRight, style));

        // Draw horizontal lines
        for (int i = 1; i < w - 1; i++) {
            screen.text(x + i, y, new AttributedString(horizontal, style));
            screen.text(x + i, y + h - 1, new AttributedString(horizontal, style));
        }

        // Draw vertical lines
        for (int i = 1; i < h - 1; i++) {
            screen.text(x, y + i, new AttributedString(vertical, style));
            screen.text(x + w - 1, y + i, new AttributedString(vertical, style));
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        Size sz = getComponent().getPreferredSize();
        if (getBehaviors().contains(Behavior.NoDecoration)) {
            return sz;
        } else {
            return new Size(sz.w() + 2, sz.h() + 2);
        }
    }

    @Override
    protected BoxRenderer getDefaultRenderer() {
        return new BoxRenderer() {
            @Override
            public void draw(Screen screen, Component box) {
                ((Box) box).doDraw(screen);
            }

            @Override
            public Size getPreferredSize(Component box) {
                return ((Box) box).doGetPreferredSize();
            }

            @Override
            public Position getComponentOffset() {
                return new Position(1, 1);
            }

            @Override
            public Size getComponentSize(Size box) {
                return new Size(Math.max(0, box.w() - 2), Math.max(0, box.h() - 2));
            }
        };
    }

    private AttributedString createTitleWithShortcut() {
        return createTitleWithShortcut(false);
    }

    private AttributedString createTitleWithShortcut(boolean focused) {
        String titleStyleName = focused ? ".box.title.focused" : ".box.title";
        AttributedStyle titleStyle = getTheme().getStyle(titleStyleName);
        AttributedStyle keyStyle = getTheme().getStyle(".box.key");

        if (shortcutKey == null) {
            return new AttributedString(title, titleStyle);
        }

        // Find the shortcut key in the title (case insensitive)
        int keyIndex = title.toLowerCase().indexOf(shortcutKey.toLowerCase());
        if (keyIndex < 0) {
            return new AttributedString(title, titleStyle);
        }

        // Build the title with highlighted shortcut key
        AttributedStringBuilder sb = new AttributedStringBuilder();

        // Text before the key
        if (keyIndex > 0) {
            sb.style(titleStyle);
            sb.append(title, 0, keyIndex);
        }

        // The shortcut key (highlighted)
        sb.style(keyStyle);
        sb.append(title, keyIndex, keyIndex + shortcutKey.length());

        // Text after the key
        if (keyIndex + shortcutKey.length() < title.length()) {
            sb.style(titleStyle);
            sb.append(title, keyIndex + shortcutKey.length(), title.length());
        }

        return sb.toAttributedString();
    }

    public interface BoxRenderer extends Renderer {
        Position getComponentOffset();

        Size getComponentSize(Size box);
    }
}
