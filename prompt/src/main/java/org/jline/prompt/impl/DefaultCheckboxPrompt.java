/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.prompt.CheckboxItem;
import org.jline.prompt.CheckboxPrompt;

/**
 * Default implementation of CheckboxPrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultCheckboxPrompt extends DefaultPrompt implements CheckboxPrompt {

    private final List<CheckboxItem> items;

    public DefaultCheckboxPrompt(String name, String message, List<CheckboxItem> items) {
        super(name, message);
        this.items = new ArrayList<>(items);
    }

    @Override
    public List<CheckboxItem> getItems() {
        return new ArrayList<>(items);
    }
}
