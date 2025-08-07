/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Console Module.
 * <p>
 * This module provides a high-level console interface that combines the
 * functionality of the terminal, reader, and builtins modules into a
 * unified console experience.
 * <p>
 * The console module is designed to be the main entry point for applications
 * that want to provide a complete interactive command-line interface with
 * built-in commands, syntax highlighting, and advanced terminal features.
 */
module org.jline.console {
    // Core Java platform
    requires java.base;
    requires java.logging;
    requires java.desktop;

    // JLine dependencies
    requires transitive org.jline.terminal;
    requires transitive org.jline.reader;
    requires org.jline.style;
    requires transitive org.jline.builtins;
    requires org.jline.console.ui;

    // Export public API
    exports org.jline.console;
}
