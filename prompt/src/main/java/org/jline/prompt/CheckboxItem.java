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
 * Interface for checkbox items in the Prompter.
 * Checkbox items are immutable - their checked state is managed by the builder.
 */
public interface CheckboxItem extends PromptItem {

    /**
     * Whether this checkbox is initially checked.
     * This represents the default/initial state, not the current selection.
     *
     * @return true if this checkbox is initially checked
     */
    default boolean isInitiallyChecked() {
        return false;
    }
}
