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

import org.jline.prompt.KeyPressBuilder;
import org.jline.prompt.PromptBuilder;

/**
 * Default implementation of KeyPressBuilder.
 */
public class DefaultKeyPressBuilder implements KeyPressBuilder {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private String hint = "Press any key to continue...";
    private Function<String, String> transformer;
    private Function<String, String> filter;

    public DefaultKeyPressBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public KeyPressBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public KeyPressBuilder message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public KeyPressBuilder hint(String hint) {
        this.hint = hint;
        return this;
    }

    @Override
    public KeyPressBuilder transformer(Function<String, String> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public KeyPressBuilder filter(Function<String, String> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        DefaultKeyPressPrompt prompt = new DefaultKeyPressPrompt(name, message, hint);
        prompt.setTransformer(transformer);
        prompt.setFilter(filter);
        parent.addPrompt(prompt);
        return parent;
    }
}
