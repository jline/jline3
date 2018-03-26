/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses.impl;

import org.jline.curses.*;
import org.jline.terminal.MouseEvent;

import java.util.EnumSet;

public abstract class AbstractComponent implements Component {

    private Size size;
    private Size preferredSize;
    private Position position;
    private boolean enabled;
    private boolean focused;
    private Container parent;
    private Renderer renderer;
    private Theme theme;
    private EnumSet<Behavior> behaviors = EnumSet.noneOf(Behavior.class);

    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getScreenPosition() {
        Position p = parent != null ? parent.getScreenPosition() : new Position(0, 0);
        return new Position(position.x() + p.x(), position.y() + p.y());
    }

    @Override
    public boolean isIn(int x, int y) {
        Position p = getScreenPosition();
        Size s = getSize();
        return p.x() <= x && x <= p.x() + s.w()
                && p.y() <= y && y <= p.y() + s.h();
    }

    public Size getSize() {
        return size;
    }

    @Override
    public void setSize(Size size) {
        this.size = size;
    }

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

    @SuppressWarnings("unchecked")
    @Override
    public void draw(Screen screen) {
        getRenderer().draw(screen, this);
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
        this.focused = focused;
        if (focused) {
            this.onFocus();
        } else {
            this.onUnfocus();
        }
    }

    public void onFocus() {
    }

    public void onUnfocus() {
    }

    @SuppressWarnings("unchecked")
    protected Size computePreferredSize() {
        return getRenderer().getPreferredSize(this);
    }

    @SuppressWarnings("unchecked")
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
                AbstractComponent.class.cast(component).doDraw(screen);
            }

            @Override
            public Size getPreferredSize(Component component) {
                return AbstractComponent.class.cast(component).doGetPreferredSize();
            }
        };
    }

    protected abstract void doDraw(Screen screen);

    protected abstract Size doGetPreferredSize();

    @Override
    public void handleMouse(MouseEvent event) {
    }

    @Override
    public void handleInput(String input) {
    }
}
