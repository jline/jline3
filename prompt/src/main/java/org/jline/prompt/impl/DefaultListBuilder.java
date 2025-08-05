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

import org.jline.prompt.ListBuilder;
import org.jline.prompt.ListItem;
import org.jline.prompt.ListSeparatorBuilder;
import org.jline.prompt.PromptBuilder;
import org.jline.prompt.SeparatorItem;

/**
 * Default implementation of ListBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultListBuilder implements ListBuilder {

    private final DefaultPromptBuilder parent;
    private final List<ListItem> items = new ArrayList<>();
    private String name;
    private String message;
    private String currentItemId;
    private String currentItemText;
    private boolean currentItemDisabled;
    private String currentItemDisabledText;
    private int pageSize = 0;
    private boolean showPageIndicator = true;

    /**
     * Create a new DefaultListBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultListBuilder(DefaultPromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this prompt.
     *
     * @param name the name
     * @return this builder
     */
    public ListBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the message to display to the user.
     *
     * @param message the message
     * @return this builder
     */
    public ListBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Create a new item with the given ID.
     *
     * @param id the ID
     * @return this builder
     */
    public ListBuilder newItem(String id) {
        this.currentItemId = id;
        this.currentItemText = null;
        this.currentItemDisabled = false;
        this.currentItemDisabledText = null;
        return this;
    }

    /**
     * Set the text for the current item.
     *
     * @param text the text
     * @return this builder
     */
    public ListBuilder text(String text) {
        this.currentItemText = text;
        return this;
    }

    /**
     * Set whether the current item is disabled.
     *
     * @param disabled whether the item is disabled
     * @return this builder
     */
    public ListBuilder disabled(boolean disabled) {
        this.currentItemDisabled = disabled;
        return this;
    }

    /**
     * Set the disabled text for the current item.
     *
     * @param disabledText the disabled text
     * @return this builder
     */
    public ListBuilder disabledText(String disabledText) {
        this.currentItemDisabledText = disabledText;
        return this;
    }

    /**
     * Add the current item to the list.
     *
     * @return this builder
     */
    public ListBuilder add() {
        if (currentItemId != null) {
            DefaultListItem item =
                    new DefaultListItem(currentItemId, currentItemText, currentItemDisabled, currentItemDisabledText);
            items.add(item);
            currentItemId = null;
            currentItemText = null;
            currentItemDisabled = false;
            currentItemDisabledText = null;
        }
        return this;
    }

    @Override
    public ListBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public ListBuilder showPageIndicator(boolean showPageIndicator) {
        this.showPageIndicator = showPageIndicator;
        return this;
    }

    @Override
    public ListSeparatorBuilder newSeparator() {
        return new DefaultListSeparatorBuilder(this);
    }

    @Override
    public ListSeparatorBuilder newSeparator(String text) {
        return new DefaultListSeparatorBuilder(this, text);
    }

    /**
     * Add a separator item to the list.
     *
     * @param separatorItem the separator item to add
     */
    public void addSeparator(SeparatorItem separatorItem) {
        items.add((ListItem) separatorItem);
    }

    /**
     * Add this prompt to the parent builder and return to the parent.
     *
     * @return the parent prompt builder
     */
    public PromptBuilder addPrompt() {
        DefaultListPrompt prompt = new DefaultListPrompt(name, message, items, pageSize, showPageIndicator);
        parent.addPrompt(prompt);
        return parent;
    }
}
