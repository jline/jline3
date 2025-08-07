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
 * Builder interface for password prompts.
 */
public interface PasswordBuilder extends PromptBuilder {

    /**
     * Set the name of the prompt.
     *
     * @param name the name
     * @return this builder
     */
    PasswordBuilder name(String name);

    /**
     * Set the message to display.
     *
     * @param message the message
     * @return this builder
     */
    PasswordBuilder message(String message);

    /**
     * Set the default value.
     *
     * @param defaultValue the default value
     * @return this builder
     */
    PasswordBuilder defaultValue(String defaultValue);

    /**
     * Set the mask character.
     *
     * @param mask the mask character
     * @return this builder
     */
    PasswordBuilder mask(Character mask);

    /**
     * Set whether to show the mask character or hide input completely.
     *
     * @param showMask true to show mask characters, false to hide completely
     * @return this builder
     */
    PasswordBuilder showMask(boolean showMask);

    /**
     * Add the password prompt to the builder.
     *
     * @return the parent builder
     */
    PromptBuilder addPrompt();
}
