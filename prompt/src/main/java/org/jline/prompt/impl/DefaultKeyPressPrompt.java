/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.KeyPressPrompt;

/**
 * Default implementation of KeyPressPrompt.
 */
public class DefaultKeyPressPrompt extends DefaultPrompt implements KeyPressPrompt {

    private final String hint;

    public DefaultKeyPressPrompt(String name, String message, String hint) {
        super(name, message);
        this.hint = hint;
    }

    @Override
    public String getHint() {
        return hint;
    }
}
