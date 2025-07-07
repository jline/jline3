/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.PrompterConfig;
import org.jline.utils.OSUtils;

/**
 * Default implementation of PrompterConfig interface.
 */
public class DefaultPrompterConfig implements PrompterConfig {

    private final String indicator;
    private final String uncheckedBox;
    private final String checkedBox;
    private final String unavailable;
    private final boolean cancellableFirstPrompt;

    /**
     * Create a default configuration with sensible defaults.
     */
    public DefaultPrompterConfig() {
        this(
                "?",
                OSUtils.IS_WINDOWS ? "o" : "◯",
                OSUtils.IS_WINDOWS ? "x" : "◉",
                OSUtils.IS_WINDOWS ? "-" : "⊝",
                false);
    }

    /**
     * Create a configuration with specific values.
     */
    public DefaultPrompterConfig(
            String indicator,
            String uncheckedBox,
            String checkedBox,
            String unavailable,
            boolean cancellableFirstPrompt) {
        this.indicator = indicator;
        this.uncheckedBox = uncheckedBox;
        this.checkedBox = checkedBox;
        this.unavailable = unavailable;
        this.cancellableFirstPrompt = cancellableFirstPrompt;
    }

    @Override
    public String indicator() {
        return indicator;
    }

    @Override
    public String uncheckedBox() {
        return uncheckedBox;
    }

    @Override
    public String checkedBox() {
        return checkedBox;
    }

    @Override
    public String unavailable() {
        return unavailable;
    }

    @Override
    public boolean cancellableFirstPrompt() {
        return cancellableFirstPrompt;
    }
}
