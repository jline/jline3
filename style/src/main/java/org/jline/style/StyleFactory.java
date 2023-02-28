/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;

/**
 * Factory to create styled strings.
 *
 * @since 3.4
 */
public class StyleFactory {
    private final StyleResolver resolver;

    public StyleFactory(final StyleResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    /**
     * Encode string with style applying value.
     *
     * @param style the style
     * @param value the value
     * @return the result string
     */
    public AttributedString style(final String style, final String value) {
        requireNonNull(value);
        AttributedStyle astyle = resolver.resolve(style);
        return new AttributedString(value, astyle);
    }

    /**
     * Encode string with style formatted value.
     *
     * @param style the style
     * @param format the format
     * @param params the parameters
     * @return the result string
     * @see #style(String, String)
     */
    public AttributedString style(final String style, final String format, final Object... params) {
        requireNonNull(format);
        requireNonNull(params);
        // params may be empty
        return style(style, String.format(format, params));
    }

    /**
     * Evaluate a style expression.
     *
     * @param expression the expression to evaluate
     * @return the result string
     */
    public AttributedString evaluate(final String expression) {
        requireNonNull(expression);
        return new StyleExpression(resolver).evaluate(expression);
    }

    /**
     * Evaluate a style expression with format.
     *
     * @param format the format
     * @param params the parameters
     * @return the result string
     * @see #evaluate(String)
     */
    public AttributedString evaluate(final String format, final Object... params) {
        requireNonNull(format);
        requireNonNull(params);
        // params may be empty
        return evaluate(String.format(format, params));
    }
}
