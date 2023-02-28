/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

import java.io.FileDescriptor;

/**
 * Interface to access some low level.
 *
 * @see    JLineNativeLoader
 */
@SuppressWarnings("unused")
public class JLineLibrary {

    static {
        JLineNativeLoader.initialize();
    }

    public static native FileDescriptor newFileDescriptor(int fd);
}
