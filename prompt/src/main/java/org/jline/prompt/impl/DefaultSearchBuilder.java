/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.List;
import java.util.function.Function;

import org.jline.prompt.PromptBuilder;
import org.jline.prompt.SearchBuilder;

/**
 * Default implementation of SearchBuilder.
 */
public class DefaultSearchBuilder<T> implements SearchBuilder<T> {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private Function<String, List<T>> searchFunction;
    private Function<T, String> displayFunction;
    private Function<T, String> valueFunction;
    private String placeholder = "Type to search...";
    private int minSearchLength = 0;
    private int maxResults = 10;
    private Function<String, String> transformer;
    private Function<String, String> filter;

    public DefaultSearchBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public SearchBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public SearchBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public SearchBuilder<T> searchFunction(Function<String, List<T>> searchFunction) {
        this.searchFunction = searchFunction;
        return this;
    }

    @Override
    public SearchBuilder<T> displayFunction(Function<T, String> displayFunction) {
        this.displayFunction = displayFunction;
        return this;
    }

    @Override
    public SearchBuilder<T> valueFunction(Function<T, String> valueFunction) {
        this.valueFunction = valueFunction;
        return this;
    }

    @Override
    public SearchBuilder<T> placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public SearchBuilder<T> minSearchLength(int minSearchLength) {
        this.minSearchLength = minSearchLength;
        return this;
    }

    @Override
    public SearchBuilder<T> maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public SearchBuilder<T> transformer(Function<String, String> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public SearchBuilder<T> filter(Function<String, String> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        DefaultSearchPrompt<T> prompt = new DefaultSearchPrompt<>(
                name,
                message,
                searchFunction,
                displayFunction,
                valueFunction,
                placeholder,
                minSearchLength,
                maxResults);
        prompt.setTransformer(transformer);
        prompt.setFilter(filter);
        parent.addPrompt(prompt);
        return parent;
    }
}
