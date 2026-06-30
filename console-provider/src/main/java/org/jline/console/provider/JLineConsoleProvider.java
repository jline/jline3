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
 */
public class JLineConsoleProvider implements JdkConsoleProvider {

    /** Creates a new {@code JLineConsoleProvider}. */
    public JLineConsoleProvider() {}

    /**
     * Creates a new {@link JdkConsole} backed by JLine's terminal and line reader.
     *
     * @param isTTY   whether the JVM is attached to a terminal
     * @param charset the charset for console I/O
     * @return a JLine-backed console implementation, or {@code null} if not attached to a TTY
     */
    @Override
    public JdkConsole console(boolean isTTY, Charset charset) {
        if (!isTTY) {
            return null;
        }
        return new JLineJdkConsole(charset);
    }
}
