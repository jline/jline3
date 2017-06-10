/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style

import org.sonatype.goodies.testsupport.TestSupport

import com.planet57.gossip.Log
import org.junit.Before
import org.slf4j.LoggerFactory

/**
 * Support for style tests.
 */
class StyleTestSupport
  extends TestSupport
{
  protected MemoryStyleSource source

  @Before
  void setUp() {
    // force bootstrap gossip logger to adapt to runtime logger-factory
    Log.configure(LoggerFactory.getILoggerFactory())

    this.source = new MemoryStyleSource()
  }
}
