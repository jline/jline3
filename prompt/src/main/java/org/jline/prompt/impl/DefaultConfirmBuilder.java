/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.function.Function;

import org.jline.prompt.ConfirmBuilder;
import org.jline.prompt.PromptBuilder;

/**
 * Default implementation of ConfirmBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultConfirmBuilder implements ConfirmBuilder {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private boolean defaultValue;
    private Function<String, String> transformer;
    private Function<String, String> filter;

    /**
     * Create a new DefaultConfirmBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultConfirmBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this prompt.
     *
     * @param name the name
     * @return this builder
     */
    @Override
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
    @Override
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

    @Override
    public ConfirmBuilder transformer(Function<String, String> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public ConfirmBuilder filter(Function<String, String> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Add this prompt to the parent builder.
     */
    @Override
    public PromptBuilder addPrompt() {
        DefaultConfirmPrompt prompt = new DefaultConfirmPrompt(name, message, defaultValue);
        prompt.setTransformer(transformer);
        prompt.setFilter(filter);
        parent.addPrompt(prompt);
        return parent;
    }
}
