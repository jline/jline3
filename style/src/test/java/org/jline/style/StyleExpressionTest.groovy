/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style

import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Tests for {@link StyleExpression}.
 */
class StyleExpressionTest
  extends StyleTestSupport
{
  private StyleExpression underTest

  @Before
  void setUp() {
    super.setUp()
    this.underTest = new StyleExpression(new StyleResolver(source, 'test'))
  }

  @Test
  void 'evaluate expression with prefix and suffix'() {
    def result = underTest.evaluate('foo @{bold bar} baz')
    println result.toAnsi()
    assert result == new AttributedStringBuilder()
        .append('foo ')
        .append('bar', org.jline.utils.AttributedStyle.BOLD)
        .append(' baz')
        .toAttributedString()
  }

  @Test
  void 'evaluate expression with prefix'() {
    def result = underTest.evaluate('foo @{bold bar}')
    println result.toAnsi()
    assert result == new AttributedStringBuilder()
        .append('foo ')
        .append('bar', org.jline.utils.AttributedStyle.BOLD)
        .toAttributedString()
  }

  @Test
  void 'evaluate expression with suffix'() {
    def result = underTest.evaluate('@{bold foo} bar')
    println result.toAnsi()
    assert result == new AttributedStringBuilder()
        .append('foo', org.jline.utils.AttributedStyle.BOLD)
        .append(' bar')
        .toAttributedString()
  }

  @Test
  void 'evaluate expression'() {
    def result = underTest.evaluate('@{bold foo}')
    println result.toAnsi()
    assert result == new AttributedString('foo', org.jline.utils.AttributedStyle.BOLD)
  }

  @Test
  void 'evaluate expression with default'() {
    def result = underTest.evaluate('@{.foo:-bold foo}')
    println result.toAnsi()
    assert result == new AttributedString('foo', org.jline.utils.AttributedStyle.BOLD)
  }

  @Test
  void 'evaluate expression with multiple replacements'() {
    def result = underTest.evaluate('@{bold foo} @{fg:red bar} @{underline baz}')
    println result.toAnsi()
    assert result == new AttributedStringBuilder()
        .append('foo', org.jline.utils.AttributedStyle.BOLD)
        .append(' ')
        .append('bar', org.jline.utils.AttributedStyle.DEFAULT.foreground(org.jline.utils.AttributedStyle.RED))
        .append(' ')
        .append('baz', org.jline.utils.AttributedStyle.DEFAULT.underline())
        .toAttributedString()
  }

  @Test
  void 'evaluate expression missing value'() {
    def result = underTest.evaluate('@{bold}')
    println result.toAnsi()
    assert result == new AttributedString('@{bold}', org.jline.utils.AttributedStyle.DEFAULT)
  }

  @Test
  void 'evaluate expression missing tokens'() {
    def result = underTest.evaluate('foo')
    println result.toAnsi()
    assert result == new AttributedString('foo', org.jline.utils.AttributedStyle.DEFAULT)
  }

  @Test
  @Ignore("FIXME: need to adjust parser to cope with } in value")
  void 'evaluate expression with ${} value'() {
    def result = underTest.evaluate('@{bold,fg:cyan ${foo}}')
    println result.toAnsi()
    // FIXME: this is not presently valid; will match value '${foo'
    assert result == new AttributedString('${foo}', org.jline.utils.AttributedStyle.DEFAULT.foreground(org.jline.utils.AttributedStyle.CYAN))
  }
}
