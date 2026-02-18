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
 * Interface for toggle prompts.
 * A toggle prompt presents a binary on/off switch with labeled states
 * (e.g., "Yes / No", "Enable / Disable"). The user toggles with arrow keys or Tab.
 */
public interface TogglePrompt extends Prompt {

    /**
     * Get the label for the active (on) state.
     *
     * @return the active state label
     */
    String getActiveLabel();

    /**
     * Get the label for the inactive (off) state.
     *
     * @return the inactive state label
     */
    String getInactiveLabel();

    /**
     * Get the default value.
     *
     * @return true if the default is the active state, false for inactive
     */
    boolean getDefaultValue();
}
