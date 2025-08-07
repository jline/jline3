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
 * Result of a confirmation prompt. Contains a boolean value indicating whether the user confirmed.
 */
public interface ConfirmResult extends PromptResult<ConfirmPrompt> {

    /**
     * Possible confirmation values.
     */
    enum ConfirmationValue {
        YES,
        NO
    }

    /**
     * Get the confirmation value.
     *
     * @return the confirmation value
     */
    ConfirmationValue getConfirmed();

    /**
     * Check if the user confirmed (answered YES).
     *
     * @return true if the user confirmed, false otherwise
     */
    boolean isConfirmed();
}
