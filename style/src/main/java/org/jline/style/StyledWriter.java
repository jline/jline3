/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import javax.annotation.Nonnull;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

import static java.util.Objects.requireNonNull;

/**
 * A {@link PrintWriter} extension that understands and evaluates {@link StyleExpression} syntax.
 * <p>
 * This class extends PrintWriter to provide automatic evaluation of style expressions
 * in the format {@code @{style value}} when writing strings. It uses a {@link StyleExpression}
 * to evaluate the expressions and a {@link Terminal} to convert the resulting
 * {@link AttributedString}s to ANSI escape sequences appropriate for the terminal.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * Terminal terminal = ...; // Get a Terminal instance
 * StyleResolver resolver = Styler.resolver("mygroup");
 *
 * // Create a StyledWriter that writes to System.out
 * StyledWriter writer = new StyledWriter(System.out, terminal, resolver, true);
 *
 * // Write styled text
 * writer.println("Normal text with @{bold,fg:red important} parts");
 * writer.printf("@{bold %s}: @{fg:blue %s}", "Name", "John Doe");
 * </pre>
 *
 * @since 3.4
 * @see StyleExpression
 * @see PrintWriter
 * @see Terminal
 */
public class StyledWriter extends PrintWriter {
    private final Terminal terminal;

    private final StyleExpression expression;

    /**
     * Constructs a new StyledWriter that writes to a Writer.
     * <p>
     * This constructor creates a StyledWriter that will write to the specified Writer,
     * using the specified Terminal to convert AttributedStrings to ANSI escape sequences
     * and the specified StyleResolver to resolve style specifications.
     * </p>
     *
     * @param out the Writer to write to
     * @param terminal the Terminal to use for ANSI conversion (must not be null)
     * @param resolver the StyleResolver to use for style resolution (must not be null)
     * @param autoFlush whether to automatically flush the output after each print operation
     * @throws NullPointerException if terminal is null
     */
    public StyledWriter(
            final Writer out, final Terminal terminal, final StyleResolver resolver, final boolean autoFlush) {
        super(out, autoFlush);
        this.terminal = requireNonNull(terminal);
        this.expression = new StyleExpression(resolver);
    }

    /**
     * Constructs a new StyledWriter that writes to an OutputStream.
     * <p>
     * This constructor creates a StyledWriter that will write to the specified OutputStream,
     * using the specified Terminal to convert AttributedStrings to ANSI escape sequences
     * and the specified StyleResolver to resolve style specifications.
     * </p>
     *
     * @param out the OutputStream to write to
     * @param terminal the Terminal to use for ANSI conversion (must not be null)
     * @param resolver the StyleResolver to use for style resolution (must not be null)
     * @param autoFlush whether to automatically flush the output after each print operation
     * @throws NullPointerException if terminal is null
     */
    public StyledWriter(
            final OutputStream out, final Terminal terminal, final StyleResolver resolver, final boolean autoFlush) {
        super(out, autoFlush);
        this.terminal = requireNonNull(terminal);
        this.expression = new StyleExpression(resolver);
    }

    /**
     * Writes a string after evaluating any style expressions it contains.
     * <p>
     * This method overrides the standard write method to evaluate any style expressions
     * in the format {@code @{style value}} in the input string before writing it.
     * The resulting AttributedString is converted to ANSI escape sequences appropriate
     * for the terminal.
     * </p>
     *
     * @param value the string to write (must not be null)
     * @throws NullPointerException if value is null
     */
    @Override
    public void write(@Nonnull final String value) {
        AttributedString result = expression.evaluate(value);
        super.write(result.toAnsi(terminal));
    }

    // Prevent partial output from being written while formatting or we will get rendering exceptions

    /**
     * Formats a string and writes it after evaluating any style expressions it contains.
     * <p>
     * This method overrides the standard format method to ensure that the entire
     * formatted string is evaluated for style expressions before any output is written.
     * This prevents partial output from being written, which could lead to rendering
     * exceptions with ANSI escape sequences.
     * </p>
     *
     * @param format the format string (must not be null)
     * @param args the arguments referenced by the format specifiers in the format string
     * @return this StyledWriter
     * @throws NullPointerException if format is null
     * @see String#format(String, Object...)
     */
    @Override
    public PrintWriter format(@Nonnull final String format, final Object... args) {
        print(String.format(format, args));
        return this;
    }

    /**
     * Formats a string using the specified locale and writes it after evaluating any style expressions it contains.
     * <p>
     * This method overrides the standard format method to ensure that the entire
     * formatted string is evaluated for style expressions before any output is written.
     * This prevents partial output from being written, which could lead to rendering
     * exceptions with ANSI escape sequences.
     * </p>
     *
     * @param locale the locale to use for formatting
     * @param format the format string (must not be null)
     * @param args the arguments referenced by the format specifiers in the format string
     * @return this StyledWriter
     * @throws NullPointerException if format is null
     * @see String#format(Locale, String, Object...)
     */
    @Override
    public PrintWriter format(final Locale locale, @Nonnull final String format, final Object... args) {
        print(String.format(locale, format, args));
        return this;
    }
}
