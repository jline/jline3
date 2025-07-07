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
 * Builder for text displays.
 */
public interface TextBuilder extends BaseBuilder<TextBuilder> {

    /**
     * Set the text to display.
     *
     * @param text the text
     * @return this builder
     */
    TextBuilder text(String text);
}
