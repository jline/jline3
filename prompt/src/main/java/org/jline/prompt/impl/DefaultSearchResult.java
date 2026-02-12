/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.SearchPrompt;
import org.jline.prompt.SearchResult;

/**
 * Implementation of SearchResult.
 *
 * @param <T> the type of items being searched
 */
public class DefaultSearchResult<T> extends AbstractPromptResult<SearchPrompt<T>> implements SearchResult<T> {

    private final String selectedValue;

    /**
     * Create a new DefaultSearchResult with the given selected value.
     *
     * @param selectedValue the selected value as a string
     * @param prompt the associated SearchPrompt
     */
    public DefaultSearchResult(String selectedValue, SearchPrompt<T> prompt) {
        super(prompt);
        this.selectedValue = selectedValue;
    }

    @Override
    public String getSelectedValue() {
        return selectedValue;
    }

    @Override
    public String getResult() {
        return selectedValue;
    }

    @Override
    public String getDisplayResult() {
        return selectedValue;
    }

    @Override
    public String toString() {
        return "SearchResult{selectedValue='" + selectedValue + "'}";
    }
}
