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

import org.jline.prompt.InputPrompt;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;

/**
 * Default implementation of InputPrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultInputPrompt extends DefaultPrompt implements InputPrompt {

    private final String defaultValue;
    private final Character mask;
    private final Completer completer;
    private final LineReader lineReader;
    private final Function<String, Boolean> validator;

    public DefaultInputPrompt(
            String name,
            String message,
            String defaultValue,
            Character mask,
            Completer completer,
            LineReader lineReader,
            Function<String, Boolean> validator) {
        super(name, message);
        this.defaultValue = defaultValue;
        this.mask = mask;
        this.completer = completer;
        this.lineReader = lineReader;
        this.validator = validator;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Character getMask() {
        return mask;
    }

    @Override
    public Completer getCompleter() {
        return completer;
    }

    @Override
    public LineReader getLineReader() {
        return lineReader;
    }

    @Override
    public Function<String, Boolean> getValidator() {
        return validator;
    }
}
