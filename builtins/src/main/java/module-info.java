/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Builtins Module.
 * <p>
 * This module provides built-in commands and utilities for JLine applications.
 * It includes common shell-like commands such as ls, cat, pwd, cd, and others
 * that can be used in interactive command-line applications.
 * <p>
 * The builtins module depends on the reader, terminal, and style modules to
 * provide a complete command-line experience with syntax highlighting,
 * completion, and terminal interaction capabilities.
 */
module org.jline.builtins {
    // Core Java platform
    requires java.base;
    requires java.logging;
    requires java.management;

    // JLine dependencies
    requires transitive org.jline.terminal;
    requires transitive org.jline.reader;
    requires transitive org.jline.style;

    // Optional dependencies
    requires static juniversalchardet;

    // Export public API
    exports org.jline.builtins;
}
