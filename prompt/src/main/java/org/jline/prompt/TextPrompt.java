/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.List;

import org.jline.utils.AttributedString;

/**
 * Interface for text prompts.
 * A text prompt displays text to the user without requiring input.
 */
public interface TextPrompt extends Prompt {

    /**
     * Get the text to display.
     *
     * @return the text
     */
    String getText();

    /**
     * Get the text lines as AttributedString list.
     * This is used for adding to the header like console-ui's Text.getLines().
     *
     * @return the text lines
     */
    List<AttributedString> getLines();
}
