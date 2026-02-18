/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.function.Function;

import org.jline.prompt.PromptBuilder;
import org.jline.prompt.ToggleBuilder;

/**
 * Default implementation of ToggleBuilder.
 */
public class DefaultToggleBuilder implements ToggleBuilder {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private String activeLabel = "Yes";
    private String inactiveLabel = "No";
    private boolean defaultValue;
    private Function<String, String> transformer;
    private Function<String, String> filter;

    public DefaultToggleBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public ToggleBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public ToggleBuilder message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public ToggleBuilder activeLabel(String label) {
        this.activeLabel = label;
        return this;
    }

    @Override
    public ToggleBuilder inactiveLabel(String label) {
        this.inactiveLabel = label;
        return this;
    }

    @Override
    public ToggleBuilder defaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    public ToggleBuilder transformer(Function<String, String> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public ToggleBuilder filter(Function<String, String> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        DefaultTogglePrompt prompt = new DefaultTogglePrompt(name, message, activeLabel, inactiveLabel, defaultValue);
        prompt.setTransformer(transformer);
        prompt.setFilter(filter);
        parent.addPrompt(prompt);
        return parent;
    }
}
