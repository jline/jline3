/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Console UI Module.
 * <p>
 * This module provides user interface components for building console applications.
 * It includes widgets, layouts, and other UI elements that can be used to create
 * rich text-based user interfaces in terminal applications.
 * <p>
 * The console-ui module builds on top of the terminal and style modules to provide
 * higher-level UI abstractions for creating interactive console applications.
 */
module org.jline.console.ui {
    // Core Java platform
    requires java.base;
    requires java.logging;

    // JLine dependencies
    requires transitive org.jline.terminal;
    requires transitive org.jline.style;
    requires transitive org.jline.reader;
    requires transitive org.jline.builtins;

    // Export public API
    exports org.jline.consoleui.elements;
    exports org.jline.consoleui.elements.items;
    exports org.jline.consoleui.elements.items.impl;
    exports org.jline.consoleui.prompt;
    exports org.jline.consoleui.prompt.builder;
}
