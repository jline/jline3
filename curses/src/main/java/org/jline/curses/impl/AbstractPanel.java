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
import org.jline.keymap.KeyMap;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.MouseEvent;

public abstract class AbstractPanel extends AbstractComponent implements Container {

    protected final Map<Component, Constraint> components = new LinkedHashMap<>();
    private KeyMap<Component> shortcutKeyMap;

    public void addComponent(Component component, Constraint constraint) {
        if (!(component instanceof AbstractComponent)) {
            throw new IllegalArgumentException("Components should extend AbstractComponent");
        }
        components.put(component, constraint);
        ((AbstractComponent) component).setParent(this);

        // If this component has a shortcut key, register it
        String shortcutKey = component.getShortcutKey();
        if (shortcutKey != null) {
            registerShortcut(component, shortcutKey);
        }
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
        getComponents().forEach(c -> {
            Size s = c.getSize();
            if (s != null && s.w() > 0 && s.h() > 0) {
                c.draw(screen);
            }
        });
    }

    @Override
    public boolean handleMouse(MouseEvent event) {
        for (Component component : components.keySet()) {
            if (component.isIn(event.getX(), event.getY())) {
                // If it's a click event and the component can be focused, focus it
                if (event.getType() == MouseEvent.Type.Pressed
                        && !component.getBehaviors().contains(Behavior.NoFocus)) {
                    component.focus();
                }

                // Let the component handle the mouse event
                if (component.handleMouse(event)) {
                    return true;
                }
            }
        }
        return super.handleMouse(event);
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        // First check for shortcut keys
        if (shortcutKeyMap != null) {
            // Check if this is an Alt+key combination for shortcuts
            if (event.getType() == KeyEvent.Type.Character && event.hasModifier(KeyEvent.Modifier.Alt)) {
                char shortcutChar = Character.toLowerCase(event.getCharacter());
                String altKey = "\u001b" + shortcutChar;
                Component target = shortcutKeyMap.getBound(altKey);
                if (target != null) {
                    // Focus the target component (it will delegate appropriately)
                    target.focus();
                    return true;
                }
            }
        }

        // If no shortcut matched, delegate to superclass
        return super.handleKey(event);
    }

    private void registerShortcut(Component component, String shortcutKey) {
        if (shortcutKeyMap == null) {
            shortcutKeyMap = new KeyMap<>();
        }

        // Bind Alt+key for the shortcut
        String altKey = "\u001b" + shortcutKey.toLowerCase();
        shortcutKeyMap.bind(component, altKey);
    }
}
