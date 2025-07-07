/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.NoResult;
import org.jline.prompt.Prompt;

/**
 * Implementation of NoResult.
 */
public class DefaultNoResult extends AbstractPromptResult<Prompt> implements NoResult {

    /**
     * Singleton instance of DefaultNoResult.
     */
    public static final DefaultNoResult INSTANCE = new DefaultNoResult();

    private DefaultNoResult() {
        super(null);
    }

    @Override
    public String getResult() {
        return "NO_RESULT";
    }

    @Override
    public String getDisplayResult() {
        return "NO_RESULT";
    }

    @Override
    public String toString() {
        return "NoResult{}";
    }
}
