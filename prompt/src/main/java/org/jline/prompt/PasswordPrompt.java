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
 * Interface for password prompts.
 * A password prompt is like an input prompt but masks the input characters.
 */
public interface PasswordPrompt extends InputPrompt {

    /**
     * Get the mask character to use for hiding input.
     * If null, uses the default mask character '*'.
     *
     * @return the mask character, or null for default
     */
    @Override
    Character getMask();

    /**
     * Whether to show the mask character or completely hide input.
     * If true, shows mask characters. If false, shows nothing.
     *
     * @return true to show mask characters, false to hide completely
     */
    default boolean showMask() {
        return true;
    }
}
