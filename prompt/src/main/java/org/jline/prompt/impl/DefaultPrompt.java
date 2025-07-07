/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.Prompt;

/**
 * Base implementation class for all prompt types.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public abstract class DefaultPrompt implements Prompt {

    protected final String name;
    protected final String message;

    protected DefaultPrompt(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
