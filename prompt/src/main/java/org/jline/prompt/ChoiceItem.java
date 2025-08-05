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
 * Interface for choice items in the Prompter.
 */
public interface ChoiceItem extends PromptItem {

    /**
     * Whether this choice is the default choice.
     *
     * @return true if this choice is the default
     */
    default boolean isDefaultChoice() {
        return false;
    }

    /**
     * Get the key associated with this choice.
     *
     * @return the key
     */
    default Character getKey() {
        return ' ';
    }
}
