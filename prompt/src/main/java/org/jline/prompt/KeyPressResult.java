/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Result of a key press prompt. Contains the key that was pressed.
 */
public interface KeyPressResult extends PromptResult<KeyPressPrompt> {

    /**
     * Get the key that was pressed.
     *
     * @return the pressed key as a string
     */
    String getKey();
}
