/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * An OutputStream implementation that writes to a Writer, bridging byte and character streams.
 *
 * <p>
 * The WriterOutputStream class provides an OutputStream implementation that redirects
 * its output to a Writer by decoding the bytes using a specified character encoding.
 * This allows code that expects to write to an OutputStream to work with a Writer
 * destination instead.
 * </p>
 *
 * <p>
 * This class handles the complexities of character encoding conversion, including:
 * </p>
 * <ul>
 *   <li>Proper handling of multi-byte character encodings</li>
 *   <li>Buffering of partial character sequences</li>
 *   <li>Configurable behavior for malformed input and unmappable characters</li>
 * </ul>
 *
 * <p>
 * This class is particularly useful in JLine for bridging between byte-oriented
 * and character-oriented I/O in terminal handling, especially when dealing with
 * legacy APIs that expect byte streams.
 * </p>
 *
 * <p><b>Note:</b> This class should only be used if it is necessary to
 * redirect an {@link OutputStream} to a {@link Writer} for compatibility
 * purposes. It is much more efficient to write to the {@link Writer}
 * directly.</p>
 */
public class WriterOutputStream extends OutputStream {

    private final Writer out;
    private final CharsetDecoder decoder;
    private final ByteBuffer decoderIn = ByteBuffer.allocate(256);
    private final CharBuffer decoderOut = CharBuffer.allocate(128);

    public WriterOutputStream(Writer out, Charset charset) {
        this(
                out,
                charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    public WriterOutputStream(Writer out, CharsetDecoder decoder) {
        this.out = out;
        this.decoder = decoder;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            final int c = Math.min(len, decoderIn.remaining());
            decoderIn.put(b, off, c);
            processInput(false);
            len -= c;
            off += c;
        }
        flush();
    }

    @Override
    public void flush() throws IOException {
        flushOutput();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        processInput(true);
        flush();
        out.close();
    }

    /**
     * Decode the contents of the input ByteBuffer into a CharBuffer.
     *
     * @param endOfInput indicates end of input
     * @throws IOException if an I/O error occurs
     */
    private void processInput(final boolean endOfInput) throws IOException {
        // Prepare decoderIn for reading
        decoderIn.flip();
        CoderResult coderResult;
        while (true) {
            coderResult = decoder.decode(decoderIn, decoderOut, endOfInput);
            if (coderResult.isOverflow()) {
                flushOutput();
            } else if (coderResult.isUnderflow()) {
                break;
            } else {
                // The decoder is configured to replace malformed input and unmappable characters,
                // so we should not get here.
                throw new IOException("Unexpected coder result");
            }
        }
        // Discard the bytes that have been read
        decoderIn.compact();
    }

    /**
     * Flush the output.
     *
     * @throws IOException if an I/O error occurs
     */
    private void flushOutput() throws IOException {
        if (decoderOut.position() > 0) {
            out.write(decoderOut.array(), 0, decoderOut.position());
            decoderOut.rewind();
        }
    }
}
