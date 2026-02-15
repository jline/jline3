/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import java.util.EnumSet;

import org.jline.terminal.KeyEvent;
import org.jline.terminal.MouseEvent;

public interface Component {

    Position getPosition();

    void setPosition(Position position);

    Position getScreenPosition();

    boolean isIn(int x, int y);

    Size getSize();

    void setSize(Size size);

    Container getParent();

    Size getPreferredSize();

    boolean isFocused();

    boolean isEnabled();

    void enable(boolean enabled);

    void focus();

    void draw(Screen screen);

    EnumSet<Behavior> getBehaviors();

    enum Behavior {
        NoFocus,
        FullScreen,
        NoDecoration,
        CloseButton,
        ManualLayout,
        Popup,
        Modal
    }

    boolean handleMouse(MouseEvent event);

    /**
     * Handle a key event.
     * @param event the key event to handle
     * @return true if the event was handled, false otherwise
     */
    boolean handleKey(KeyEvent event);

    /**
     * Marks this component as needing to be repainted.
     * This will trigger a redraw on the next render cycle.
     */
    void invalidate();

    /**
     * Returns true if this component needs to be repainted.
     * @return true if the component is invalid and needs repainting
     */
    boolean isInvalid();

    /**
     * Gets the shortcut key for this component, if any.
     * @return the shortcut key, or null if none
     */
    default String getShortcutKey() {
        return null;
    }
}
