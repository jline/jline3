/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements.items.impl;

import org.jline.consoleui.elements.items.ChoiceItemIF;

public class ChoiceItem implements ChoiceItemIF {
    private final Character key;
    private final String name;
    private final String message;
    private final boolean defaultChoice;

    public ChoiceItem(Character key, String name, String message, boolean isDefaultChoice) {
        this.key = key;
        this.name = name;
        this.message = message;
        this.defaultChoice = isDefaultChoice;
    }

    public Character getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getText() {
        return message;
    }

    public boolean isSelectable() {
        return true;
    }

    public boolean isDefaultChoice() {
        return defaultChoice;
    }
}
