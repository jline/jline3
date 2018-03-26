/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses;

import org.jline.utils.AttributedStyle;

public interface Theme {

    AttributedStyle getStyle(String spec);

    void box(Screen screen, int x, int y, int w, int h, Curses.Border border, String style);

    void separatorH(Screen screen, int x, int y, int w, Curses.Border sepBorder, Curses.Border boxBorder, AttributedStyle style);
}
