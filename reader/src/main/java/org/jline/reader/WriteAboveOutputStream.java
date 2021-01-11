/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Redirects an {@link OutputStream} to a {@link LineReader}'s {@link LineReader#writeAbove(char[], int, int)} method,
 * which draws output above the current prompt / input line.
 *
 * <p>Example:</p>
 * <pre>
 *     LineReader reader = LineReaderBuilder.builder().terminal(terminal).parser(parser).build();
 *     WriteAboveOutputStream waos = new WriteAboveOutputStream(reader);
 *     waos.write(new byte[] { 0x68, 0x69, 0x21});
 * </pre>
 *
 */
public class WriteAboveOutputStream extends OutputStream {

    private final LineReader reader;

    public WriteAboveOutputStream(LineReader reader) {
        this.reader = reader;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] { (byte)b }, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        char[] c = new char[len];
        for (int i = off; i < len; i++) {
            c[i] = (char) (b[i] & 0xff);
        }
        reader.writeAbove(c, off, len);
    }
}
