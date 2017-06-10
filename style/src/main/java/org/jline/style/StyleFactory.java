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
import org.jline.utils.AttributedStyle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory to create styled strings.
 *
 * @since TBD
 */
public class StyleFactory
{
  private final StyleResolver resolver;

  public StyleFactory(final StyleResolver resolver) {
    this.resolver = checkNotNull(resolver);
  }

  /**
   * Encode string with style applying value.
   */
  public AttributedString style(final String style, final String value) {
    checkNotNull(value);
    AttributedStyle astyle = resolver.resolve(style);
    return new AttributedString(value, astyle);
  }

  /**
   * Encode string with style formatted value.
   *
   * @see #style(String, String)
   */
  public AttributedString style(final String style, final String format, final Object... params) {
    checkNotNull(format);
    checkNotNull(params);
    // params may be empty
    return style(style, String.format(format, params));
  }

  /**
   * Evaluate a style expression.
   */
  public AttributedString evaluate(final String expression) {
    checkNotNull(expression);
    return new StyleExpression(resolver).evaluate(expression);
  }

  /**
   * Evaluate a style expression with format.
   *
   * @see #evaluate(String)
   */
  public AttributedString evaluate(final String format, final Object... params) {
    checkNotNull(format);
    checkNotNull(params);
    // params may be empty
    return evaluate(String.format(format, params));
  }
}
