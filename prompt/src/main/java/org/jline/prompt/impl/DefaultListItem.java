/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ListItem;

/**
 * Default implementation of ListItem interface.
 */
public class DefaultListItem implements ListItem {

    private final String name;
    private final String text;
    private final boolean disabled;
    private final String disabledText;

    public DefaultListItem(String name, String text, boolean disabled, String disabledText) {
        this.name = name;
        this.text = text;
        this.disabled = disabled;
        this.disabledText = disabledText;
    }

    public DefaultListItem(String name, String text) {
        this(name, text, false, null);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public String getDisabledText() {
        return disabledText;
    }

    @Override
    public String toString() {
        return "DefaultListItem{name='" + name + "', text='" + text + "', disabled=" + disabled + "}";
    }
}
