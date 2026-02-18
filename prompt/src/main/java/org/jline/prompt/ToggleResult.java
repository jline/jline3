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
 * Result of a toggle prompt. Contains a boolean indicating the selected state.
 */
public interface ToggleResult extends PromptResult<TogglePrompt> {

    /**
     * Get the selected value.
     *
     * @return true if active state was selected, false for inactive
     */
    boolean isActive();
}
