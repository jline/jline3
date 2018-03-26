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

import java.util.Collection;
import java.util.Collections;

public class Box extends AbstractComponent implements Container {

    private String title;
    private Curses.Border border;
    private Component component;

    public Box(String title, Curses.Border border, Component component) {
        this.title = title;
        this.border = border;
        this.component = component;
        AbstractComponent.class.cast(component).setParent(this);
    }

    @Override
    public BoxRenderer getRenderer() {
        return BoxRenderer.class.cast(super.getRenderer());
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(BoxRenderer.class.cast(renderer));
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
    protected void doDraw(Screen screen) {

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
                Box.class.cast(box).doDraw(screen);
            }

            @Override
            public Size getPreferredSize(Component box) {
                return Box.class.cast(box).doGetPreferredSize();
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

    interface BoxRenderer extends Renderer {
        Position getComponentOffset();
        Size getComponentSize(Size box);
    }
}
