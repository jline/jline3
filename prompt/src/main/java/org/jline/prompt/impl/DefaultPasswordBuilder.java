/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.*;

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
    public PromptBuilder addPrompt() {
        PasswordPrompt prompt = new DefaultPasswordPrompt(name, message, defaultValue, mask, showMask);
        parent.addPrompt(prompt);
        return parent;
    }
}
