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

/**
 * Interface for choice prompts.
 * A choice prompt allows the user to select a single item from a list of choices,
 * where each choice has a key associated with it.
 */
public interface ChoicePrompt extends Prompt {

    /**
     * Get the list of choice items.
     *
     * @return the list of choice items
     */
    List<ChoiceItem> getItems();
}
