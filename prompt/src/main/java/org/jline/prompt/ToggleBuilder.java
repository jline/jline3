/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Builder for toggle prompts.
 */
public interface ToggleBuilder extends BaseBuilder<ToggleBuilder> {

    /**
     * Set the label for the active (on) state.
     *
     * @param label the active state label (default: "Yes")
     * @return this builder
     */
    ToggleBuilder activeLabel(String label);

    /**
     * Set the label for the inactive (off) state.
     *
     * @param label the inactive state label (default: "No")
     * @return this builder
     */
    ToggleBuilder inactiveLabel(String label);

    /**
     * Set the default value.
     *
     * @param defaultValue true for active, false for inactive (default: false)
     * @return this builder
     */
    ToggleBuilder defaultValue(boolean defaultValue);
}
