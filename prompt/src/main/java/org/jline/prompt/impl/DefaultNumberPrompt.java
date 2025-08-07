/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.NumberPrompt;

/**
 * Default implementation of NumberPrompt interface.
 */
public class DefaultNumberPrompt extends DefaultInputPrompt implements NumberPrompt {

    private final Double min;
    private final Double max;
    private final boolean allowDecimals;
    private final String invalidNumberMessage;
    private final String outOfRangeMessage;

    public DefaultNumberPrompt(String name, String message) {
        this(name, message, null, null, true);
    }

    public DefaultNumberPrompt(String name, String message, Double min, Double max) {
        this(name, message, min, max, true);
    }

    public DefaultNumberPrompt(String name, String message, Double min, Double max, boolean allowDecimals) {
        this(name, message, min, max, allowDecimals, null, null, null);
    }

    public DefaultNumberPrompt(
            String name,
            String message,
            Double min,
            Double max,
            boolean allowDecimals,
            String defaultValue,
            String invalidNumberMessage,
            String outOfRangeMessage) {
        super(name, message, defaultValue, null, null, null, null);
        this.min = min;
        this.max = max;
        this.allowDecimals = allowDecimals;
        this.invalidNumberMessage = invalidNumberMessage;
        this.outOfRangeMessage = outOfRangeMessage;
    }

    @Override
    public Double getMin() {
        return min;
    }

    @Override
    public Double getMax() {
        return max;
    }

    @Override
    public boolean allowDecimals() {
        return allowDecimals;
    }

    @Override
    public String getInvalidNumberMessage() {
        return invalidNumberMessage != null ? invalidNumberMessage : NumberPrompt.super.getInvalidNumberMessage();
    }

    @Override
    public String getOutOfRangeMessage() {
        return outOfRangeMessage != null ? outOfRangeMessage : NumberPrompt.super.getOutOfRangeMessage();
    }
}
