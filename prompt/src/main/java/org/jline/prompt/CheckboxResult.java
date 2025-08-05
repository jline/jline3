/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.Set;

/**
 * Result of a checkbox choice. Contains a set with the IDs of the selected checkbox items.
 */
public interface CheckboxResult extends PromptResult<CheckboxPrompt> {

    /**
     * Get the IDs of the selected items.
     *
     * @return the IDs of the selected items
     */
    Set<String> getSelectedIds();
}
