/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper that prevents closing the underlying output stream.
 * Used for system streams ({@code System.out}/{@code System.err}) to prevent
 * closing the shared {@code FileDescriptor}.
 */
public class NonCloseableOutputStream extends FilterOutputStream {

    public NonCloseableOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
