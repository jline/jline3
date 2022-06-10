/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jna;

import java.io.IOException;

import com.sun.jna.Platform;
import org.jline.terminal.Size;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class JnaNativePtyTest {

    @Test
    public void testDescriptor() {
        assumeTrue(!System.getProperty("os.name").startsWith("Windows"));
        assertNotNull(JnaNativePty.newDescriptor(4));
    }


    @Test
    public void testOpen() throws IOException {
        // https://github.com/jline/jline3/issues/688
        // currently disabled on Mac M1 silicon
        assumeFalse(Platform.isMac() && Platform.is64Bit() && Platform.isARM());
        assumeFalse(Platform.isWindows());
        JnaNativePty pty = JnaNativePty.open(null, null);
        assertNotNull(pty);
        Size sz = pty.getSize();
        assertNotNull(sz);
        Size nsz = new Size(sz.getColumns() + 1, sz.getRows() + 1);
        pty.setSize(nsz);
        sz = pty.getSize();
        assertNotNull(sz);
        assertEquals(nsz, sz);
    }
}
