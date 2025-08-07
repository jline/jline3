/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Reader Module.
 * <p>
 * This module provides line reading functionality with features like
 * command completion, history, and line editing. It builds on top of
 * the terminal module to provide a rich interactive command-line
 * reading experience.
 * <p>
 * The reader module includes support for:
 * - Line editing with cursor movement
 * - Command history
 * - Tab completion
 * - Syntax highlighting
 * - Custom key bindings
 */
module org.jline.reader {
    // Dependencies
    requires transitive org.jline.terminal;
    requires java.base;
    requires java.logging;

    // Export public API
    exports org.jline.reader;
    exports org.jline.reader.impl;
    exports org.jline.reader.impl.completer;

    // Export keymap package for builtins
    exports org.jline.keymap;
    exports org.jline.reader.impl.history;

    // Open packages for reflection access needed by java.io internals
    // This is required for FileDescriptor and stream manipulation
    opens org.jline.reader.impl to
            java.base;
}
