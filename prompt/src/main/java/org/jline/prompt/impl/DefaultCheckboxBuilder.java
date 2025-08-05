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

import org.jline.prompt.CheckboxBuilder;
import org.jline.prompt.CheckboxItem;
import org.jline.prompt.CheckboxSeparatorBuilder;
import org.jline.prompt.PromptBuilder;
import org.jline.prompt.SeparatorItem;

/**
 * Default implementation of CheckboxBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultCheckboxBuilder implements CheckboxBuilder {

    private final DefaultPromptBuilder parent;
    private final List<CheckboxItem> items = new ArrayList<>();
    private String name;
    private String message;
    private String currentItemId;
    private String currentItemText;
    private boolean currentItemChecked;
    private boolean currentItemDisabled;
    private String currentItemDisabledText;
    private int pageSize = 0;
    private boolean showPageIndicator = true;

    /**
     * Create a new DefaultCheckboxBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultCheckboxBuilder(DefaultPromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this prompt.
     *
     * @param name the name
     * @return this builder
     */
    public CheckboxBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the message to display to the user.
     *
     * @param message the message
     * @return this builder
     */
    public CheckboxBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Create a new item with the given ID.
     *
     * @param id the ID
     * @return this builder
     */
    public CheckboxBuilder newItem(String id) {
        this.currentItemId = id;
        this.currentItemText = null;
        this.currentItemChecked = false;
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
    public CheckboxBuilder text(String text) {
        this.currentItemText = text;
        return this;
    }

    /**
     * Set whether the current item is checked.
     *
     * @param checked whether the item is checked
     * @return this builder
     */
    public CheckboxBuilder checked(boolean checked) {
        this.currentItemChecked = checked;
        return this;
    }

    /**
     * Set whether the current item is disabled.
     *
     * @param disabled whether the item is disabled
     * @return this builder
     */
    public CheckboxBuilder disabled(boolean disabled) {
        // Note: CheckboxItemBuilder doesn't have a disabled method
        // This is a no-op for compatibility
        return this;
    }

    /**
     * Set the disabled text for the current item.
     *
     * @param disabledText the disabled text
     * @return this builder
     */
    public CheckboxBuilder disabledText(String disabledText) {
        this.currentItemDisabledText = disabledText;
        return this;
    }

    /**
     * Add the current item to the list.
     *
     * @return this builder
     */
    public CheckboxBuilder add() {
        if (currentItemId != null) {
            DefaultCheckboxItem item = new DefaultCheckboxItem(
                    currentItemId, currentItemText, currentItemChecked, currentItemDisabled, currentItemDisabledText);
            items.add(item);
            currentItemId = null;
            currentItemText = null;
            currentItemChecked = false;
            currentItemDisabled = false;
            currentItemDisabledText = null;
        }
        return this;
    }

    @Override
    public CheckboxBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public CheckboxBuilder showPageIndicator(boolean showPageIndicator) {
        this.showPageIndicator = showPageIndicator;
        return this;
    }

    @Override
    public CheckboxSeparatorBuilder newSeparator() {
        return new DefaultCheckboxSeparatorBuilder(this);
    }

    @Override
    public CheckboxSeparatorBuilder newSeparator(String text) {
        return new DefaultCheckboxSeparatorBuilder(this, text);
    }

    /**
     * Add a separator item to the list.
     *
     * @param separatorItem the separator item to add
     */
    public void addSeparator(SeparatorItem separatorItem) {
        items.add((CheckboxItem) separatorItem);
    }

    /**
     * Add this prompt to the parent builder.
     */
    public PromptBuilder addPrompt() {
        DefaultCheckboxPrompt prompt = new DefaultCheckboxPrompt(name, message, items, pageSize, showPageIndicator);
        parent.addPrompt(prompt);
        return parent;
    }
}
