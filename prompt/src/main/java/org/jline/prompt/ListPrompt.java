/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.List;

/**
 * Interface for list prompts.
 * A list prompt allows the user to select a single item from a list.
 */
public interface ListPrompt extends Prompt {

    /**
     * Get the list of list items.
     *
     * @return the list of list items
     */
    List<ListItem> getItems();

    /**
     * Get the page size for pagination.
     * If 0 or negative, all items are shown without pagination.
     *
     * @return the page size, or 0 for no pagination
     */
    default int getPageSize() {
        return 0;
    }

    /**
     * Whether to show a loop indicator when pagination is enabled.
     * If true, shows "(1/3)" style indicators.
     *
     * @return true to show page indicators, false to hide
     */
    default boolean showPageIndicator() {
        return true;
    }
}
