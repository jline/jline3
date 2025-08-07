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
 * Interface for all promptable elements in the Prompter.
 */
public interface Prompt {

    /**
     * Get the name of this prompt.
     * The name is used as the key in the result map.
     *
     * @return the name of this prompt
     */
    String getName();

    /**
     * Get the message to display to the user.
     *
     * @return the message
     */
    String getMessage();
}
