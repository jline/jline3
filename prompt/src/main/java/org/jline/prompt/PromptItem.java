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
 * Interface for all UI items in the Prompter.
 * Items are immutable value objects that represent the configuration of prompt elements.
 */
public interface PromptItem {

    /**
     * Whether this item can be selected.
     *
     * @return true if this item can be selected
     */
    boolean isSelectable();

    /**
     * Get the name of this item.
     *
     * @return the name
     */
    String getName();

    /**
     * Whether this item is disabled.
     *
     * @return true if this item is disabled
     */
    default boolean isDisabled() {
        return false;
    }

    /**
     * Get the text to display for this item.
     *
     * @return the text
     */
    String getText();

    /**
     * Get the text to display when this item is disabled.
     *
     * @return the disabled text
     */
    default String getDisabledText() {
        return "";
    }
}
