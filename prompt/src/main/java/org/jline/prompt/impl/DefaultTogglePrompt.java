/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.TogglePrompt;

/**
 * Default implementation of TogglePrompt.
 */
public class DefaultTogglePrompt extends DefaultPrompt implements TogglePrompt {

    private final String activeLabel;
    private final String inactiveLabel;
    private final boolean defaultValue;

    public DefaultTogglePrompt(
            String name, String message, String activeLabel, String inactiveLabel, boolean defaultValue) {
        super(name, message);
        this.activeLabel = activeLabel;
        this.inactiveLabel = inactiveLabel;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getActiveLabel() {
        return activeLabel;
    }

    @Override
    public String getInactiveLabel() {
        return inactiveLabel;
    }

    @Override
    public boolean getDefaultValue() {
        return defaultValue;
    }
}
