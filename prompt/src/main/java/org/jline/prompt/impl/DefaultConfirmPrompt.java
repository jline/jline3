/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ConfirmPrompt;

/**
 * Default implementation of ConfirmPrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultConfirmPrompt extends DefaultPrompt implements ConfirmPrompt {

    private final boolean defaultValue;

    public DefaultConfirmPrompt(String name, String message, boolean defaultValue) {
        super(name, message);
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean getDefaultValue() {
        return defaultValue;
    }
}
