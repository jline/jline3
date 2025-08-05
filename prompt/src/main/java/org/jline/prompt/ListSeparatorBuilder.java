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
 * Builder interface for list separators.
 */
public interface ListSeparatorBuilder {

    /**
     * Set the separator text.
     *
     * @param text the separator text
     * @return this builder
     */
    ListSeparatorBuilder text(String text);

    /**
     * Add the separator to the list prompt and return to the list builder.
     *
     * @return the list builder
     */
    ListBuilder add();
}
