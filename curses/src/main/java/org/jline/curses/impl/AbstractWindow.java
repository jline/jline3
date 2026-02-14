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
import java.util.EnumSet;
import java.util.List;

import org.jline.curses.*;
import org.jline.terminal.KeyEvent;
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
        // Initialize behaviors after all fields are set to avoid this-escape warning
        initializeBehaviors();
    }

    private void initializeBehaviors() {
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
    public boolean handleKey(KeyEvent event) {
        // Handle 'q' key to close window (only when no component is focused or focused didn't handle it)
        if (event.getType() == KeyEvent.Type.Character
                && !event.hasModifier(KeyEvent.Modifier.Alt)
                && !event.hasModifier(KeyEvent.Modifier.Control)
                && event.getCharacter() == 'q'
                && focused == null) {
            close();
            return true;
        }

        // Handle Tab / Shift+Tab for focus cycling
        if (event.getType() == KeyEvent.Type.Special && event.getSpecial() == KeyEvent.Special.Tab) {
            if (event.hasModifier(KeyEvent.Modifier.Shift)) {
                focusPrevious();
            } else {
                focusNext();
            }
            return true;
        }

        // Check for Alt+letter shortcuts (search all nested containers)
        if (component instanceof Container) {
            if (tryFocusShortcut((Container) component, event)) {
                return true;
            }
        }

        // Handle 'q' to close when focused component doesn't handle it
        if (event.getType() == KeyEvent.Type.Character
                && !event.hasModifier(KeyEvent.Modifier.Alt)
                && !event.hasModifier(KeyEvent.Modifier.Control)
                && event.getCharacter() == 'q') {
            // Let focused component try first
            if (focused != null && focused.handleKey(event)) {
                return true;
            }
            close();
            return true;
        }

        // Try the focused component
        if (focused != null && focused.handleKey(event)) {
            return true;
        }

        // If not handled by focused component, try the main component
        if (component != null && component.handleKey(event)) {
            return true;
        }

        return false;
    }

    private void focusNext() {
        List<Component> focusable = new ArrayList<>();
        collectFocusable(component, focusable);
        if (focusable.isEmpty()) {
            return;
        }
        int idx = focused != null ? focusable.indexOf(focused) : -1;
        int next = (idx + 1) % focusable.size();
        focusable.get(next).focus();
    }

    private void focusPrevious() {
        List<Component> focusable = new ArrayList<>();
        collectFocusable(component, focusable);
        if (focusable.isEmpty()) {
            return;
        }
        int idx = focused != null ? focusable.indexOf(focused) : 0;
        int prev = (idx - 1 + focusable.size()) % focusable.size();
        focusable.get(prev).focus();
    }

    private void collectFocusable(Component comp, List<Component> result) {
        if (comp == null) {
            return;
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                collectFocusable(child, result);
            }
        } else if (!comp.getBehaviors().contains(Behavior.NoFocus)) {
            result.add(comp);
        }
    }

    private boolean tryFocusShortcut(Container container, KeyEvent event) {
        if (event.getType() != KeyEvent.Type.Character || !event.hasModifier(KeyEvent.Modifier.Alt)) {
            return false;
        }
        char actualChar = Character.toLowerCase(event.getCharacter());
        return tryFocusShortcutRecursive(container, actualChar);
    }

    private boolean tryFocusShortcutRecursive(Container container, char shortcutChar) {
        for (Component child : container.getComponents()) {
            String shortcutKey = child.getShortcutKey();
            if (shortcutKey != null && shortcutKey.length() == 1) {
                char expectedChar = shortcutKey.toLowerCase().charAt(0);
                if (expectedChar == shortcutChar) {
                    child.focus();
                    return true;
                }
            }
            if (child instanceof Container && tryFocusShortcutRecursive((Container) child, shortcutChar)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleMouse(MouseEvent event) {
        // Check close button first
        if (getBehaviors().contains(Behavior.CloseButton) && !getBehaviors().contains(Behavior.NoDecoration)) {
            Position pos = getScreenPosition();
            if (event.getX() == pos.x() + getSize().w() - 2 && event.getY() == pos.y()) {
                close();
                return true;
            }
        }

        // Try to find the component under the mouse and handle the event
        if (component != null) {
            Component target = findComponentAt(component, event.getX(), event.getY());
            if (target != null) {
                // If it's a click event and the component can be focused, focus it
                if (event.getType() == MouseEvent.Type.Pressed
                        && !target.getBehaviors().contains(Behavior.NoFocus)) {
                    target.focus();
                }

                // Let the component handle the mouse event
                if (target.handleMouse(event)) {
                    return true;
                }
            }
        }

        return false; // Not handled
    }

    private Component findComponentAt(Component component, int x, int y) {
        if (!component.isIn(x, y)) {
            return null;
        }

        // If it's a container, check children first (front to back)
        if (component instanceof Container) {
            Container container = (Container) component;
            // Check children in reverse order (topmost first)
            java.util.List<Component> children = new java.util.ArrayList<>(container.getComponents());
            java.util.Collections.reverse(children);

            for (Component child : children) {
                Component found = findComponentAt(child, x, y);
                if (found != null) {
                    return found;
                }
            }
        }

        return component; // This component is the target
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
            // Use focused border style if this window has focus
            String borderStyleName = (focused != null) ? ".window.border.focused" : ".window.border";
            getTheme()
                    .box(screen, pos.x(), pos.y(), getSize().w(), getSize().h(), Curses.Border.Double, borderStyleName);
            if (getBehaviors().contains(Behavior.CloseButton)) {
                screen.text(
                        pos.x() + getSize().w() - 2,
                        pos.y(),
                        new AttributedString("x", getTheme().getStyle(".window.close")));
            }
            if (title != null) {
                String titleStyleName = (focused != null) ? ".window.title.focused" : ".window.title";
                screen.text(
                        pos.x() + 3,
                        pos.y(),
                        new AttributedString(title, getTheme().getStyle(titleStyleName)));
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
