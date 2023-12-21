/*
 * Copyright (c) 2002-2017, the original author(s).
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
 * Styled {@link PrintWriter} which is aware of {@link StyleExpression} syntax.
 *
 * @since 3.4
 */
public class StyledWriter extends PrintWriter {
    private final Terminal terminal;

    private final StyleExpression expression;

    public StyledWriter(
            final Writer out, final Terminal terminal, final StyleResolver resolver, final boolean autoFlush) {
        super(out, autoFlush);
        this.terminal = requireNonNull(terminal);
        this.expression = new StyleExpression(resolver);
    }

    public StyledWriter(
            final OutputStream out, final Terminal terminal, final StyleResolver resolver, final boolean autoFlush) {
        super(out, autoFlush);
        this.terminal = requireNonNull(terminal);
        this.expression = new StyleExpression(resolver);
    }

    @Override
    public void write(@Nonnull final String value) {
        AttributedString result = expression.evaluate(value);
        super.write(result.toAnsi(terminal));
    }

    // Prevent partial output from being written while formatting or we will get rendering exceptions

    @Override
    public PrintWriter format(@Nonnull final String format, final Object... args) {
        print(String.format(format, args));
        return this;
    }

    @Override
    public PrintWriter format(final Locale locale, @Nonnull final String format, final Object... args) {
        print(String.format(locale, format, args));
        return this;
    }
}
