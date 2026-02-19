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

import org.jline.prompt.CheckboxItem;
import org.jline.prompt.CheckboxPrompt;

/**
 * Default implementation of CheckboxPrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultCheckboxPrompt extends DefaultPrompt implements CheckboxPrompt {

    private final List<CheckboxItem> items;
    private final int pageSize;
    private final boolean showPageIndicator;
    private final int minSelections;
    private final int maxSelections;

    public DefaultCheckboxPrompt(String name, String message, List<CheckboxItem> items) {
        this(name, message, items, 0, true, 0, 0);
    }

    public DefaultCheckboxPrompt(String name, String message, List<CheckboxItem> items, int pageSize) {
        this(name, message, items, pageSize, true, 0, 0);
    }

    public DefaultCheckboxPrompt(
            String name, String message, List<CheckboxItem> items, int pageSize, boolean showPageIndicator) {
        this(name, message, items, pageSize, showPageIndicator, 0, 0);
    }

    public DefaultCheckboxPrompt(
            String name,
            String message,
            List<CheckboxItem> items,
            int pageSize,
            boolean showPageIndicator,
            int minSelections,
            int maxSelections) {
        super(name, message);
        this.items = new ArrayList<>(items);
        this.pageSize = pageSize;
        this.showPageIndicator = showPageIndicator;
        this.minSelections = minSelections;
        this.maxSelections = maxSelections;
    }

    @Override
    public List<CheckboxItem> getItems() {
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
    public int getMinSelections() {
        return minSelections;
    }

    @Override
    public int getMaxSelections() {
        return maxSelections;
    }
}
