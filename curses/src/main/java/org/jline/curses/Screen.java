/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

public interface Screen {

    void text(int x, int y, AttributedString s);

    void fill(int x, int y, int w, int h, AttributedStyle style);

}
