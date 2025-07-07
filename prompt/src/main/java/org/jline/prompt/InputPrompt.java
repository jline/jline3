/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.function.Function;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;

/**
 * Interface for input prompts.
 * An input prompt allows the user to enter text.
 */
public interface InputPrompt extends Prompt {

    /**
     * Get the default value.
     *
     * @return the default value
     */
    String getDefaultValue();

    /**
     * Get the mask character for the input.
     *
     * @return the mask character, or null if masking is disabled
     */
    Character getMask();

    /**
     * Get the completer.
     *
     * @return the completer
     */
    Completer getCompleter();

    /**
     * Get the line reader.
     *
     * @return the line reader
     */
    LineReader getLineReader();

    /**
     * Get the validator.
     *
     * @return the validator
     */
    Function<String, Boolean> getValidator();
}
