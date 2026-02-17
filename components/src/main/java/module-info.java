/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
module org.jline.components {
    requires transitive org.jline.terminal;

    exports org.jline.components;
    exports org.jline.components.layout;
    exports org.jline.components.ui;
    exports org.jline.components.animation;
}
