/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.function.Function;

/**
 * Interface for all promptable elements in the Prompter.
 */
public interface Prompt {

    /**
     * Get the name of this prompt.
     * The name is used as the key in the result map.
     *
     * @return the name of this prompt
     */
    String getName();

    /**
     * Get the message to display to the user.
     *
     * @return the message
     */
    String getMessage();

    /**
     * Get the transformer function that modifies how the answer is displayed
     * after submission. This does not change the actual returned value.
     * For example, a password prompt might transform the answer to "***".
     *
     * @return the transformer function, or null for default display
     */
    default Function<String, String> getTransformer() {
        return null;
    }

    /**
     * Get the filter function that modifies the actual returned value.
     * This changes the value stored in the result map.
     * For example, trimming whitespace or converting to lowercase.
     *
     * @return the filter function, or null for no filtering
     */
    default Function<String, String> getFilter() {
        return null;
    }
}
