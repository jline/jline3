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
 * Interface for confirmation prompts.
 * A confirmation prompt allows the user to answer yes or no to a question.
 */
public interface ConfirmPrompt extends Prompt {

    /**
     * Get the default value.
     *
     * @return the default value
     */
    boolean getDefaultValue();
}
