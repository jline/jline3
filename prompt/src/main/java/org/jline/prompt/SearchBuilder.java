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
 * Builder interface for search prompts.
 *
 * <p>
 * SearchBuilder extends {@link BaseBuilder} to provide a consistent fluent API
 * for creating search prompts. The name() and message() methods are inherited
 * from BaseBuilder, ensuring API consistency across all prompt types.
 * </p>
 *
 * @param <T> the type of items being searched
 * @see BaseBuilder
 * @see SearchPrompt
 */
public interface SearchBuilder<T> extends BaseBuilder<SearchBuilder<T>> {

    /**
     * Set the search function.
     *
     * @param searchFunction the function to search items
     * @return this builder
     */
    SearchBuilder<T> searchFunction(Function<String, List<T>> searchFunction);

    /**
     * Set the display function.
     *
     * @param displayFunction the function to convert items to display strings
     * @return this builder
     */
    SearchBuilder<T> displayFunction(Function<T, String> displayFunction);

    /**
     * Set the value function.
     *
     * @param valueFunction the function to convert items to values
     * @return this builder
     */
    SearchBuilder<T> valueFunction(Function<T, String> valueFunction);

    /**
     * Set the placeholder text.
     *
     * @param placeholder the placeholder text
     * @return this builder
     */
    SearchBuilder<T> placeholder(String placeholder);

    /**
     * Set the minimum search length.
     *
     * @param minSearchLength the minimum number of characters before searching
     * @return this builder
     */
    SearchBuilder<T> minSearchLength(int minSearchLength);

    /**
     * Set the maximum number of results.
     *
     * @param maxResults the maximum results to display
     * @return this builder
     */
    SearchBuilder<T> maxResults(int maxResults);
}
