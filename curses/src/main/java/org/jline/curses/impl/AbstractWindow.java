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
import org.jline.terminal.MouseEvent;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

public abstract class AbstractWindow extends AbstractComponent implements Window {

    private String title;
    private Component component;
    private GUI gui;
    private AbstractComponent focused;

    public AbstractWindow() {
        this(null, null);
    }

    public AbstractWindow(String title) {
        this(title, null);
    }

    public AbstractWindow(String title, Component component) {
        this.title = title;
        this.component = component;
        this.setBehaviors(EnumSet.of(Behavior.CloseButton));
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public void setComponent(Component component) {
        ((AbstractComponent) component).setParent(this);
        this.component = component;
    }

    @Override
    public GUI getGUI() {
        return gui;
    }

    public void setGUI(GUI gui) {
        this.gui = gui;
    }

    @Override
    public void setSize(Size size) {
        super.setSize(size);
        if (component != null) {
            component.setPosition(getRenderer().getComponentOffset());
            component.setSize(getRenderer().getComponentSize(size));
        }
    }

    @Override
    public Size getPreferredSize() {
        return component != null ? component.getPreferredSize() : new Size(0, 0);
    }

    @Override
    public void focus(Component component) {
        AbstractComponent c = (AbstractComponent) component;
        if (c != null && c.getWindow() != this) {
            throw new IllegalStateException();
        }
        if (focused != c) {
            if (focused != null) {
                focused.focused(false);
            }
            focused = c;
            if (focused != null) {
                focused.focused(true);
            }
        }
    }

    @Override
    public WindowRenderer getRenderer() {
        return (WindowRenderer) super.getRenderer();
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer((WindowRenderer) renderer);
    }

    @Override
    protected WindowRenderer getDefaultRenderer() {
        return new WindowRenderer() {
            @Override
            public void draw(Screen screen, Component window) {
                ((AbstractWindow) window).doDraw(screen);
            }

            @Override
            public Size getPreferredSize(Component window) {
                return ((AbstractWindow) window).doGetPreferredSize();
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

    @Override
    public void handleInput(String input) {
        if (input.contains("q")) {
            close();
        }
    }

    @Override
    public void handleMouse(MouseEvent event) {
        if (component != null && component.isIn(event.getX(), event.getY())) {
            component.handleMouse(event);
            return;
        }
        if (getBehaviors().contains(Behavior.CloseButton) && !getBehaviors().contains(Behavior.NoDecoration)) {
            Position pos = getScreenPosition();
            if (event.getX() == pos.x() + getSize().w() - 2 && event.getY() == pos.y()) {
                close();
            }
        }
    }

    @Override
    public void close() {
        GUI gui = getGUI();
        if (gui != null) {
            gui.removeWindow(this);
        }
    }

    @Override
    protected void doDraw(Screen screen) {
        Position pos = getScreenPosition();
        if (getBehaviors().contains(Behavior.NoDecoration)) {
            AttributedStyle st = getTheme().getStyle(".window.border");
            screen.fill(pos.x(), pos.y(), getSize().w(), getSize().h(), st);
        } else {
            screen.fill(
                    pos.x() + 2,
                    pos.y() + 1,
                    getSize().w(),
                    getSize().h(),
                    getTheme().getStyle(".window.shadow"));
            getTheme()
                    .box(
                            screen,
                            pos.x(),
                            pos.y(),
                            getSize().w(),
                            getSize().h(),
                            Curses.Border.Double,
                            ".window.border");
            if (getBehaviors().contains(Behavior.CloseButton)) {
                screen.text(
                        pos.x() + getSize().w() - 2,
                        pos.y(),
                        new AttributedString("x", getTheme().getStyle(".window.close")));
            }
            if (title != null) {
                screen.text(
                        pos.x() + 3,
                        pos.y(),
                        new AttributedString(title, getTheme().getStyle(".window.title")));
            }
            if (component != null) {
                component.draw(screen);
            }
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

    public interface WindowRenderer extends Renderer {
        Position getComponentOffset();

        Size getComponentSize(Size box);
    }
}
