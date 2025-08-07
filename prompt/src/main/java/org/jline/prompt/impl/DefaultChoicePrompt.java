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

import org.jline.prompt.ChoiceItem;
import org.jline.prompt.ChoicePrompt;

/**
 * Default implementation of ChoicePrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultChoicePrompt extends DefaultPrompt implements ChoicePrompt {

    private final List<ChoiceItem> items;

    public DefaultChoicePrompt(String name, String message, List<ChoiceItem> items) {
        super(name, message);
        this.items = new ArrayList<>(items);
    }

    @Override
    public List<ChoiceItem> getItems() {
        return new ArrayList<>(items);
    }
}
