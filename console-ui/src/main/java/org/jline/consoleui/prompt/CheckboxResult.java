/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import java.util.Set;

/**
 * Result of a checkbox choice. CheckboxResult contains a {@link java.util.Set} with the
 * IDs of the selected checkbox items.
 */
public class CheckboxResult implements PromptResultItemIF {
    Set<String> selectedIds;

    /**
     * Default Constructor.
     * @param selectedIds Selected IDs.
     */
    public CheckboxResult(Set<String> selectedIds) {
        this.selectedIds = selectedIds;
    }

    /**
     * Returns the set with the IDs of selected checkbox items.
     *
     * @return set with IDs
     */
    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    public String getResult() {
        return selectedIds.toString();
    }

    @Override
    public String toString() {
        return "CheckboxResult{" + "selectedIds=" + selectedIds + '}';
    }
}
