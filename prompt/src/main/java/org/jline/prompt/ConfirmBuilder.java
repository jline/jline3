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
 * Builder for confirmation prompts.
 */
public interface ConfirmBuilder extends BaseBuilder<ConfirmBuilder> {

    /**
     * Set the default value.
     *
     * @param defaultValue the default value
     * @return this builder
     */
    ConfirmBuilder defaultValue(boolean defaultValue);
}
