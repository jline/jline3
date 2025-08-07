/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Jansi Core Module.
 * <p>
 * This module provides JLine's own implementation of ANSI escape sequence support,
 * offering an alternative to the external Jansi library. It provides cross-platform
 * ANSI support for terminal applications.
 * <p>
 * This module is designed to be a drop-in replacement for basic Jansi functionality
 * while being fully integrated with the JLine ecosystem.
 */
module org.jline.jansi.core {
    // Dependencies
    requires transitive org.jline.terminal;
    requires java.base;
    requires java.logging;

    // Export public API
    exports org.jline.jansi;
    exports org.jline.jansi.io;
}
