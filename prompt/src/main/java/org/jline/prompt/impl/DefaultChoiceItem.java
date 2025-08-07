/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ChoiceItem;

/**
 * Default implementation of ChoiceItem interface.
 */
public class DefaultChoiceItem implements ChoiceItem {

    private final String name;
    private final String text;
    private final char key;
    private final String helpText;
    private final boolean defaultChoice;

    public DefaultChoiceItem(String name, String text, char key, String helpText, boolean defaultChoice) {
        this.name = name;
        this.text = text;
        this.key = key;
        this.helpText = helpText;
        this.defaultChoice = defaultChoice;
    }

    public DefaultChoiceItem(String name, String text, char key) {
        this(name, text, key, null, false);
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
    public Character getKey() {
        return key;
    }

    public String getHelpText() {
        return helpText;
    }

    @Override
    public boolean isDefaultChoice() {
        return defaultChoice;
    }

    @Override
    public String toString() {
        return "DefaultChoiceItem{name='" + name + "', text='" + text + "', key=" + key + ", defaultChoice="
                + defaultChoice + "}";
    }
}
