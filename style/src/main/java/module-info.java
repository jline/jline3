/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Style Module.
 * <p>
 * This module provides styling support for JLine, including color management,
 * style resolution, and style parsing functionality. It handles the conversion
 * between different color formats and provides utilities for styling terminal
 * output.
 * <p>
 * The style module is used by higher-level modules like builtins and console
 * to provide rich text formatting capabilities.
 */
module org.jline.style {
    // Core Java platform
    requires java.base;
    requires java.logging;

    // JLine dependencies
    requires transitive org.jline.terminal;

    // Export public API
    exports org.jline.style;
}
