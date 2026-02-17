/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.ui;

import org.jline.components.Component;

/**
 * Base class for components providing dirty-tracking.
 */
public abstract class AbstractComponent implements Component {

    private boolean dirty = true;

    protected AbstractComponent() {}

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void invalidate() {
        dirty = true;
    }

    protected void markClean() {
        dirty = false;
    }
}
