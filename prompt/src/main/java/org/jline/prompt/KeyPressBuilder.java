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
 * Builder for key press prompts.
 */
public interface KeyPressBuilder extends BaseBuilder<KeyPressBuilder> {

    /**
     * Set the hint text displayed to the user.
     *
     * @param hint the hint text (default: "Press any key to continue...")
     * @return this builder
     */
    KeyPressBuilder hint(String hint);
}
