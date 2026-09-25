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
     * Whether to show the mask character in the post-input display result.
     * When {@code true}, the display result shows mask characters for each typed character.
     * When {@code false}, the display result is empty (no visible feedback after input).
     * <p>
     * Note: this controls only the post-input display value returned by
     * {@link InputResult#getDisplayResult()}, not what is shown during typing.
     * To suppress all visible feedback while typing, use {@code mask('\0')}.
     *
     * @return true to show mask characters in the result, false to hide completely
     */
    default boolean showMask() {
        return true;
    }
}
