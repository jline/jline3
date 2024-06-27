/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.*;

import org.jline.curses.*;
import org.jline.terminal.MouseEvent;

public abstract class AbstractPanel extends AbstractComponent implements Container {

    protected final Map<Component, Constraint> components = new LinkedHashMap<>();

    public void addComponent(Component component, Constraint constraint) {
        if (!(component instanceof AbstractComponent)) {
            throw new IllegalArgumentException("Components should extend AbstractComponent");
        }
        components.put(component, constraint);
        ((AbstractComponent) component).setParent(this);
    }

    @Override
    public Collection<Component> getComponents() {
        return Collections.unmodifiableSet(components.keySet());
    }

    @Override
    public void setSize(Size size) {
        super.setSize(size);
        layout();
    }

    protected abstract void layout();

    @Override
    protected void doDraw(Screen screen) {
        getComponents().forEach(c -> c.draw(screen));
    }

    @Override
    public void handleMouse(MouseEvent event) {
        for (Component component : components.keySet()) {
            if (component.isIn(event.getX(), event.getY())) {
                component.handleMouse(event);
                return;
            }
        }
        super.handleMouse(event);
    }

    @Override
    public void handleInput(String input) {
        super.handleInput(input);
    }
}
