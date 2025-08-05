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

import org.jline.prompt.InputBuilder;
import org.jline.prompt.PromptBuilder;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;

/**
 * Default implementation of InputBuilder.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultInputBuilder implements InputBuilder {

    private final DefaultPromptBuilder parent;
    private String name;
    private String message;
    private String defaultValue;
    private Character mask;
    private Completer completer;
    private LineReader lineReader;
    private Function<String, Boolean> validator;

    /**
     * Create a new DefaultInputBuilder with the given parent.
     *
     * @param parent the parent builder
     */
    public DefaultInputBuilder(DefaultPromptBuilder parent) {
        this.parent = parent;
    }

    /**
     * Set the name of this prompt.
     *
     * @param name the name
     * @return this builder
     */
    public InputBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the message to display to the user.
     *
     * @param message the message
     * @return this builder
     */
    public InputBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Set the default value.
     *
     * @param defaultValue the default value
     * @return this builder
     */
    public InputBuilder defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Set the mask character for the input.
     *
     * @param mask the mask character, or null to disable masking
     * @return this builder
     */
    public InputBuilder mask(Character mask) {
        this.mask = mask;
        return this;
    }

    /**
     * Set the completer to use.
     *
     * @param completer the completer
     * @return this builder
     */
    public InputBuilder completer(Completer completer) {
        this.completer = completer;
        return this;
    }

    /**
     * Set the line reader to use.
     *
     * @param lineReader the line reader
     * @return this builder
     */
    public InputBuilder lineReader(LineReader lineReader) {
        this.lineReader = lineReader;
        return this;
    }

    /**
     * Set the validator to use.
     *
     * @param validator the validator
     * @return this builder
     */
    public InputBuilder validator(Function<String, Boolean> validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Add this prompt to the parent builder and return to the parent.
     *
     * @return the parent prompt builder
     */
    public PromptBuilder addPrompt() {
        DefaultInputPrompt prompt =
                new DefaultInputPrompt(name, message, defaultValue, mask, completer, lineReader, validator);
        parent.addPrompt(prompt);
        return parent;
    }
}
