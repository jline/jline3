/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Terminal Module.
 * <p>
 * This module provides the core terminal abstraction and functionality.
 * It defines the Terminal interface and basic implementations, along with
 * the service provider interface for terminal providers.
 * <p>
 * Terminal providers are loaded via the standard ServiceLoader mechanism.
 * This module uses terminal provider implementations but does not provide
 * any concrete providers itself - those are provided by separate modules
 * like jline-terminal-jni, etc.
 */
module org.jline.terminal {
    // Dependencies
    requires org.jline.nativ;
    requires java.base;
    requires java.logging;

    // Export public API
    exports org.jline.terminal;
    exports org.jline.terminal.spi;
    exports org.jline.terminal.impl;

    // Export for terminal providers that extend base classes
    exports org.jline.terminal.impl.exec;

    // Export utility classes used by other modules
    exports org.jline.utils;

    // Service provider interface - terminal providers implement this
    uses org.jline.terminal.spi.TerminalProvider;

    // Open packages for reflection access needed by FileDescriptor manipulation
    // and internal terminal operations
    opens org.jline.terminal.impl to
            java.base;
    opens org.jline.terminal.impl.exec to
            java.base;
}
