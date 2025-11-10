/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import org.jline.prompt.impl.DefaultNoResult;

/**
 * A result that represents no result.
 */
public interface NoResult extends PromptResult<Prompt> {

    /**
     * Singleton instance of NoResult.
     */
    NoResult INSTANCE = DefaultNoResult.INSTANCE;
}
