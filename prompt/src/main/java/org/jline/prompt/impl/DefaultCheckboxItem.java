/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.CheckboxItem;

/**
 * Default implementation of CheckboxItem interface.
 */
public class DefaultCheckboxItem implements CheckboxItem {

    private final String name;
    private final String text;
    private final boolean checked;
    private final boolean disabled;
    private final String disabledText;

    public DefaultCheckboxItem(String name, String text, boolean checked, boolean disabled, String disabledText) {
        this.name = name;
        this.text = text;
        this.checked = checked;
        this.disabled = disabled;
        this.disabledText = disabledText;
    }

    public DefaultCheckboxItem(String name, String text, boolean checked) {
        this(name, text, checked, false, null);
    }

    public DefaultCheckboxItem(String name, String text) {
        this(name, text, false, false, null);
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
    public boolean isInitiallyChecked() {
        return checked;
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
        return "DefaultCheckboxItem{name='" + name + "', text='" + text + "', checked=" + checked + ", disabled="
                + disabled + "}";
    }
}
