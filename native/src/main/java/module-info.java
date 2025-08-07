/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Native Library Module.
 * <p>
 * This module provides native library loading functionality for JLine.
 * It handles the discovery, extraction, and loading of platform-specific
 * native libraries required for terminal operations.
 * <p>
 * The module exports the native API and handles native library loading
 * across different platforms (Windows, Linux, macOS, etc.).
 */
module org.jline.nativ {
    // Core Java platform
    requires java.base;
    requires java.logging;

    // Export public API
    exports org.jline.nativ;
}
