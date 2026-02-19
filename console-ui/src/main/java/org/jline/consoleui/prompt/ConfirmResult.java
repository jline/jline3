/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import org.jline.consoleui.elements.ConfirmChoice;

/**
 * Result of a confirmation choice. Holds a single value of 'yes' or 'no'
 * from enum {@link ConfirmChoice.ConfirmationValue}.
 */
public class ConfirmResult implements PromptResultItemIF {
    ConfirmChoice.ConfirmationValue confirmed;

    /**
     * Default constructor.
     *
     * @param confirm the result value to hold.
     */
    public ConfirmResult(ConfirmChoice.ConfirmationValue confirm) {
        this.confirmed = confirm;
    }

    /**
     * Returns the confirmation value.
     * @return confirmation value.
     */
    public ConfirmChoice.ConfirmationValue getConfirmed() {
        return confirmed;
    }

    public String getResult() {
        return confirmed.toString();
    }

    @Override
    public String toString() {
        return "ConfirmResult{" + "confirmed=" + confirmed + '}';
    }
}
