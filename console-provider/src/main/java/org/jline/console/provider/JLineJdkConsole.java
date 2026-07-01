/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.provider;

import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Locale;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import jdk.internal.io.JdkConsole;

/**
 * JLine-backed implementation of {@link JdkConsole}.
 * <p>
 * This class wraps JLine's {@link Terminal} and {@link LineReader} behind the
 * {@link JdkConsole} interface, providing line editing, history, and other
 * terminal features to applications that use {@link java.io.Console}.
 * <p>
 * The terminal and line reader are initialized lazily on first use to avoid
 * unnecessary resource allocation if the console is never actually used.
 * <p>
 * This implementation provides methods for both the JDK 22-24 API (without
 * {@link Locale} parameters) and the JDK 25+ API (with {@link Locale} parameters).
 * The JVM dispatches to the matching method signature at runtime, so the same
 * class works across JDK versions without recompilation.
 */
class JLineJdkConsole implements JdkConsole {

    private final Charset charset;
    private final Object lock = new Object();
    private volatile Terminal terminal;
    private volatile LineReader reader;
    private volatile PrintWriter writer;

    JLineJdkConsole(Charset charset) {
        this.charset = charset;
    }

    private void ensureInitialized() {
        if (terminal == null) {
            synchronized (lock) {
                if (terminal == null) {
                    try {
                        terminal = TerminalBuilder.builder()
                                .name("JLine JDK Console")
                                .system(true)
                                .encoding(charset)
                                .build();
                        reader = LineReaderBuilder.builder()
                                .terminal(terminal)
                                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                                .build();
                    } catch (IOException e) {
                        throw new IOError(e);
                    }
                }
            }
        }
    }

    // --- Common methods (same signature in all JDK versions) ---

    @Override
    public PrintWriter writer() {
        ensureInitialized();
        if (writer == null) {
            synchronized (lock) {
                if (writer == null) {
                    writer = terminal.writer();
                }
            }
        }
        return writer;
    }

    @Override
    public Reader reader() {
        ensureInitialized();
        return terminal.reader();
    }

    @Override
    public String readLine() {
        return readLine((String) null);
    }

    @Override
    public char[] readPassword() {
        return readPassword((String) null);
    }

    @Override
    public void flush() {
        ensureInitialized();
        terminal.flush();
    }

    @Override
    public Charset charset() {
        return charset;
    }

    // --- JDK 22-24 API methods (without Locale) ---

    /**
     * Formats and writes a string to the console output.
     * <p>
     * JDK 22-24 API.
     */
    public JdkConsole format(String format, Object... args) {
        writer().format(format, args);
        writer().flush();
        return this;
    }

    /**
     * Formats and writes a string to the console output.
     * Equivalent to {@link #format(String, Object...)}.
     * <p>
     * JDK 22-24 API.
     */
    public JdkConsole printf(String format, Object... args) {
        return format(format, args);
    }

    /**
     * Reads a single line of text from the console with an optional formatted prompt.
     * <p>
     * JDK 22-24 API.
     */
    public String readLine(String format, Object... args) {
        ensureInitialized();
        try {
            String prompt = formatPrompt(format, args);
            return reader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    /**
     * Reads a password from the console with input masking and an optional formatted prompt.
     * <p>
     * JDK 22-24 API.
     *
     * @return the password as a character array, or {@code null} if end of stream was reached
     *         (matching the {@link java.io.Console#readPassword()} contract)
     */
    @SuppressWarnings("java:S1168") // null return is required by the Console API contract (null = EOF)
    public char[] readPassword(String format, Object... args) {
        ensureInitialized();
        try {
            String prompt = formatPrompt(format, args);
            String password = reader.readLine(prompt, '\0');
            return password != null ? password.toCharArray() : null;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    // --- JDK 25+ API methods (with Locale, and println/print) ---

    /**
     * Writes an object followed by a newline to the console output.
     * <p>
     * JDK 25+ API.
     */
    public JdkConsole println(Object obj) {
        writer().println(obj);
        writer().flush();
        return this;
    }

    /**
     * Writes an object to the console output.
     * <p>
     * JDK 25+ API.
     */
    public JdkConsole print(Object obj) {
        writer().print(obj);
        writer().flush();
        return this;
    }

    /**
     * Formats and writes a string to the console output using the specified locale.
     * <p>
     * JDK 25+ API.
     */
    public JdkConsole format(Locale locale, String format, Object... args) {
        writer().format(locale, format, args);
        writer().flush();
        return this;
    }

    /**
     * Reads a single line of text from the console with an optional locale-formatted prompt.
     * <p>
     * JDK 25+ API.
     */
    public String readLine(Locale locale, String format, Object... args) {
        ensureInitialized();
        try {
            String prompt = formatPrompt(locale, format, args);
            return reader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    /**
     * Reads a password from the console with an optional locale-formatted prompt.
     * <p>
     * JDK 25+ API.
     *
     * @return the password as a character array, or {@code null} if end of stream was reached
     *         (matching the {@link java.io.Console#readPassword()} contract)
     */
    @SuppressWarnings("java:S1168") // null return is required by the Console API contract (null = EOF)
    public char[] readPassword(Locale locale, String format, Object... args) {
        ensureInitialized();
        try {
            String prompt = formatPrompt(locale, format, args);
            String password = reader.readLine(prompt, '\0');
            return password != null ? password.toCharArray() : null;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    // --- Prompt formatting helpers ---

    private static String formatPrompt(String format, Object... args) {
        if (format == null || format.isEmpty()) {
            return null;
        }
        if (args == null || args.length == 0) {
            return format;
        }
        return String.format(format, args);
    }

    private static String formatPrompt(Locale locale, String format, Object... args) {
        if (format == null || format.isEmpty()) {
            return null;
        }
        if (args == null || args.length == 0) {
            return format;
        }
        return String.format(locale, format, args);
    }
}
