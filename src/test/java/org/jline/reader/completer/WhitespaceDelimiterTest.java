/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.completer;

import org.jline.reader.ReaderTestSupport;
import org.jline.reader.completer.ArgumentCompleter.ArgumentList;
import org.jline.reader.completer.ArgumentCompleter.WhitespaceArgumentDelimiter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for {@link WhitespaceArgumentDelimiter}.
 * 
 * @author <a href="mailto:mdrob@apache.org">Mike Drob</a>
 */
public class WhitespaceDelimiterTest extends ReaderTestSupport {

  ArgumentList delimited;
  WhitespaceArgumentDelimiter delimiter;

  @Before
  public void setUp() {
    delimiter = new WhitespaceArgumentDelimiter();
  }

  @Test
  public void testDelimit() {
    // These all passed before adding quoting and escaping
    delimited = delimiter.delimit("1 2 3", 0);
    assertArrayEquals(new String[] {"1", "2", "3"}, delimited.getArguments());

    delimited = delimiter.delimit("1  2  3", 0);
    assertArrayEquals(new String[] {"1", "2", "3"}, delimited.getArguments());
  }

  @Test
  public void testQuotedDelimit() {
    delimited = delimiter.delimit("\"1 2\" 3", 0);
    assertArrayEquals(new String[] {"1 2", "3"}, delimited.getArguments());

    delimited = delimiter.delimit("'1 2' 3", 0);
    assertArrayEquals(new String[] {"1 2", "3"}, delimited.getArguments());

    delimited = delimiter.delimit("1 '2 3'", 0);
    assertArrayEquals(new String[] {"1", "2 3"}, delimited.getArguments());
  }

  @Test
  public void testMixedQuotes() {
    delimited = delimiter.delimit("\"1' '2\" 3", 0);
    assertArrayEquals(new String[] {"1' '2", "3"}, delimited.getArguments());

    delimited = delimiter.delimit("'1\" 2' 3\"", 0);
    assertArrayEquals(new String[] {"1\" 2", "3"}, delimited.getArguments());
  }

  @Test
  public void testEscapedSpace() {
    delimited = delimiter.delimit("1\\ 2 3", 0);
    assertArrayEquals(new String[] {"1 2", "3"}, delimited.getArguments());
  }

  @Test
  public void testEscapedQuotes() {
    delimited = delimiter.delimit("'1 \\'2' 3", 0);
    assertArrayEquals(new String[] {"1 '2", "3"}, delimited.getArguments());

    delimited = delimiter.delimit("\\'1 '2' 3", 0);
    assertArrayEquals(new String[] {"'1", "2", "3"}, delimited.getArguments());

    delimited = delimiter.delimit("'1 '2\\' 3", 0);
    assertArrayEquals(new String[] {"1 ", "2'", "3"}, delimited.getArguments());
  }
}
