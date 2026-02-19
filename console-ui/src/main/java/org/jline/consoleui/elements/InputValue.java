/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements;

import org.jline.reader.Completer;

public class InputValue extends AbstractPromptableElement {
    private String value;
    private final String defaultValue;
    private Character mask;
    private Completer completer;

    public InputValue(String name, String message) {
        super(message, name);
        this.value = null;
        this.defaultValue = null;
    }

    public InputValue(String name, String message, String value, String defaultValue) {
        super(message, name);
        // this.value = value;
        if (value != null)
            throw new IllegalStateException("pre filled values for InputValue are not supported at the moment.");
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setMask(Character mask) {
        this.mask = mask;
    }

    public Character getMask() {
        return mask;
    }

    public void setCompleter(Completer completer) {
        this.completer = completer;
    }

    public Completer getCompleter() {
        return completer;
    }
}
