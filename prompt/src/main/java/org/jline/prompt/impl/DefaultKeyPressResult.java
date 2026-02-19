/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.KeyPressPrompt;
import org.jline.prompt.KeyPressResult;

/**
 * Implementation of KeyPressResult.
 */
public class DefaultKeyPressResult extends AbstractPromptResult<KeyPressPrompt> implements KeyPressResult {

    private final String key;

    public DefaultKeyPressResult(String key, KeyPressPrompt prompt) {
        super(prompt);
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    protected String getRawResult() {
        return key;
    }

    @Override
    public String toString() {
        return "KeyPressResult{key='" + key + "'}";
    }
}
