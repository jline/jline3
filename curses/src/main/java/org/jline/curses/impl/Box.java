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

public class Box extends AbstractComponent implements Container {

    private final String title;
    private final Curses.Border border;
    private final Component component;

    public Box(String title, Curses.Border border, Component component) {
        this.title = title;
        this.border = border;
        this.component = component;
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
    public void setSize(Size size) {
        super.setSize(size);
        component.setPosition(getRenderer().getComponentOffset());
        component.setSize(getRenderer().getComponentSize(size));
    }

    @Override
    public Collection<Component> getComponents() {
        return Collections.singleton(component);
    }

    @Override
    protected void doDraw(Screen screen) {}

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

    public interface BoxRenderer extends Renderer {
        Position getComponentOffset();

        Size getComponentSize(Size box);
    }
}
