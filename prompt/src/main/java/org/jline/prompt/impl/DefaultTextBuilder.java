/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.PromptBuilder;
import org.jline.prompt.TextBuilder;

/**
 * Default implementation of TextBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultTextBuilder implements TextBuilder {

    private final DefaultPromptBuilder parent;
    private String name;
    private String message;
    private String text;

    /**
     * Create a new DefaultTextBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultTextBuilder(DefaultPromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this text.
     *
     * @param name the name
     * @return this builder
     */
    public TextBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the text to display.
     *
     * @param text the text
     * @return this builder
     */
    public TextBuilder text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Set the message for this text display.
     *
     * @param message the message
     * @return this builder
     */
    public TextBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Add this text to the parent builder.
     */
    public PromptBuilder addPrompt() {
        DefaultTextPrompt prompt = new DefaultTextPrompt(name, message, text);
        parent.addPrompt(prompt);
        return parent;
    }
}
