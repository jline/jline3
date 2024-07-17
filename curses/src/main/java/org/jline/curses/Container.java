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

public interface Container extends Component {

    /**
     * Returns a read-only collection of all contained components.
     */
    Collection<Component> getComponents();
}
