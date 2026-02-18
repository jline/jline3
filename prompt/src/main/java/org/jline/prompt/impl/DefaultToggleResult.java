/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.TogglePrompt;
import org.jline.prompt.ToggleResult;

/**
 * Implementation of ToggleResult.
 */
public class DefaultToggleResult extends AbstractPromptResult<TogglePrompt> implements ToggleResult {

    private final boolean active;

    public DefaultToggleResult(boolean active, TogglePrompt prompt) {
        super(prompt);
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    protected String getRawResult() {
        return String.valueOf(active);
    }

    @Override
    public String getDisplayResult() {
        if (getPrompt() == null) {
            return String.valueOf(active);
        }
        return active ? getPrompt().getActiveLabel() : getPrompt().getInactiveLabel();
    }

    @Override
    public String toString() {
        return "ToggleResult{active=" + active + "}";
    }
}
