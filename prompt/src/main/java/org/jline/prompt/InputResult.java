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
 * Result of an input prompt. Contains the input text.
 */
public interface InputResult extends PromptResult<InputPrompt> {

    /**
     * Get the input text.
     *
     * @return the input text
     */
    String getInput();
}
