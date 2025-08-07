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
 * Interface for confirmation items in the Prompter.
 * Confirmation items are immutable - their default value is set during creation.
 */
public interface ConfirmItem extends PromptItem {

    /**
     * Get the default confirmation value.
     *
     * @return the default confirmation value
     */
    default boolean getDefaultValue() {
        return false;
    }
}
