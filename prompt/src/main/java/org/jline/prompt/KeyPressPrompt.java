/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Interface for key press prompts.
 * A key press prompt displays a message and waits for the user to press any key to continue.
 */
public interface KeyPressPrompt extends Prompt {

    /**
     * Get the hint text displayed to the user (e.g., "Press any key to continue...").
     *
     * @return the hint text
     */
    default String getHint() {
        return "Press any key to continue...";
    }
}
