/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import org.jline.terminal.KeyEvent;

/**
 * Represents an input event that can be either a KeyEvent or a Mouse event indicator.
 * This is used in the KeyMap to distinguish between keyboard and mouse input.
 */
public class InputEvent {

    /**
     * Special singleton instance to indicate mouse events.
     */
    public static final InputEvent MOUSE = new InputEvent();

    private final KeyEvent keyEvent;
    private final boolean isMouse;

    // Private constructor for mouse indicator
    private InputEvent() {
        this.keyEvent = null;
        this.isMouse = true;
    }

    /**
     * Creates an InputEvent for a KeyEvent.
     */
    public InputEvent(KeyEvent keyEvent) {
        this.keyEvent = keyEvent;
        this.isMouse = false;
    }

    /**
     * Returns true if this is a mouse event indicator.
     */
    public boolean isMouse() {
        return isMouse;
    }

    /**
     * Returns true if this is a key event.
     */
    public boolean isKey() {
        return !isMouse;
    }

    /**
     * Returns the KeyEvent if this is a key event, null otherwise.
     */
    public KeyEvent getKeyEvent() {
        return keyEvent;
    }

    @Override
    public String toString() {
        if (isMouse) {
            return "InputEvent[MOUSE]";
        } else {
            return "InputEvent[" + keyEvent + "]";
        }
    }
}
