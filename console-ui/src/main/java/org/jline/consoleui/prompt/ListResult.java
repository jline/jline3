/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

/**
 * Result of a list choice. Holds the id of the selected item.
 */
public class ListResult implements PromptResultItemIF {

    String selectedId;

    /**
     * Returns the ID of the selected item.
     *
     * @return id of selected item
     */
    public String getSelectedId() {
        return selectedId;
    }

    public String getResult() {
        return selectedId;
    }

    /**
     * Default constructor.
     *
     * @param selectedId id of selected item.
     */
    public ListResult(String selectedId) {
        this.selectedId = selectedId;
    }

    @Override
    public String toString() {
        return "ListResult{" + "selectedId='" + selectedId + '\'' + '}';
    }
}
