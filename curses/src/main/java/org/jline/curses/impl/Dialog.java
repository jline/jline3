/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.EnumSet;

import org.jline.curses.*;

/**
 * A modal dialog window.
 *
 * <p>Dialog extends BasicWindow with Modal and Popup behaviors, making it
 * appear centered over the current GUI and capturing all input until closed.</p>
 */
public class Dialog extends BasicWindow {

    public Dialog() {
        setBehaviors(EnumSet.of(Behavior.CloseButton, Behavior.Popup, Behavior.Modal));
    }

    public Dialog(String title, Component content) {
        setBehaviors(EnumSet.of(Behavior.CloseButton, Behavior.Popup, Behavior.Modal));
        setTitle(title);
        setComponent(content);
    }
}
