/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Terminal JNI Provider Module.
 * <p>
 * This module provides terminal functionality using custom JNI (Java Native Interface)
 * implementations. It uses native libraries compiled specifically for JLine to provide
 * low-level terminal access.
 * <p>
 * This provider depends on the native library loading functionality provided by
 * the jline-native module and includes platform-specific native libraries.
 */
module org.jline.terminal.jni {
    // Dependencies
    requires transitive org.jline.terminal;
    requires transitive org.jline.nativ;
    requires java.base;

    // Export public API
    exports org.jline.terminal.impl.jni;

    // Service provider implementation
    provides org.jline.terminal.spi.TerminalProvider with
            org.jline.terminal.impl.jni.JniTerminalProvider;
}
