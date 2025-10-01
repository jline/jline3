/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Curses Module.
 * <p>
 * This module provides curses-like UI components for building text-based user interfaces
 * in terminal applications. It includes windows, panels, buttons, text areas, and other
 * UI components that can be used to create rich interactive console applications.
 * <p>
 * The curses module builds on top of the terminal and reader modules to provide
 * higher-level UI abstractions similar to the traditional curses library, but
 * implemented in pure Java using JLine's terminal capabilities.
 * <p>
 * Key features include:
 * <ul>
 * <li>Window management and layout</li>
 * <li>UI components (buttons, text areas, menus)</li>
 * <li>Border and panel containers</li>
 * <li>Mouse and keyboard event handling</li>
 * <li>Theming and styling support</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The curses GUI implementation handles threading internally to ensure safe operation
 * when signal handlers (such as window resize events) are triggered from different threads.
 * The GUI uses synchronization to coordinate access to the underlying Display instance,
 * preventing concurrent modification exceptions and ensuring consistent terminal output.
 * </p>
 * <p>
 * Applications using the curses module should generally interact with GUI components
 * from a single thread (typically the main thread), while the library handles
 * signal-driven events safely in the background.
 * </p>
 */
module org.jline.curses {
    // Core Java platform
    requires java.base;
    requires java.logging;

    // JLine dependencies
    requires transitive org.jline.terminal;
    requires transitive org.jline.reader;

    // Export public API
    exports org.jline.curses;

    // Open packages for testing
    opens org.jline.curses.impl to
            org.junit.platform.commons;
}
