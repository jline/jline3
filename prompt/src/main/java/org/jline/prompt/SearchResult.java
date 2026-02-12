/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Result of a search prompt. Contains the selected value from the search results.
 *
 * @param <T> the type of items being searched
 */
public interface SearchResult<T> extends PromptResult<SearchPrompt<T>> {

    /**
     * Get the selected value as a string.
     *
     * @return the selected value
     */
    String getSelectedValue();
}
