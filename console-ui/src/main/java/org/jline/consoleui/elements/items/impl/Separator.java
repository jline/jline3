/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements.items.impl;

import org.jline.consoleui.elements.items.CheckboxItemIF;
import org.jline.consoleui.elements.items.ChoiceItemIF;
import org.jline.consoleui.elements.items.ListItemIF;

public class Separator implements CheckboxItemIF, ListItemIF, ChoiceItemIF {
    private String message;

    public Separator(String message) {
        this.message = message;
    }

    public Separator() {}

    public String getMessage() {
        return message;
    }

    public String getText() {
        return message;
    }

    public boolean isSelectable() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }
}
