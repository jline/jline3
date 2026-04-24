/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * An OutputStream that decodes bytes and writes them to a {@link ScreenTerminal},
 * feeding any VT100 responses back as terminal input.
 *
 * <p>This class unifies the output stream pattern used when a {@link ScreenTerminal}
 * is wired to a {@link org.jline.terminal.impl.LineDisciplineTerminal}.</p>
 */
public class ScreenTerminalOutputStream extends OutputStream {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final CharsetDecoder decoder;
    private final ScreenTerminal screenTerminal;
    private final OutputStream feedbackOutput;

    public ScreenTerminalOutputStream(ScreenTerminal screenTerminal, Charset charset, OutputStream feedbackOutput) {
        this.screenTerminal = screenTerminal;
        this.feedbackOutput = feedbackOutput;
        this.decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    @Override
    public synchronized void write(int b) {
        buffer.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        buffer.write(b, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        int size = buffer.size();
        if (size > 0) {
            CharBuffer out;
            for (; ; ) {
                out = CharBuffer.allocate(size);
                ByteBuffer in = ByteBuffer.wrap(buffer.toByteArray());
                CoderResult result = decoder.decode(in, out, false);
                if (result.isOverflow()) {
                    size *= 2;
                } else {
                    buffer.reset();
                    buffer.write(in.array(), in.arrayOffset() + in.position(), in.remaining());
                    break;
                }
            }
            if (out.position() > 0) {
                out.flip();
                screenTerminal.write(out);
                if (feedbackOutput != null) {
                    String response = screenTerminal.read();
                    if (!response.isEmpty()) {
                        feedbackOutput.write(response.getBytes());
                    }
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * A placeholder OutputStream whose delegate can be set after construction.
     * Used when {@code super()} requires an OutputStream but the real one
     * cannot be created until after the constructor returns.
     */
    public static class DelegateOutputStream extends OutputStream {
        OutputStream output;

        public DelegateOutputStream() {}

        @Override
        public void write(int b) throws IOException {
            if (output != null) output.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (output != null) output.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (output != null) output.flush();
        }

        @Override
        public void close() throws IOException {
            if (output != null) output.close();
        }
    }
}
