/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NonBlockingBufferedReaderTest {

    @Test
    void readsIntoPartialBuffer() throws Exception {
        for (boolean encoded : new boolean[] {false, true}) {
            try (NonBlockingReader reader = reader("a", encoded)) {
                char[] buffer = "#####".toCharArray();
                assertEquals(1, reader.readBuffered(buffer, 1, 2, 0));
                assertArrayEquals("#a###".toCharArray(), buffer);
            }
        }
    }

    @Test
    void emptyReadDoesNotConsumeInput() throws Exception {
        for (boolean encoded : new boolean[] {false, true}) {
            try (NonBlockingReader reader = reader("a", encoded)) {
                assertEquals(0, reader.readBuffered(new char[3], 1, 0, 0));
                assertEquals('a', reader.read());
            }
        }
    }

    @Test
    void rejectsInvalidRangesBeforeReading() throws Exception {
        for (boolean encoded : new boolean[] {false, true}) {
            try (NonBlockingReader reader = reader("a", encoded)) {
                char[] buffer = new char[3];
                assertThrows(IndexOutOfBoundsException.class, () -> reader.readBuffered(buffer, -1, 1, 0));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.readBuffered(buffer, 1, -1, 0));
                assertThrows(IndexOutOfBoundsException.class, () -> reader.readBuffered(buffer, 4, 0, 0));
                assertThrows(
                        IndexOutOfBoundsException.class, () -> reader.readBuffered(buffer, 1, Integer.MAX_VALUE, 0));
                assertEquals('a', reader.read());
            }
        }
    }

    @Test
    void peekedCharacterUsesRequestedOffset() throws Exception {
        try (NonBlockingReader reader = reader("a", false)) {
            assertEquals('a', reader.peek(1000));
            char[] buffer = "###".toCharArray();
            assertEquals(1, reader.readBuffered(buffer, 2, 1, 0));
            assertArrayEquals("##a".toCharArray(), buffer);
        }
    }

    @Test
    void peekedEndOfInputIsNotReturnedAsACharacter() throws Exception {
        try (NonBlockingReader reader = reader("", false)) {
            assertEquals(-1, reader.peek(1000));
            char[] buffer = "##".toCharArray();
            assertEquals(-1, reader.readBuffered(buffer, 0, 2, 0));
            assertArrayEquals("##".toCharArray(), buffer);
        }
    }

    private NonBlockingReader reader(String text, boolean encoded) {
        return encoded
                ? NonBlocking.nonBlocking(
                        "test", new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
                : NonBlocking.nonBlocking("test", new StringReader(text));
    }
}
