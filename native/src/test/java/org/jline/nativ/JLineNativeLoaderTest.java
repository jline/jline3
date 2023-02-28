/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

import org.junit.jupiter.api.Test;

public class JLineNativeLoaderTest {

    @Test
    public void testLoadLibrary() {
        JLineNativeLoader.initialize();
    }
}
