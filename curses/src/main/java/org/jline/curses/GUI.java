/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import org.jline.terminal.Terminal;

public interface GUI {

    <C extends Component> Renderer getRenderer(Class<C> componentClass);

    <C extends Component> void setRenderer(Class<C> componentClass, Renderer renderer);

    Theme getTheme();

    void setTheme(Theme theme);

    void addWindow(Window window);

    void removeWindow(Window window);

    void run();

    Terminal getTerminal();
}
