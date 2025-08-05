/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.List;
import java.util.function.Function;

import org.jline.prompt.SearchPrompt;

/**
 * Default implementation of SearchPrompt interface.
 */
public class DefaultSearchPrompt<T> extends DefaultPrompt implements SearchPrompt<T> {

    private final Function<String, List<T>> searchFunction;
    private final Function<T, String> displayFunction;
    private final Function<T, String> valueFunction;
    private final String placeholder;
    private final int minSearchLength;
    private final int maxResults;

    public DefaultSearchPrompt(
            String name,
            String message,
            Function<String, List<T>> searchFunction,
            Function<T, String> displayFunction,
            Function<T, String> valueFunction) {
        this(name, message, searchFunction, displayFunction, valueFunction, "Type to search...", 0, 10);
    }

    public DefaultSearchPrompt(
            String name,
            String message,
            Function<String, List<T>> searchFunction,
            Function<T, String> displayFunction,
            Function<T, String> valueFunction,
            String placeholder,
            int minSearchLength,
            int maxResults) {
        super(name, message);
        this.searchFunction = searchFunction;
        this.displayFunction = displayFunction;
        this.valueFunction = valueFunction;
        this.placeholder = placeholder;
        this.minSearchLength = minSearchLength;
        this.maxResults = maxResults;
    }

    @Override
    public Function<String, List<T>> getSearchFunction() {
        return searchFunction;
    }

    @Override
    public Function<T, String> getDisplayFunction() {
        return displayFunction;
    }

    @Override
    public Function<T, String> getValueFunction() {
        return valueFunction;
    }

    @Override
    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public int getMinSearchLength() {
        return minSearchLength;
    }

    @Override
    public int getMaxResults() {
        return maxResults;
    }
}
