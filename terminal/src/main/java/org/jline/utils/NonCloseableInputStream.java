/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper that prevents closing the underlying input stream.
 * Used for system streams ({@code System.in}) to prevent closing
 * the shared {@code FileDescriptor}.
 */
public class NonCloseableInputStream extends FilterInputStream {

    public NonCloseableInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // Do not close the underlying stream
    }
}
