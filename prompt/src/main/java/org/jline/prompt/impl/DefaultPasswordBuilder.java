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

import org.jline.prompt.PasswordBuilder;
import org.jline.prompt.PromptBuilder;

/**
 * Default implementation of PasswordBuilder.
 */
public class DefaultPasswordBuilder implements PasswordBuilder {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private String defaultValue;
    private Character mask = '*';
    private boolean showMask = true;
    private Function<String, String> transformer;
    private Function<String, String> filter;

    public DefaultPasswordBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public PasswordBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public PasswordBuilder message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public PasswordBuilder defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    public PasswordBuilder mask(Character mask) {
        this.mask = mask;
        return this;
    }

    @Override
    public PasswordBuilder showMask(boolean showMask) {
        this.showMask = showMask;
        return this;
    }

    @Override
    public PasswordBuilder transformer(Function<String, String> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public PasswordBuilder filter(Function<String, String> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        DefaultPasswordPrompt prompt = new DefaultPasswordPrompt(name, message, defaultValue, mask, showMask);
        prompt.setTransformer(transformer);
        prompt.setFilter(filter);
        parent.addPrompt(prompt);
        return parent;
    }
}
