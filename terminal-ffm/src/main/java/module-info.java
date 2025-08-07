/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Terminal FFM Provider Module.
 * <p>
 * This module provides terminal functionality using the Foreign Function Memory (FFM) API
 * introduced in JDK 22. It allows direct access to native system calls without requiring
 * external native libraries like JNA or custom JNI implementations.
 * <p>
 * <strong>Important:</strong> This module requires native access permissions to function
 * properly. When running with this module, you must specify:
 * {@code --enable-native-access=org.jline.terminal.ffm}
 * <p>
 * This module is only available on JDK 22+ where the FFM API is available.
 */
module org.jline.terminal.ffm {
    // Dependencies
    requires transitive org.jline.terminal;
    requires java.base;
    requires java.logging;

    // Export public API
    exports org.jline.terminal.impl.ffm;

    // Service provider implementation
    provides org.jline.terminal.spi.TerminalProvider with
            org.jline.terminal.impl.ffm.FfmTerminalProvider;
}
