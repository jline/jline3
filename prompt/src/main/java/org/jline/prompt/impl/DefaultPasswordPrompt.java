/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.PasswordPrompt;
import org.jline.reader.Completer;

/**
 * Default implementation of PasswordPrompt interface.
 */
public class DefaultPasswordPrompt extends DefaultInputPrompt implements PasswordPrompt {

    private final boolean showMask;

    public DefaultPasswordPrompt(String name, String message) {
        this(name, message, '*', true);
    }

    public DefaultPasswordPrompt(String name, String message, Character mask) {
        this(name, message, mask, true);
    }

    public DefaultPasswordPrompt(String name, String message, Character mask, boolean showMask) {
        super(name, message, null, mask, null, null, null);
        this.showMask = showMask;
    }

    public DefaultPasswordPrompt(String name, String message, String defaultValue, Character mask, boolean showMask) {
        super(name, message, defaultValue, mask, null, null, null);
        this.showMask = showMask;
    }

    @Override
    public boolean showMask() {
        return showMask;
    }

    @Override
    public Character getMask() {
        Character mask = super.getMask();
        return mask != null ? mask : '*';
    }

    @Override
    public Completer getCompleter() {
        // Password prompts should not have completion
        return null;
    }
}
