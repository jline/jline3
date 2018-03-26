/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.curses;

import org.jline.terminal.MouseEvent;

import java.util.Collection;
import java.util.Collections;

public interface Window extends Container {

    String getTitle();

    void setTitle(String title);

    Component getComponent();

    void setComponent(Component component);

    default Collection<Component> getComponents() {
        return Collections.singleton(getComponent());
    }

    void focus(Component component);

    GUI getGUI();

    void close();

}
