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
 * Interface for list prompts.
 * A list prompt allows the user to select a single item from a list.
 */
public interface ListPrompt extends Prompt {

    /**
     * Get the list of list items.
     *
     * @return the list of list items
     */
    List<ListItem> getItems();
}
