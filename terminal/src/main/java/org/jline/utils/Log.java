/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Supplier;

/**
 * Internal logging utility for JLine components.
 *
 * <p>
 * The Log class provides a simple logging facility for JLine components, using
 * the {@link System.Logger} API under the hood. It offers methods for logging
 * at various levels (trace, debug, info, warn, error) and supports both direct
 * message logging and lazy evaluation through suppliers.
 * </p>
 *
 * <p>
 * Using {@link System.Logger} keeps JLine free of any compile-time dependency on
 * the {@code java.logging} module, which is desirable for consumers building
 * trimmed runtime images with {@code jlink}. When the {@code java.logging} module
 * is present at runtime, the default {@code System.Logger} implementation routes
 * to {@code java.util.logging}.
 * </p>
 *
 * <p>
 * This class uses a single logger named "org.jline" for all JLine components.
 * The actual log level can be configured through whichever logging backend the
 * running JVM provides a {@link System.LoggerFinder} for.
 * </p>
 *
 * <p>
 * Key features include:
 * </p>
 * <ul>
 *   <li>Simple static methods for different log levels</li>
 *   <li>Support for multiple message objects that are concatenated</li>
 *   <li>Lazy evaluation of log messages using Supplier interfaces</li>
 *   <li>Automatic exception handling with stack trace logging</li>
 *   <li>Performance optimization to avoid string concatenation when logging is disabled</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Simple logging
 * Log.debug("Processing command: ", command);
 *
 * // Logging with lazy evaluation
 * Log.trace(() -> "Expensive computation result: " + computeResult());
 *
 * // Logging exceptions
 * try {
 *     // Some operation
 * } catch (Exception e) {
 *     Log.error("Failed to process: ", e);
 * }
 * </pre>
 *
 * @since 2.0
 */
public final class Log {

    /**
     * Private constructor to prevent instantiation.
     */
    private Log() {
        // Utility class
    }

    private static final Logger logger = System.getLogger("org.jline");

    public static void trace(final Object... messages) {
        log(Level.TRACE, messages);
    }

    public static void trace(Supplier<String> supplier) {
        log(Level.TRACE, supplier);
    }

    public static void debug(Supplier<String> supplier) {
        log(Level.DEBUG, supplier);
    }

    public static void debug(final Object... messages) {
        log(Level.DEBUG, messages);
    }

    public static void info(final Object... messages) {
        log(Level.INFO, messages);
    }

    public static void warn(final Object... messages) {
        log(Level.WARNING, messages);
    }

    public static void error(final Object... messages) {
        log(Level.ERROR, messages);
    }

    public static boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    /**
     * Helper to support rendering messages.
     */
    static void render(final PrintStream out, final Object message) {
        if (message != null && message.getClass().isArray()) {
            Object[] array = (Object[]) message;

            out.print("[");
            for (int i = 0; i < array.length; i++) {
                out.print(array[i]);
                if (i + 1 < array.length) {
                    out.print(",");
                }
            }
            out.print("]");
        } else {
            out.print(message);
        }
    }

    static void log(final Level level, final Supplier<String> message) {
        logger.log(level, message);
    }

    static void log(final Level level, final Object... messages) {
        if (!logger.isLoggable(level)) {
            return;
        }
        Throwable cause = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        for (int i = 0; i < messages.length; i++) {
            // Special handling for the last message if it's a throwable, render its stack on the next line
            if (i + 1 == messages.length && messages[i] instanceof Throwable) {
                cause = (Throwable) messages[i];
            } else {
                render(ps, messages[i]);
            }
        }
        ps.close();
        String message = baos.toString();
        if (cause != null) {
            logger.log(level, message, cause);
        } else {
            logger.log(level, message);
        }
    }

    static boolean isEnabled(Level level) {
        return logger.isLoggable(level);
    }
}
