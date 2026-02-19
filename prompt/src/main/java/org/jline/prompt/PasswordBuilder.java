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
 * Builder interface for password prompts.
 *
 * <p>
 * PasswordBuilder extends {@link BaseBuilder} to provide a consistent fluent API
 * for creating password prompts. The name() and message() methods are inherited
 * from BaseBuilder, ensuring API consistency across all prompt types.
 * </p>
 *
 * @see BaseBuilder
 * @see PasswordPrompt
 */
public interface PasswordBuilder extends BaseBuilder<PasswordBuilder> {

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
}
