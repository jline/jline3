/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses.impl;

import org.jline.curses.Component;
import org.jline.curses.Renderer;
import org.jline.curses.Screen;
import org.jline.curses.Size;

public class Button extends AbstractComponent {

    @Override
    protected void doDraw(Screen screen) {

    }

    @Override
    protected Size doGetPreferredSize() {
        return null;
    }
}
