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
import java.util.List;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.terminal.KeyEvent;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A checkbox component that can be toggled on and off.
 */
public class Checkbox extends AbstractComponent {

    private String text;
    private boolean checked;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public Checkbox(String text) {
        this.text = text != null ? text : "";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            fireChangeListeners();
        }
    }

    public void toggle() {
        setChecked(!checked);
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void fireChangeListeners() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        if (event.getType() == KeyEvent.Type.Special && event.getSpecial() == KeyEvent.Special.Enter) {
            toggle();
            return true;
        }
        if (event.getType() == KeyEvent.Type.Character && event.getCharacter() == ' ') {
            toggle();
            return true;
        }
        if (event.getType() == KeyEvent.Type.Arrow) {
            if (event.getArrow() == KeyEvent.Arrow.Up || event.getArrow() == KeyEvent.Arrow.Left) {
                return focusSibling(-1);
            }
            if (event.getArrow() == KeyEvent.Arrow.Down || event.getArrow() == KeyEvent.Arrow.Right) {
                return focusSibling(1);
            }
        }
        return false;
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        Position pos = getScreenPosition();
        if (size == null || pos == null) {
            return;
        }

        AttributedStyle style = resolveStyle(
                isFocused() ? ".checkbox.focused" : ".checkbox.normal",
                isFocused() ? AttributedStyle.DEFAULT.inverse() : AttributedStyle.DEFAULT);

        String display = (checked ? "[x] " : "[ ] ") + text;
        if (display.length() > size.w()) {
            display = display.substring(0, size.w());
        }

        screen.fill(pos.x(), pos.y(), size.w(), size.h(), style);
        screen.text(pos.x(), pos.y(), new AttributedString(display, style));
    }

    @Override
    protected Size doGetPreferredSize() {
        return new Size(text.length() + 4, 1);
    }
}
