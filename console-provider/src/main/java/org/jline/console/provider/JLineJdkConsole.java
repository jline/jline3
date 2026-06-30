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
 */
class JLineJdkConsole implements JdkConsole {

    private final Charset charset;
    private final Object lock = new Object();
    private Terminal terminal;
    private LineReader reader;
    private PrintWriter writer;

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
    public JdkConsole format(String format, Object... args) {
        writer().format(format, args);
        writer().flush();
        return this;
    }

    @Override
    public JdkConsole printf(String format, Object... args) {
        return format(format, args);
    }

    @Override
    public String readLine(String format, Object... args) {
        ensureInitialized();
        try {
            String prompt = formatPrompt(format, args);
            return reader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    @Override
    public String readLine() {
        return readLine(null);
    }

    @Override
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

    @Override
    public char[] readPassword() {
        return readPassword(null);
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

    private static String formatPrompt(String format, Object... args) {
        if (format == null || format.isEmpty()) {
            return null;
        }
        if (args == null || args.length == 0) {
            return format;
        }
        return String.format(format, args);
    }
}
