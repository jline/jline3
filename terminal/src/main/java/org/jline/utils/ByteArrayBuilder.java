/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A reusable byte buffer optimized for building ANSI escape sequences and terminal output.
 *
 * <p>
 * This class provides efficient methods for constructing terminal output as raw bytes,
 * bypassing the overhead of {@link StringBuilder} and charset encoding for content that
 * is overwhelmingly ASCII (ANSI escape sequences, color codes, cursor positioning).
 * </p>
 *
 * <p>Key optimizations over String-based output:</p>
 * <ul>
 *   <li>{@link #appendInt(int)} writes ASCII digit bytes directly without {@code Integer.toString()}</li>
 *   <li>{@link #appendAscii(String)} copies bytes directly without charset encoding</li>
 *   <li>{@link #appendUtf8(char)} fast-paths single-byte ASCII characters</li>
 *   <li>{@link #reset()} reuses the buffer across frames without reallocation</li>
 * </ul>
 */
public class ByteArrayBuilder {

    private byte[] buffer;
    private int count;

    public ByteArrayBuilder() {
        this(256);
    }

    public ByteArrayBuilder(int initialCapacity) {
        buffer = new byte[initialCapacity];
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > buffer.length) {
            long doubleCap = (long) buffer.length * 2;
            int newCapacity = (int) Math.max(Math.min(doubleCap, Integer.MAX_VALUE), minCapacity);
            byte[] newBuffer = new byte[newCapacity];
            System.arraycopy(buffer, 0, newBuffer, 0, count);
            buffer = newBuffer;
        }
    }

    /**
     * Appends a CSI (Control Sequence Introducer) escape: ESC [
     */
    public ByteArrayBuilder csi() {
        ensureCapacity(count + 2);
        buffer[count++] = 0x1b;
        buffer[count++] = '[';
        return this;
    }

    /**
     * Appends a single ASCII character as a byte.
     */
    public ByteArrayBuilder appendAscii(char c) {
        ensureCapacity(count + 1);
        buffer[count++] = (byte) c;
        return this;
    }

    /**
     * Appends an ASCII string directly as bytes, bypassing charset encoding.
     * The string must contain only ASCII characters (0x00-0x7F).
     */
    public ByteArrayBuilder appendAscii(String s) {
        int len = s.length();
        ensureCapacity(count + len);
        for (int i = 0; i < len; i++) {
            buffer[count++] = (byte) s.charAt(i);
        }
        return this;
    }

    /**
     * Appends an integer as ASCII digit bytes without creating a String.
     * Optimized for the common case of small values (0-999) used in ANSI color codes.
     */
    public ByteArrayBuilder appendInt(int value) {
        if (value < 0) {
            if (value == Integer.MIN_VALUE) {
                return appendAscii(Integer.toString(value));
            }
            appendAscii('-');
            appendInt(-value);
            return this;
        }
        if (value < 10) {
            ensureCapacity(count + 1);
            buffer[count++] = (byte) ('0' + value);
        } else if (value < 100) {
            ensureCapacity(count + 2);
            buffer[count++] = (byte) ('0' + value / 10);
            buffer[count++] = (byte) ('0' + value % 10);
        } else if (value < 1000) {
            ensureCapacity(count + 3);
            buffer[count++] = (byte) ('0' + value / 100);
            buffer[count++] = (byte) ('0' + (value / 10) % 10);
            buffer[count++] = (byte) ('0' + value % 10);
        } else {
            appendAscii(Integer.toString(value));
        }
        return this;
    }

    /**
     * Appends a character as UTF-8 encoded bytes.
     * Fast-paths ASCII characters (single byte).
     */
    public ByteArrayBuilder appendUtf8(char c) {
        if (c < 0x80) {
            ensureCapacity(count + 1);
            buffer[count++] = (byte) c;
        } else if (c < 0x800) {
            ensureCapacity(count + 2);
            buffer[count++] = (byte) (0xC0 | (c >> 6));
            buffer[count++] = (byte) (0x80 | (c & 0x3F));
        } else {
            ensureCapacity(count + 3);
            buffer[count++] = (byte) (0xE0 | (c >> 12));
            buffer[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
            buffer[count++] = (byte) (0x80 | (c & 0x3F));
        }
        return this;
    }

    /**
     * Appends a Unicode code point as UTF-8 encoded bytes.
     * Handles supplementary characters (code points above U+FFFF).
     */
    public ByteArrayBuilder appendUtf8(int codePoint) {
        if (codePoint < 0x80) {
            ensureCapacity(count + 1);
            buffer[count++] = (byte) codePoint;
        } else if (codePoint < 0x800) {
            ensureCapacity(count + 2);
            buffer[count++] = (byte) (0xC0 | (codePoint >> 6));
            buffer[count++] = (byte) (0x80 | (codePoint & 0x3F));
        } else if (codePoint < 0x10000) {
            ensureCapacity(count + 3);
            buffer[count++] = (byte) (0xE0 | (codePoint >> 12));
            buffer[count++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
            buffer[count++] = (byte) (0x80 | (codePoint & 0x3F));
        } else {
            ensureCapacity(count + 4);
            buffer[count++] = (byte) (0xF0 | (codePoint >> 18));
            buffer[count++] = (byte) (0x80 | ((codePoint >> 12) & 0x3F));
            buffer[count++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
            buffer[count++] = (byte) (0x80 | (codePoint & 0x3F));
        }
        return this;
    }

    /**
     * Returns the internal buffer. Only bytes from index 0 to {@link #length()} - 1 are valid.
     */
    public byte[] buffer() {
        return buffer;
    }

    /**
     * Returns the number of bytes written to the buffer.
     */
    public int length() {
        return count;
    }

    /**
     * Resets the write position to zero without deallocating the buffer.
     */
    public void reset() {
        count = 0;
    }

    /**
     * Returns a copy of the buffer contents as a byte array.
     */
    public byte[] toByteArray() {
        byte[] result = new byte[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    /**
     * Writes the buffer contents to an output stream.
     */
    public void writeTo(OutputStream out) throws IOException {
        if (count > 0) {
            out.write(buffer, 0, count);
        }
    }

    /**
     * Returns the buffer contents as a UTF-8 string.
     */
    public String toStringUtf8() {
        return new String(buffer, 0, count, StandardCharsets.UTF_8);
    }

    /**
     * Returns an {@link Appendable} view that writes ASCII characters to this builder.
     * Suitable for use with {@link Curses#tputs(Appendable, String, Object...)} since
     * terminal capability sequences are pure ASCII.
     */
    public Appendable asAsciiAppendable() {
        return new AsciiAppendable();
    }

    private class AsciiAppendable implements Appendable {
        @Override
        public Appendable append(CharSequence csq) {
            if (csq == null) {
                csq = "null";
            }
            int len = csq.length();
            ensureCapacity(count + len);
            for (int i = 0; i < len; i++) {
                buffer[count++] = (byte) csq.charAt(i);
            }
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            if (csq == null) {
                csq = "null";
            }
            int len = end - start;
            ensureCapacity(count + len);
            for (int i = start; i < end; i++) {
                buffer[count++] = (byte) csq.charAt(i);
            }
            return this;
        }

        @Override
        public Appendable append(char c) {
            ensureCapacity(count + 1);
            buffer[count++] = (byte) c;
            return this;
        }
    }
}
