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
import java.util.Collections;
import java.util.List;

/**
 * A group that manages mutual exclusion among radio buttons.
 */
public class RadioGroup {

    private final List<RadioButton> buttons = new ArrayList<>();
    private RadioButton selected;

    public RadioGroup(RadioButton... buttons) {
        if (buttons != null) {
            for (RadioButton button : buttons) {
                add(button);
            }
        }
    }

    public void add(RadioButton button) {
        if (button != null && !buttons.contains(button)) {
            buttons.add(button);
            button.setGroup(this);
            if (button.isSelected()) {
                select(button);
            }
        }
    }

    public void remove(RadioButton button) {
        if (button != null && buttons.remove(button)) {
            button.setGroup(null);
            if (selected == button) {
                selected = null;
            }
        }
    }

    public List<RadioButton> getButtons() {
        return Collections.unmodifiableList(buttons);
    }

    public RadioButton getSelected() {
        return selected;
    }

    public void select(RadioButton button) {
        if (button != null && buttons.contains(button)) {
            if (selected != null && selected != button) {
                selected.setSelectedInternal(false);
            }
            selected = button;
            if (!button.isSelected()) {
                button.setSelectedInternal(true);
            }
        }
    }

    public int getSelectedIndex() {
        return selected != null ? buttons.indexOf(selected) : -1;
    }
}
