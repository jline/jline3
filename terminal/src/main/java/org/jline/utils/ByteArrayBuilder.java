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
class ByteArrayBuilder {

    private byte[] buffer;
    private int count;

    /**
     * Creates a new ByteArrayBuilder with a default initial capacity of 256 bytes.
     */
    public ByteArrayBuilder() {
        this(256);
    }

    /**
     * Creates a ByteArrayBuilder with the specified initial capacity.
     *
     * @param initialCapacity the initial size of the internal byte buffer in bytes
     */
    public ByteArrayBuilder(int initialCapacity) {
        buffer = new byte[initialCapacity];
    }

    /**
     * Ensures the internal byte buffer has length at least {@code minCapacity}, growing it if necessary.
     *
     * If the current buffer is too small, allocates a larger array (at least {@code minCapacity}),
     * copies existing bytes, and replaces the buffer. Growth attempts to double the current size,
     * capped to {@code Integer.MAX_VALUE}.
     *
     * @param minCapacity minimum required capacity for the internal buffer
     */
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
     * Appends the CSI (Control Sequence Introducer) two-byte sequence ESC '[' to the buffer.
     *
     * @return this ByteArrayBuilder instance for call chaining
     */
    public ByteArrayBuilder csi() {
        ensureCapacity(count + 2);
        buffer[count++] = 0x1b;
        buffer[count++] = '[';
        return this;
    }

    /**
     * Appends the given ASCII character as a single byte to the builder.
     *
     * @param c the ASCII character to append (expected in range U+0000..U+007F)
     * @return this ByteArrayBuilder
     */
    public ByteArrayBuilder appendAscii(char c) {
        ensureCapacity(count + 1);
        buffer[count++] = (byte) c;
        return this;
    }

    /**
     * Appends the characters of the given string as raw ASCII bytes to the builder without charset encoding.
     *
     * @param s the ASCII string to append; each character must be in the range 0x00–0x7F
     * @return this ByteArrayBuilder
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
     * Appends the decimal ASCII representation of an integer to the buffer.
     *
     * <p>Negative values are prefixed with `'-'`. `Integer.MIN_VALUE` and values
     * outside the common small range are appended via a `String` fallback.
     *
     * @param value the integer to append as ASCII digits
     * @return this builder instance for method chaining
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
     * Appends the given character to the builder by encoding it as UTF-8 bytes.
     *
     * @param c the character to encode and append
     * @return this ByteArrayBuilder instance
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
     * Append the given Unicode code point encoded as UTF-8 bytes.
     *
     * @param codePoint the Unicode code point to append (U+0000..U+10FFFF)
     * @return the builder instance for chaining
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
     * Reports the number of bytes written to the buffer.
     *
     * @return the number of valid bytes stored in the internal buffer
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
     * Create a new byte array containing the bytes written to the builder.
     *
     * @return a newly allocated byte array with the builder's contents (bytes at indices 0..length()-1)
     */
    public byte[] toByteArray() {
        byte[] result = new byte[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    /**
     * Writes the valid bytes stored in this builder to the given output stream.
     *
     * Writes the bytes in the internal buffer from index 0 (inclusive) to {@link #length()} (exclusive).
     * If the builder is empty, this method does nothing.
     *
     * @param out the OutputStream to which the bytes will be written
     * @throws IOException if an I/O error occurs while writing to the stream
     */
    public void writeTo(OutputStream out) throws IOException {
        if (count > 0) {
            out.write(buffer, 0, count);
        }
    }

    /**
     * Convert the builder's written bytes to a UTF-8 String.
     *
     * @return the bytes from index 0 to length()-1 decoded using UTF-8
     */
    public String toStringUtf8() {
        return new String(buffer, 0, count, StandardCharsets.UTF_8);
    }

    /**
     * Provides an Appendable that writes ASCII characters into this builder.
     *
     * The returned Appendable encodes each character as a single ASCII byte and appends it
     * to this ByteArrayBuilder, making it suitable for Appendable-based APIs that emit
     * terminal capability sequences.
     *
     * @return an Appendable whose append methods write characters as ASCII bytes into this builder
     */
    public Appendable asAsciiAppendable() {
        return new AsciiAppendable();
    }

    private class AsciiAppendable implements Appendable {
        /**
         * Appends the characters of a CharSequence to the enclosing ByteArrayBuilder as ASCII bytes.
         *
         * If {@code csq} is {@code null}, the four characters "null" are appended. Each character is written
         * by casting to a single byte (low 8 bits).
         *
         * @param csq the character sequence to append, or {@code null}
         * @return this Appendable
         */
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

        /**
         * Appends the subsequence [start, end) of the given CharSequence as ASCII bytes.
         *
         * If {@code csq} is {@code null}, the four-character sequence {@code "null"} is appended.
         * Each character is cast to a single byte and written into the enclosing ByteArrayBuilder.
         *
         * @param csq   the character sequence to append, or {@code null}
         * @param start start index, inclusive
         * @param end   end index, exclusive
         * @return      this Appendable
         */
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

        /**
         * Appends a single character to the builder as an ASCII byte.
         *
         * @param c the character to append; its low-order 8 bits are written as a single byte
         * @return this Appendable instance
         */
        @Override
        public Appendable append(char c) {
            ensureCapacity(count + 1);
            buffer[count++] = (byte) c;
            return this;
        }
    }
}
