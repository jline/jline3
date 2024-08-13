/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.fusesource.jansi.io;

import java.io.OutputStream;

/**
 * A simple buffering output stream with no synchronization.
 *
 * @deprecated Use {@link org.jline.jansi.io.FastBufferedOutputStream} instead.
 */
@Deprecated
public class FastBufferedOutputStream extends org.jline.utils.FastBufferedOutputStream {

    public FastBufferedOutputStream(OutputStream out) {
        super(out);
    }
}
