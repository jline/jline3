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
 * Builder interface for checkbox separators.
 */
public interface CheckboxSeparatorBuilder {

    /**
     * Set the separator text.
     *
     * @param text the separator text
     * @return this builder
     */
    CheckboxSeparatorBuilder text(String text);

    /**
     * Add the separator to the checkbox prompt and return to the checkbox builder.
     *
     * @return the checkbox builder
     */
    CheckboxBuilder add();
}
