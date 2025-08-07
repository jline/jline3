/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.Set;

import org.jline.prompt.CheckboxPrompt;
import org.jline.prompt.CheckboxResult;

/**
 * Implementation of CheckboxResult.
 */
public class DefaultCheckboxResult extends AbstractPromptResult<CheckboxPrompt> implements CheckboxResult {

    private final Set<String> selectedIds;

    /**
     * Create a new DefaultCheckboxResult with the given selected IDs and item.
     *
     * @param selectedIds the IDs of the selected items
     * @param item the associated PromptItem, or null if none
     */
    public DefaultCheckboxResult(Set<String> selectedIds, CheckboxPrompt prompt) {
        super(prompt);
        this.selectedIds = selectedIds;
    }

    @Override
    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    @Override
    public String getResult() {
        return selectedIds.toString();
    }

    @Override
    public String toString() {
        return "CheckboxResult{selectedIds=" + selectedIds + "}";
    }
}
