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
import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;

/**
 * Factory for creating styled strings using a specific style group.
 * <p>
 * This class provides methods for creating {@link AttributedString}s with styles
 * applied to them. It uses a {@link StyleResolver} to resolve style specifications
 * into {@link AttributedStyle} objects.
 * </p>
 * <p>
 * The factory supports two main ways of creating styled strings:
 * </p>
 * <ul>
 *   <li>Direct styling with the {@link #style(String, String)} methods</li>
 *   <li>Style expression evaluation with the {@link #evaluate(String)} methods</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * StyleFactory factory = Styler.factory("mygroup");
 *
 * // Direct styling
 * AttributedString text1 = factory.style("bold,fg:red", "Important message");
 * AttributedString text2 = factory.style(".error", "Error message"); // Named style
 *
 * // Style expression evaluation
 * AttributedString text3 = factory.evaluate("Normal text with @{bold,fg:red important} parts");
 * </pre>
 *
 * @since 3.4
 * @see StyleResolver
 * @see StyleExpression
 * @see Styler#factory(String)
 */
public class StyleFactory {
    private final StyleResolver resolver;

    /**
     * Constructs a new StyleFactory with the specified StyleResolver.
     * <p>
     * This constructor creates a StyleFactory that will use the specified
     * StyleResolver to resolve style specifications when creating styled strings.
     * </p>
     * <p>
     * Typically, you would use {@link Styler#factory(String)} to create a
     * StyleFactory rather than constructing one directly.
     * </p>
     *
     * @param resolver the style resolver to use (must not be null)
     * @throws NullPointerException if resolver is null
     * @see Styler#factory(String)
     */
    public StyleFactory(final StyleResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    /**
     * Creates a styled string by applying the specified style to the given value.
     * <p>
     * This method resolves the style specification using the factory's StyleResolver
     * and applies the resulting AttributedStyle to the value.
     * </p>
     * <p>
     * The style specification can be in any format supported by {@link StyleResolver},
     * including direct style specifications and named style references.
     * </p>
     * <p>
     * Examples:
     * </p>
     * <pre>
     * // Direct style specification
     * AttributedString text1 = factory.style("bold,fg:red", "Important message");
     *
     * // Named style reference
     * AttributedString text2 = factory.style(".error", "Error message");
     *
     * // Named style reference with default
     * AttributedString text3 = factory.style(".missing:-bold,fg:blue", "Fallback message");
     * </pre>
     *
     * @param style the style specification to apply (must not be null)
     * @param value the text value to style (must not be null)
     * @return the resulting AttributedString
     * @throws NullPointerException if value is null
     */
    public AttributedString style(final String style, final String value) {
        requireNonNull(value);
        AttributedStyle astyle = resolver.resolve(style);
        return new AttributedString(value, astyle);
    }

    /**
     * Creates a styled string by applying the specified style to a formatted value.
     * <p>
     * This method is similar to {@link #style(String, String)}, but it allows
     * formatting the value using {@link String#format(String, Object...)} before
     * applying the style.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * AttributedString text = factory.style("bold,fg:red", "Error: %s", "File not found");
     * </pre>
     *
     * @param style the style specification to apply (must not be null)
     * @param format the format string (must not be null)
     * @param params the parameters to use for formatting (must not be null, but may be empty)
     * @return the resulting AttributedString
     * @throws NullPointerException if format or params is null
     * @see #style(String, String)
     * @see String#format(String, Object...)
     */
    public AttributedString style(final String style, final String format, final Object... params) {
        requireNonNull(format);
        requireNonNull(params);
        // params may be empty
        return style(style, String.format(format, params));
    }

    /**
     * Evaluates a style expression and returns the result as an AttributedString.
     * <p>
     * This method processes the given expression, resolving any style expressions
     * in the format {@code @{style value}}, and returns the resulting styled text
     * as an AttributedString. It uses a {@link StyleExpression} with the factory's
     * StyleResolver to evaluate the expression.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * AttributedString text = factory.evaluate("Normal text with @{bold,fg:red important} parts");
     * </pre>
     *
     * @param expression the expression to evaluate (must not be null)
     * @return the resulting AttributedString
     * @throws NullPointerException if expression is null
     * @see StyleExpression#evaluate(String)
     */
    public AttributedString evaluate(final String expression) {
        requireNonNull(expression);
        return new StyleExpression(resolver).evaluate(expression);
    }

    /**
     * Evaluates a style expression with formatting and returns the result as an AttributedString.
     * <p>
     * This method is similar to {@link #evaluate(String)}, but it allows
     * formatting the expression using {@link String#format(String, Object...)}
     * before evaluating it.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * AttributedString text = factory.evaluate("File: @{bold,fg:blue %s}", "example.txt");
     * </pre>
     *
     * @param format the format string (must not be null)
     * @param params the parameters to use for formatting (must not be null, but may be empty)
     * @return the resulting AttributedString
     * @throws NullPointerException if format or params is null
     * @see #evaluate(String)
     * @see String#format(String, Object...)
     */
    public AttributedString evaluate(final String format, final Object... params) {
        requireNonNull(format);
        requireNonNull(params);
        // params may be empty
        return evaluate(String.format(format, params));
    }
}
