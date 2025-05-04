/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;

/**
 * Provides evaluation of style expressions in the format {@code @{style value}}.
 * <p>
 * This class allows embedding styled text within regular strings using a special
 * syntax. Style expressions are enclosed in {@code @{...}} delimiters, where the
 * first part specifies the style and the rest is the text to be styled.
 * </p>
 * <p>
 * Style expressions can be nested and combined with regular text. The style
 * specification can be a direct style specification or a reference to a named
 * style in a {@link StyleSource}.
 * </p>
 * <p>
 * Examples of style expressions:
 * </p>
 * <pre>
 * "Normal text with @{bold,fg:red important} parts"
 * "@{bold Header}: @{fg:blue Value}"
 * "@{.error Error message}" (references a named style "error")
 * </pre>
 *
 * @since 3.4
 * @see StyleResolver
 * @see InterpolationHelper
 */
public class StyleExpression {

    private final StyleResolver resolver;

    /**
     * Constructs a new StyleExpression with a default StyleResolver.
     * <p>
     * This constructor creates a StyleExpression with a StyleResolver that
     * uses a {@link NopStyleSource} and an empty group name. This means that
     * only direct style specifications will work; named style references will
     * always resolve to null.
     * </p>
     */
    public StyleExpression() {
        this(new StyleResolver(new NopStyleSource(), ""));
    }

    /**
     * Constructs a new StyleExpression with the specified StyleResolver.
     * <p>
     * This constructor creates a StyleExpression that will use the specified
     * StyleResolver to resolve style specifications within expressions.
     * </p>
     *
     * @param resolver the style resolver to use (must not be null)
     * @throws NullPointerException if resolver is null
     */
    public StyleExpression(final StyleResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    /**
     * Evaluates a style expression and appends the result to the specified buffer.
     * <p>
     * This method processes the given expression, resolving any style expressions
     * in the format {@code @{style value}}, and appends the resulting styled text
     * to the provided buffer.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * AttributedStringBuilder buffer = new AttributedStringBuilder();
     * StyleExpression expr = new StyleExpression(resolver);
     * expr.evaluate(buffer, "Normal text with @{bold,fg:red important} parts");
     * </pre>
     *
     * @param buff the buffer to append the evaluated expression to (must not be null)
     * @param expression the expression to evaluate (must not be null)
     * @throws NullPointerException if buff or expression is null
     */
    public void evaluate(final AttributedStringBuilder buff, final String expression) {
        requireNonNull(buff);
        requireNonNull(expression);

        String translated = InterpolationHelper.substVars(expression, this::style, false);
        buff.appendAnsi(translated);
    }

    private String style(String key) {
        int idx = key.indexOf(' ');
        if (idx > 0) {
            String spec = key.substring(0, idx);
            String value = key.substring(idx + 1);
            AttributedStyle style = resolver.resolve(spec);
            return new AttributedStringBuilder().style(style).ansiAppend(value).toAnsi();
        }
        return null;
    }

    /**
     * Evaluates a style expression and returns the result as an AttributedString.
     * <p>
     * This method processes the given expression, resolving any style expressions
     * in the format {@code @{style value}}, and returns the resulting styled text
     * as an AttributedString.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * StyleExpression expr = new StyleExpression(resolver);
     * AttributedString result = expr.evaluate("Normal text with @{bold,fg:red important} parts");
     * </pre>
     *
     * @param expression the expression to evaluate (must not be null)
     * @return the resulting AttributedString
     * @throws NullPointerException if expression is null
     */
    public AttributedString evaluate(final String expression) {
        AttributedStringBuilder buff = new AttributedStringBuilder();
        evaluate(buff, expression);
        return buff.toAttributedString();
    }
}
