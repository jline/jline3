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

import org.jline.prompt.ChoiceBuilder;
import org.jline.prompt.ChoiceItem;
import org.jline.prompt.PromptBuilder;

/**
 * Default implementation of ChoiceBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultChoiceBuilder implements ChoiceBuilder {

    private final DefaultPromptBuilder parent;
    private final List<ChoiceItem> items = new ArrayList<>();
    private String name;
    private String message;
    private String currentItemId;
    private String currentItemText;
    private char currentItemKey;
    private String currentItemHelpText;
    private boolean currentItemDefaultChoice;

    /**
     * Create a new DefaultChoiceBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultChoiceBuilder(DefaultPromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this prompt.
     *
     * @param name the name
     * @return this builder
     */
    public ChoiceBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the message to display to the user.
     *
     * @param message the message
     * @return this builder
     */
    public ChoiceBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Create a new choice with the given ID.
     *
     * @param id the ID
     * @return this builder
     */
    public ChoiceBuilder newChoice(String id) {
        this.currentItemId = id;
        this.currentItemText = null;
        this.currentItemKey = '\0';
        this.currentItemHelpText = null;
        this.currentItemDefaultChoice = false;
        return this;
    }

    /**
     * Set the key for the current choice.
     *
     * @param key the key
     * @return this builder
     */
    public ChoiceBuilder key(char key) {
        this.currentItemKey = key;
        return this;
    }

    /**
     * Set the text for the current choice.
     *
     * @param text the text
     * @return this builder
     */
    public ChoiceBuilder text(String text) {
        this.currentItemText = text;
        return this;
    }

    /**
     * Set the help text for the current choice.
     *
     * @param helpText the help text
     * @return this builder
     */
    public ChoiceBuilder helpText(String helpText) {
        this.currentItemHelpText = helpText;
        return this;
    }

    /**
     * Set whether the current choice is the default.
     *
     * @param defaultChoice whether the choice is the default
     * @return this builder
     */
    public ChoiceBuilder defaultChoice(boolean defaultChoice) {
        this.currentItemDefaultChoice = defaultChoice;
        return this;
    }

    /**
     * Add the current choice to the list.
     *
     * @return this builder
     */
    public ChoiceBuilder add() {
        if (currentItemId != null) {
            DefaultChoiceItem item = new DefaultChoiceItem(
                    currentItemId, currentItemText, currentItemKey, currentItemHelpText, currentItemDefaultChoice);
            items.add(item);
            currentItemId = null;
            currentItemText = null;
            currentItemKey = '\0';
            currentItemHelpText = null;
            currentItemDefaultChoice = false;
        }
        return this;
    }

    /**
     * Add this prompt to the parent builder.
     */
    public PromptBuilder addPrompt() {
        DefaultChoicePrompt prompt = new DefaultChoicePrompt(name, message, items);
        parent.addPrompt(prompt);
        return parent;
    }
}
