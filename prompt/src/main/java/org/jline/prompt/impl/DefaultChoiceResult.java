/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ChoicePrompt;
import org.jline.prompt.ChoiceResult;

/**
 * Implementation of ChoiceResult.
 */
public class DefaultChoiceResult extends AbstractPromptResult<ChoicePrompt> implements ChoiceResult {

    private final String selectedId;

    /**
     * Create a new DefaultChoiceResult with the given selected ID and item.
     *
     * @param selectedId the ID of the selected item
     * @param item the associated PromptItem, or null if none
     */
    public DefaultChoiceResult(String selectedId, ChoicePrompt prompt) {
        super(prompt);
        this.selectedId = selectedId;
    }

    @Override
    public String getSelectedId() {
        return selectedId;
    }

    @Override
    public String getResult() {
        return selectedId;
    }

    @Override
    public String toString() {
        return "ChoiceResult{selectedId='" + selectedId + "'}";
    }
}
