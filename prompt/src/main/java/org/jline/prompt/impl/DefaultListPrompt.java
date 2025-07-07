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

import org.jline.prompt.ListItem;
import org.jline.prompt.ListPrompt;

/**
 * Default implementation of ListPrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultListPrompt extends DefaultPrompt implements ListPrompt {

    private final List<ListItem> items;

    public DefaultListPrompt(String name, String message, List<ListItem> items) {
        super(name, message);
        this.items = new ArrayList<>(items);
    }

    @Override
    public List<ListItem> getItems() {
        return new ArrayList<>(items);
    }
}
