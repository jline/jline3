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
import org.jline.prompt.ConfirmResult;

/**
 * Implementation of ConfirmResult.
 */
public class DefaultConfirmResult extends AbstractPromptResult<ConfirmPrompt> implements ConfirmResult {

    private final ConfirmationValue confirmed;

    /**
     * Create a new DefaultConfirmResult with the given confirmation value and item.
     *
     * @param confirmed the confirmation value
     * @param item the associated PromptItem, or null if none
     */
    public DefaultConfirmResult(ConfirmationValue confirmed, ConfirmPrompt prompt) {
        super(prompt);
        this.confirmed = confirmed;
    }

    @Override
    public ConfirmationValue getConfirmed() {
        return confirmed;
    }

    @Override
    public boolean isConfirmed() {
        return confirmed == ConfirmationValue.YES;
    }

    @Override
    public String getResult() {
        return confirmed.toString();
    }

    @Override
    public String toString() {
        return "ConfirmResult{confirmed=" + confirmed + "}";
    }
}
