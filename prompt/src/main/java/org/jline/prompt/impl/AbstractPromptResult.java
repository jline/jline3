/*
 * Copyright (c) the original author(s).
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
    private String filteredResult;

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

    /**
     * Apply a filter to this result. If set, {@link #getResult()} will return
     * the filtered value instead of the original.
     *
     * @param filteredResult the filtered result value
     */
    public void applyFilter(String filteredResult) {
        this.filteredResult = filteredResult;
    }

    /**
     * Returns the filtered result if a filter was applied, otherwise delegates
     * to the subclass implementation via {@link #getRawResult()}.
     */
    @Override
    public final String getResult() {
        return filteredResult != null ? filteredResult : getRawResult();
    }

    /**
     * Get the raw (unfiltered) result value. Subclasses must implement this
     * instead of {@link #getResult()}.
     *
     * @return the raw result value
     */
    protected abstract String getRawResult();
}
