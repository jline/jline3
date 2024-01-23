/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements;

/**
 * User: Andreas Wegmann
 * Date: 07.01.16
 */
public class ConfirmChoice extends AbstractPromptableElement {

    public enum ConfirmationValue {
        YES,
        NO
    }

    private ConfirmationValue defaultConfirmation = null;

    public ConfirmChoice(String message, String name) {
        super(message, name);
    }

    public ConfirmChoice(String message, String name, ConfirmationValue defaultConfirmation) {
        super(message, name);
        this.defaultConfirmation = defaultConfirmation;
    }

    public ConfirmationValue getDefaultConfirmation() {
        return defaultConfirmation;
    }
}
