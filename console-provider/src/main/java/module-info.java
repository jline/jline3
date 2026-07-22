/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine JDK Console Provider Module.
 * <p>
 * This module provides a {@link jdk.internal.io.JdkConsoleProvider} implementation
 * that uses JLine's {@link org.jline.reader.LineReader} as the backing implementation
 * for {@link java.io.Console}. When this module is present on the module path and
 * selected via {@code -Djdk.console=org.jline.console.provider}, applications using
 * {@code System.console()} will automatically get JLine's line editing, history, and
 * completion capabilities.
 * <p>
 * <strong>Important:</strong> This module requires JDK 22+ and the following JVM flags
 * at both compile time and runtime:
 * {@code --add-exports java.base/jdk.internal.io=org.jline.console.provider}
 * <p>
 * To activate this provider, add the module to the module path and set:
 * {@code -Djdk.console=org.jline.console.provider}
 */
module org.jline.console.provider {
    requires java.base;
    requires org.jline.terminal;
    requires org.jline.reader;

    exports org.jline.console.provider;

    provides jdk.internal.io.JdkConsoleProvider with
            org.jline.console.provider.JLineConsoleProvider;
}
