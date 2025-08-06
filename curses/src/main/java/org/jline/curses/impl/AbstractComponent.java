/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.EnumSet;

import org.jline.curses.*;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.MouseEvent;

public abstract class AbstractComponent implements Component {

    private Size size;
    private Size preferredSize;
    private Position position;
    private boolean enabled;
    private boolean focused;
    private boolean invalid = true; // Start as invalid to ensure initial draw
    private Container parent;
    private Renderer renderer;
    private Theme theme;
    private EnumSet<Behavior> behaviors = EnumSet.noneOf(Behavior.class);

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public Position getScreenPosition() {
        Position p = parent != null ? parent.getScreenPosition() : new Position(0, 0);
        return new Position(position.x() + p.x(), position.y() + p.y());
    }

    @Override
    public boolean isIn(int x, int y) {
        Position p = getScreenPosition();
        Size s = getSize();
        return p.x() <= x && x <= p.x() + s.w() && p.y() <= y && y <= p.y() + s.h();
    }

    @Override
    public Size getSize() {
        return size;
    }

    @Override
    public void setSize(Size size) {
        this.size = size;
    }

    @Override
    public Size getPreferredSize() {
        if (preferredSize == null) {
            return computePreferredSize();
        }
        return preferredSize;
    }

    public void setPreferredSize(Size preferredSize) {
        this.preferredSize = preferredSize;
    }

    @Override
    public EnumSet<Behavior> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(EnumSet<Behavior> behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public void draw(Screen screen) {
        getRenderer().draw(screen, this);
        // Mark as valid after drawing
        invalid = false;
    }

    @Override
    public void invalidate() {
        invalid = true;
        // Propagate invalidation to parent if needed
        if (parent != null && parent instanceof AbstractComponent) {
            ((AbstractComponent) parent).invalidate();
        }
    }

    @Override
    public boolean isInvalid() {
        return invalid;
    }

    public Renderer getRenderer() {
        if (renderer == null) {
            return computeRenderer();
        }
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public Theme getTheme() {
        if (theme == null) {
            return getWindow().getGUI().getTheme();
        }
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Container getParent() {
        return parent;
    }

    public void setParent(Container parent) {
        this.parent = parent;
    }

    public Window getWindow() {
        Container parent = this instanceof Container ? (Container) this : getParent();
        while (parent != null) {
            if (parent instanceof Window) {
                return (Window) parent;
            } else {
                parent = parent.getParent();
            }
        }
        return null;
    }

    @Override
    public void enable(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void focus() {
        if (getWindow() != null) {
            getWindow().focus(this);
        }
    }

    void focused(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
            // Invalidate when focus changes to trigger visual update
            invalidate();
            if (focused) {
                this.onFocus();
            } else {
                this.onUnfocus();
            }
        }
    }

    public void onFocus() {}

    public void onUnfocus() {}

    protected Size computePreferredSize() {
        return getRenderer().getPreferredSize(this);
    }

    protected Renderer computeRenderer() {
        Window window = getWindow();
        GUI gui = window != null ? window.getGUI() : null;
        Renderer renderer = gui != null ? gui.getRenderer(getClass()) : null;
        return renderer != null ? renderer : getDefaultRenderer();
    }

    protected Renderer getDefaultRenderer() {
        return new Renderer() {
            @Override
            public void draw(Screen screen, Component component) {
                ((AbstractComponent) component).doDraw(screen);
            }

            @Override
            public Size getPreferredSize(Component component) {
                return ((AbstractComponent) component).doGetPreferredSize();
            }
        };
    }

    protected abstract void doDraw(Screen screen);

    protected abstract Size doGetPreferredSize();

    @Override
    public boolean handleMouse(MouseEvent event) {
        return false; // Default: not handled
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        return false; // Default: not handled
    }
}
