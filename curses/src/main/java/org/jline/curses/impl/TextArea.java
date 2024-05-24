/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import org.jline.curses.Screen;
import org.jline.curses.Size;

public class TextArea extends AbstractComponent {

    @Override
    protected void doDraw(Screen screen) {
        // TODO
    }

    @Override
    protected Size doGetPreferredSize() {
        return new Size(3, 3);
    }
}
