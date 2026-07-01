/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.provider;

import java.nio.charset.Charset;

import jdk.internal.io.JdkConsole;
import jdk.internal.io.JdkConsoleProvider;

/**
 * A {@link JdkConsoleProvider} implementation that uses JLine for line editing,
 * history, and completion when applications use {@link java.io.Console}.
 * <p>
 * This provider is discovered via {@link java.util.ServiceLoader} when the module
 * is on the module path and activated with {@code -Djdk.console=org.jline.console.provider}.
 * <p>
 * When active, {@code System.console().readLine()} and related methods will use
 * JLine's {@link org.jline.reader.LineReader} instead of the JDK's built-in
 * console implementation, providing interactive line editing, history navigation,
 * and other terminal features.
 * <p>
 * This provider supports both the JDK 22-24 API ({@code console(boolean, Charset)})
 * and the JDK 25+ API ({@code console(boolean, Charset, Charset)}).
 */
public class JLineConsoleProvider implements JdkConsoleProvider {

    /** Creates a new {@code JLineConsoleProvider}. Required for {@link java.util.ServiceLoader} discovery. */
    public JLineConsoleProvider() {
        // Required public no-arg constructor for ServiceLoader instantiation
    }

    /**
     * Creates a new {@link JdkConsole} backed by JLine's terminal and line reader.
     * <p>
     * This signature matches the JDK 22-24 {@link JdkConsoleProvider} API.
     *
     * @param isTTY   whether the JVM is attached to a terminal
     * @param charset the charset for console I/O
     * @return a JLine-backed console implementation, or {@code null} if not attached to a TTY
     */
    public JdkConsole console(boolean isTTY, Charset charset) {
        return console(isTTY, charset, charset);
    }

    /**
     * Creates a new {@link JdkConsole} backed by JLine's terminal and line reader.
     * <p>
     * This signature matches the JDK 25+ {@link JdkConsoleProvider} API.
     *
     * @param isTTY      whether the JVM is attached to a terminal
     * @param inCharset  the charset for console input
     * @param outCharset the charset for console output
     * @return a JLine-backed console implementation, or {@code null} if not attached to a TTY
     */
    public JdkConsole console(boolean isTTY, Charset inCharset, Charset outCharset) {
        if (!isTTY) {
            return null;
        }
        return new JLineJdkConsole(outCharset);
    }
}
