/*
 * Copyright (c) the original author(s).
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
    private final int pageSize;
    private final boolean showPageIndicator;
    private final boolean filterable;

    public DefaultListPrompt(String name, String message, List<ListItem> items) {
        this(name, message, items, 0, true, true);
    }

    public DefaultListPrompt(String name, String message, List<ListItem> items, int pageSize) {
        this(name, message, items, pageSize, true, true);
    }

    public DefaultListPrompt(
            String name, String message, List<ListItem> items, int pageSize, boolean showPageIndicator) {
        this(name, message, items, pageSize, showPageIndicator, true);
    }

    public DefaultListPrompt(
            String name,
            String message,
            List<ListItem> items,
            int pageSize,
            boolean showPageIndicator,
            boolean filterable) {
        super(name, message);
        this.items = new ArrayList<>(items);
        this.pageSize = pageSize;
        this.showPageIndicator = showPageIndicator;
        this.filterable = filterable;
    }

    @Override
    public List<ListItem> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public boolean showPageIndicator() {
        return showPageIndicator;
    }

    @Override
    public boolean isFilterable() {
        return filterable;
    }
}
