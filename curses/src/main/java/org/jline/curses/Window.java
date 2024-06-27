/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

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
