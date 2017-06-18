/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@code @{style value}} expression evaluation.
 *
 * @since 3.4
 */
public class StyleExpression {

    private final StyleResolver resolver;

    public StyleExpression() {
        this(new StyleResolver(new NopStyleSource(), ""));
    }

    public StyleExpression(final StyleResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    /**
     * Evaluate expression and append to buffer.
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
     * Evaluate expression.
     */
    public AttributedString evaluate(final String expression) {
        AttributedStringBuilder buff = new AttributedStringBuilder();
        evaluate(buff, expression);
        return buff.toAttributedString();
    }
}
