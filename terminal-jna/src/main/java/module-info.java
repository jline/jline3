/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Terminal JNA Provider Module.
 * <p>
 * This module provides terminal functionality using the Java Native Access (JNA) library.
 * JNA allows Java programs to call native libraries without writing JNI code.
 * <p>
 * This provider requires the JNA library to be available on the classpath.
 * The JNA dependency is marked as static, making it optional at runtime.
 * If JNA is not available, this provider will not be loaded.
 */
@SuppressWarnings("requires-automatic")
module org.jline.terminal.jna {
    // Dependencies
    requires transitive org.jline.terminal;
    requires java.base;
    requires static com.sun.jna; // Optional dependency

    // Export public API
    exports org.jline.terminal.impl.jna;

    // JNA needs access to platform-specific classes for reflection
    exports org.jline.terminal.impl.jna.freebsd to
            com.sun.jna;
    exports org.jline.terminal.impl.jna.linux to
            com.sun.jna;
    exports org.jline.terminal.impl.jna.osx to
            com.sun.jna;
    exports org.jline.terminal.impl.jna.solaris to
            com.sun.jna;
    exports org.jline.terminal.impl.jna.win to
            com.sun.jna;

    // Service provider implementation
    provides org.jline.terminal.spi.TerminalProvider with
            org.jline.terminal.impl.jna.JnaTerminalProvider;
}
