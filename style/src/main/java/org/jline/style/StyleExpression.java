/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@code @{style value}} expression evaluation.
 *
 * @since TBD
 */
public class StyleExpression
{
  /**
   * Regular-expression to match {@code @{style value}}.
   */
  private static final Pattern PATTERN = Pattern.compile("@\\{([^ ]+) ([^}]+)\\}");

  private final StyleResolver resolver;

  public StyleExpression(final StyleResolver resolver) {
    this.resolver = requireNonNull(resolver);
  }

  /**
   * Evaluate expression and append to buffer.
   */
  public void evaluate(final AttributedStringBuilder buff, final String expression) {
    requireNonNull(buff);
    requireNonNull(expression);

    String input = expression;
    Matcher matcher = PATTERN.matcher(input);

    while (matcher.find()) {
      String spec = matcher.group(1);
      String value = matcher.group(2);

      // pull off the unmatched prefix of input
      int start = matcher.start(0);
      String prefix = input.substring(0, start);

      // pull off remainder from match
      int end = matcher.end(0);
      String suffix = input.substring(end, input.length());

      // resolve style
      AttributedStyle style = resolver.resolve(spec);

      // apply to buffer
      buff.append(prefix)
          .append(value, style);

      // reset matcher to the suffix of this match
      input = suffix;
      matcher.reset(input);
    }

    // append anything left over
    buff.append(input);
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
