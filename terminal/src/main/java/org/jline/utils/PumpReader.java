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
import java.io.InterruptedIOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class PumpReader extends Reader {

    private static final int EOF = -1;
    private static final int BUFFER_SIZE = 4096;

    // Read and write buffer are backed by the same array
    private final CharBuffer readBuffer;
    private final CharBuffer writeBuffer;

    private final Writer writer;

    private boolean closed;

    public PumpReader() {
        char[] buf = new char[BUFFER_SIZE];
        this.readBuffer = CharBuffer.wrap(buf);
        this.writeBuffer = CharBuffer.wrap(buf);
        this.writer = new Writer(this);

        // There are no bytes available to read after initialization
        readBuffer.limit(0);
    }

    public java.io.Writer getWriter() {
        return this.writer;
    }

    public java.io.InputStream createInputStream(Charset charset) {
        return new InputStream(this, charset);
    }

    private boolean wait(CharBuffer buffer) throws InterruptedIOException {
        if (closed) {
            return false;
        }

        while (!buffer.hasRemaining()) {
            // Wake up waiting readers/writers
            notifyAll();

            try {
                wait();
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }

            if (closed) {
                return false;
            }
        }

        return true;
    }

    /**
     * Blocks until more input is available or the reader is closed.
     *
     * @return true if more input is available, false if the reader is closed
     * @throws InterruptedIOException If {@link #wait()} is interrupted
     */
    private boolean waitForInput() throws InterruptedIOException {
        return wait(readBuffer);
    }

    /**
     * Blocks until there is new space available for buffering or the
     * reader is closed.
     *
     * @throws InterruptedIOException If {@link #wait()} is interrupted
     * @throws ClosedException If the reader was closed
     */
    private void waitForBufferSpace() throws InterruptedIOException, ClosedException {
        if (!wait(writeBuffer)) {
            throw new ClosedException();
        }
    }

    private static boolean rewind(CharBuffer buffer, CharBuffer other) {
        // Extend limit of other buffer if there is additional input/output available
        if (buffer.position() > other.position()) {
            other.limit(buffer.position());
        }

        // If we have reached the end of the buffer, rewind and set the new limit
        if (buffer.position() == buffer.capacity()) {
            buffer.rewind();
            buffer.limit(other.position());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempts to find additional input by rewinding the {@link #readBuffer}.
     * Updates the {@link #writeBuffer} to make read bytes available for buffering.
     *
     * @return If more input is available
     */
    private boolean rewindReadBuffer() {
        return rewind(readBuffer, writeBuffer) && readBuffer.hasRemaining();
    }

    /**
     * Attempts to find additional buffer space by rewinding the {@link #writeBuffer}.
     * Updates the {@link #readBuffer} to make written bytes available to the reader.
     */
    private void rewindWriteBuffer() {
        rewind(writeBuffer, readBuffer);
    }

    @Override
    public synchronized boolean ready() {
        return readBuffer.hasRemaining();
    }

    public synchronized int available() {
        int count = readBuffer.remaining();
        if (writeBuffer.position() < readBuffer.position()) {
            count += writeBuffer.position();
        }
        return count;
    }

    @Override
    public synchronized int read() throws IOException {
        if (!waitForInput()) {
            return EOF;
        }

        int b = readBuffer.get();
        rewindReadBuffer();
        return b;
    }

    private int copyChars(char[] cbuf, int off, int len) {
        len = Math.min(len, readBuffer.remaining());
        readBuffer.get(cbuf, off, len);
        return len;
    }

    @Override
    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        if (!waitForInput()) {
            return EOF;
        }

        int count = copyChars(cbuf, off, len);
        if (rewindReadBuffer() && count < len) {
            count += copyChars(cbuf, off + count, len - count);
            rewindReadBuffer();
        }

        return count;
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        if (!waitForInput()) {
            return EOF;
        }

        int count = readBuffer.read(target);
        if (rewindReadBuffer() && target.hasRemaining()) {
            count += readBuffer.read(target);
            rewindReadBuffer();
        }

        return count;
    }

    synchronized int readBytes(CharsetEncoder encoder, byte[] b, int off, int len) throws IOException {
        if (!waitForInput()) {
            return EOF;
        }

        ByteBuffer output = ByteBuffer.wrap(b, off, len);
        CoderResult result = encoder.encode(readBuffer, output, false);
        if (rewindReadBuffer() && result.isUnderflow()) {
            encoder.encode(readBuffer, output, false);
            rewindReadBuffer();
        }

        return output.position();
    }

    synchronized void write(char c) throws IOException {
        waitForBufferSpace();
        writeBuffer.put(c);
        rewindWriteBuffer();
    }

    synchronized void write(char[] cbuf, int off, int len) throws IOException {
        while (len > 0) {
            waitForBufferSpace();

            // Copy as much characters as we can
            int count = Math.min(len, writeBuffer.remaining());
            writeBuffer.put(cbuf, off, count);

            off += count;
            len -= count;

            // Update buffer states and rewind if necessary
            rewindWriteBuffer();
        }
    }

    synchronized void write(String str, int off, int len) throws IOException {
        char[] buf = writeBuffer.array();

        while (len > 0) {
            waitForBufferSpace();

            // Copy as much characters as we can
            int count = Math.min(len, writeBuffer.remaining());
            // CharBuffer.put(String) doesn't use getChars so do it manually
            str.getChars(off, off + count, buf, writeBuffer.position());
            writeBuffer.position(writeBuffer.position() + count);

            off += count;
            len -= count;

            // Update buffer states and rewind if necessary
            rewindWriteBuffer();
        }
    }

    synchronized void flush() {
        // Notify readers
        notifyAll();
    }

    @Override
    public synchronized void close() throws IOException {
        this.closed = true;
        notifyAll();
    }

    private static class Writer extends java.io.Writer {

        private final PumpReader reader;

        private Writer(PumpReader reader) {
            this.reader = reader;
        }

        @Override
        public void write(int c) throws IOException {
            reader.write((char) c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            reader.write(cbuf, off, len);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            reader.write(str, off, len);
        }

        @Override
        public void flush() throws IOException {
            reader.flush();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

    private static class InputStream extends java.io.InputStream {

        private final PumpReader reader;
        private final CharsetEncoder encoder;

        private InputStream(PumpReader reader, Charset charset) {
            this.reader = reader;
            this.encoder = charset.newEncoder()
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .onMalformedInput(CodingErrorAction.REPLACE);
        }

        @Override
        public int available() throws IOException {
            return (int) (reader.available() * (double) this.encoder.averageBytesPerChar());
        }

        @Override
        public int read() throws IOException {
            byte[] buf = new byte[1];
            int count = read(buf);
            return count == 1 ? buf[0] : EOF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return reader.readBytes(this.encoder, b, off, len);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

}
