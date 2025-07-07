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
 * Interface for checkbox prompts.
 * A checkbox prompt allows the user to select multiple items from a list.
 */
public interface CheckboxPrompt extends Prompt {

    /**
     * Get the list of checkbox items.
     *
     * @return the list of checkbox items
     */
    List<CheckboxItem> getItems();
}
