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
 * Result of an expandable choice prompt. Contains the ID of the selected item.
 */
public interface ChoiceResult extends PromptResult<ChoicePrompt> {

    /**
     * Get the ID of the selected item.
     *
     * @return the ID of the selected item
     */
    String getSelectedId();
}
