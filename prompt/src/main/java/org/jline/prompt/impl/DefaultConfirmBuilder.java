/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ConfirmBuilder;
import org.jline.prompt.PromptBuilder;

/**
 * Default implementation of ConfirmBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultConfirmBuilder implements ConfirmBuilder {

    private final DefaultPromptBuilder parent;
    private String name;
    private String message;
    private boolean defaultValue;

    /**
     * Create a new DefaultConfirmBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultConfirmBuilder(DefaultPromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this prompt.
     *
     * @param name the name
     * @return this builder
     */
    public ConfirmBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the message to display to the user.
     *
     * @param message the message
     * @return this builder
     */
    public ConfirmBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Set the default value.
     *
     * @param defaultValue the default value
     * @return this builder
     */
    public ConfirmBuilder defaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Add this prompt to the parent builder.
     */
    public PromptBuilder addPrompt() {
        DefaultConfirmPrompt prompt = new DefaultConfirmPrompt(name, message, defaultValue);
        parent.addPrompt(prompt);
        return parent;
    }
}
