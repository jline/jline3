/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.function.Function;

import org.jline.prompt.Prompt;

/**
 * Base implementation class for all prompt types.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public abstract class DefaultPrompt implements Prompt {

    protected final String name;
    protected final String message;
    private Function<String, String> transformer;
    private Function<String, String> filter;

    protected DefaultPrompt(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Function<String, String> getTransformer() {
        return transformer;
    }

    @Override
    public Function<String, String> getFilter() {
        return filter;
    }

    public void setTransformer(Function<String, String> transformer) {
        this.transformer = transformer;
    }

    public void setFilter(Function<String, String> filter) {
        this.filter = filter;
    }
}
