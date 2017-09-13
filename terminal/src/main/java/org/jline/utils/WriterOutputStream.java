/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Redirects an {@link OutputStream} to a {@link Writer} by decoding the data
 * using the specified {@link Charset}.
 *
 * <p><b>Note:</b> This class should only be used if it is necessary to
 * redirect an {@link OutputStream} to a {@link Writer} for compatibility
 * purposes. It is much more efficient to write to the {@link Writer}
 * directly.</p>
 */
public class WriterOutputStream extends OutputStream {

    private final Writer out;
    private final Charset charset;

    public WriterOutputStream(Writer out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(new String(b, this.charset));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(new String(b, off, len, this.charset));
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

}
