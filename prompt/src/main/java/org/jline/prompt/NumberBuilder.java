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
 */
public interface NumberBuilder extends PromptBuilder {

    /**
     * Set the name of the prompt.
     *
     * @param name the name
     * @return this builder
     */
    NumberBuilder name(String name);

    /**
     * Set the message to display.
     *
     * @param message the message
     * @return this builder
     */
    NumberBuilder message(String message);

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

    /**
     * Add the number prompt to the builder.
     *
     * @return the parent builder
     */
    PromptBuilder addPrompt();
}
