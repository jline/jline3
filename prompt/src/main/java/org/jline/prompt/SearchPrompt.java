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
import java.util.function.Function;

/**
 * Interface for search prompts.
 * A search prompt allows users to search through a list of items dynamically.
 */
public interface SearchPrompt<T> extends Prompt {

    /**
     * Get the search function that filters items based on the search term.
     *
     * @return the search function
     */
    Function<String, List<T>> getSearchFunction();

    /**
     * Get the function to convert items to display strings.
     *
     * @return the display function
     */
    Function<T, String> getDisplayFunction();

    /**
     * Get the function to convert items to their values.
     *
     * @return the value function
     */
    Function<T, String> getValueFunction();

    /**
     * Get the placeholder text to show when no search term is entered.
     *
     * @return the placeholder text
     */
    default String getPlaceholder() {
        return "Type to search...";
    }

    /**
     * Get the minimum number of characters required before searching.
     *
     * @return the minimum search length
     */
    default int getMinSearchLength() {
        return 0;
    }

    /**
     * Get the maximum number of results to display.
     *
     * @return the maximum results, or -1 for unlimited
     */
    default int getMaxResults() {
        return 10;
    }
}
