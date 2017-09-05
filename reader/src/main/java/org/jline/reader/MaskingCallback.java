/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

/**
 * Callback used to mask parts of the line
 */
public interface MaskingCallback {

    /**
     * Transforms the line before it is displayed so that
     * some parts can be hidden.
     */
    String display(String line);

    /**
     * Transforms the line before storing in the history.
     */
    String history(String line);

}
