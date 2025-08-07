/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ListPrompt;
import org.jline.prompt.ListResult;

/**
 * Implementation of ListResult.
 */
public class DefaultListResult extends AbstractPromptResult<ListPrompt> implements ListResult {

    private final String selectedId;

    /**
     * Create a new DefaultListResult with the given selected ID and prompt.
     *
     * @param selectedId the ID of the selected item
     * @param prompt the associated ListPrompt, or null if none
     */
    public DefaultListResult(String selectedId, ListPrompt prompt) {
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
        return "ListResult{selectedId='" + selectedId + "'}";
    }
}
