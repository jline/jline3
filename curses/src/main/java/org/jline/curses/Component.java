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
        Popup
    }

    void handleMouse(MouseEvent event);

    void handleInput(String input);
}
