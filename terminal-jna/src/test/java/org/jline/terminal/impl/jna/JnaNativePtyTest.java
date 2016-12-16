/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jna;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

public class JnaNativePtyTest {

    @Test
    public void testDescriptor() {
        assumeTrue(!System.getProperty( "os.name" ).startsWith( "Windows"));
        assertNotNull(JnaNativePty.newDescriptor(4));
    }
}
