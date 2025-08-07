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
import org.jline.prompt.PromptResult;

/**
 * Abstract base class for prompt results.
 */
public abstract class AbstractPromptResult<T extends Prompt> implements PromptResult<T> {

    private final T prompt;

    /**
     * Create a new AbstractPromptResult with the given prompt.
     *
     * @param prompt the associated Prompt, or null if none
     */
    protected AbstractPromptResult(T prompt) {
        this.prompt = prompt;
    }

    @Override
    public T getPrompt() {
        return prompt;
    }
}
