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
 * Builder interface for number prompts.
 *
 * <p>
 * NumberBuilder extends {@link BaseBuilder} to provide a consistent fluent API
 * for creating number prompts. The name() and message() methods are inherited
 * from BaseBuilder, ensuring API consistency across all prompt types.
 * </p>
 *
 * @see BaseBuilder
 * @see NumberPrompt
 */
public interface NumberBuilder extends BaseBuilder<NumberBuilder> {

    /**
     * Set the default value.
     *
     * @param defaultValue the default value
     * @return this builder
     */
    NumberBuilder defaultValue(String defaultValue);

    /**
     * Set the minimum allowed value.
     *
     * @param min the minimum value
     * @return this builder
     */
    NumberBuilder min(Double min);

    /**
     * Set the maximum allowed value.
     *
     * @param max the maximum value
     * @return this builder
     */
    NumberBuilder max(Double max);

    /**
     * Set whether to allow decimal numbers.
     *
     * @param allowDecimals true to allow decimals, false for integers only
     * @return this builder
     */
    NumberBuilder allowDecimals(boolean allowDecimals);

    /**
     * Set the error message for invalid numbers.
     *
     * @param message the error message
     * @return this builder
     */
    NumberBuilder invalidNumberMessage(String message);

    /**
     * Set the error message for out-of-range numbers.
     *
     * @param message the error message
     * @return this builder
     */
    NumberBuilder outOfRangeMessage(String message);
}
