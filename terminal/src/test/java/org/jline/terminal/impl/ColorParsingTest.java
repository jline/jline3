/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.util.stream.Stream;

import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorParsingTest {

    static Stream<Arguments> eofScenarios() {
        return Stream.of(
                Arguments.of("\033]10;rgb:ff/00", 10, "EOF during RGB values"),
                Arguments.of("", 10, "immediate EOF"),
                Arguments.of("\033]10;rg", 10, "EOF during rgb: prefix"));
    }

    @ParameterizedTest(name = "parseColorResponse returns -1 on {2}")
    @MethodSource("eofScenarios")
    void testParseColorResponseReturnsMinusOneOnEof(String input, int colorType, String scenario) throws Exception {
        NonBlockingReader reader = createReader(input);
        int result = ColorSupport.parseColorResponse(reader, colorType);
        assertEquals(-1, result, "parseColorResponse should return -1 on " + scenario);
    }

    @Test
    void testParseColorResponseWithValidBellTerminator() throws Exception {
        NonBlockingReader reader = createReader("\033]10;rgb:ff/ff/ff\007");
        int result = ColorSupport.parseColorResponse(reader, 10);
        assertEquals(0xFFFFFF, result, "parseColorResponse should parse white color correctly");
    }

    @Test
    void testParseColorResponseWithValidStTerminator() throws Exception {
        NonBlockingReader reader = createReader("\033]11;rgb:00/00/00\033\\");
        int result = ColorSupport.parseColorResponse(reader, 11);
        assertEquals(0x000000, result, "parseColorResponse should parse black color correctly");
    }

    /**
     * Creates a NonBlockingReader that returns characters from the given string,
     * then returns -1 (EOF) for all subsequent reads. This avoids the ClosedException
     * that NonBlockingPumpReader throws when its writer is closed.
     */
    private NonBlockingReader createReader(String data) {
        return new NonBlockingReader() {
            private final char[] chars = data.toCharArray();
            private int pos = 0;

            @Override
            protected int read(long timeout, boolean isPeek) {
                if (pos >= chars.length) {
                    return -1;
                }
                if (isPeek) {
                    return chars[pos];
                }
                return chars[pos++];
            }

            @Override
            public int readBuffered(char[] b, int off, int len, long timeout) {
                if (pos >= chars.length) {
                    return -1;
                }
                int count = Math.min(len, chars.length - pos);
                System.arraycopy(chars, pos, b, off, count);
                pos += count;
                return count;
            }
        };
    }
}
