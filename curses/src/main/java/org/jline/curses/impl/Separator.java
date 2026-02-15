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

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A separator component that draws a horizontal or vertical line.
 */
public class Separator extends AbstractComponent {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private Orientation orientation;

    public Separator() {
        this(Orientation.HORIZONTAL);
    }

    public Separator(Orientation orientation) {
        this.orientation = orientation;
        setBehaviors(EnumSet.of(Behavior.NoFocus));
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        Position pos = getScreenPosition();
        if (size == null || pos == null) {
            return;
        }

        AttributedStyle style = resolveStyle(".separator", AttributedStyle.DEFAULT);

        if (orientation == Orientation.HORIZONTAL) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size.w(); i++) {
                sb.append('\u2500'); // ─
            }
            screen.text(pos.x(), pos.y(), new AttributedString(sb.toString(), style));
        } else {
            for (int i = 0; i < size.h(); i++) {
                screen.text(pos.x(), pos.y() + i, new AttributedString("\u2502", style)); // │
            }
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        return new Size(1, 1);
    }
}
