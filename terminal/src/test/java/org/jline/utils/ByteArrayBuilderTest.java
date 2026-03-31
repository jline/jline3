/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteArrayBuilderTest {

    @Test
    void testAppendAsciiChar() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendAscii('A').appendAscii('B').appendAscii('C');
        assertEquals("ABC", buf.toStringUtf8());
    }

    @Test
    void testAppendAsciiString() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendAscii("hello world");
        assertEquals("hello world", buf.toStringUtf8());
    }

    @Test
    void testCsi() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.csi().appendAscii("0m");
        assertEquals("\033[0m", buf.toStringUtf8());
    }

    @Test
    void testAppendIntSmallValues() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendInt(0);
        assertEquals("0", buf.toStringUtf8());

        buf.reset();
        buf.appendInt(5);
        assertEquals("5", buf.toStringUtf8());

        buf.reset();
        buf.appendInt(42);
        assertEquals("42", buf.toStringUtf8());

        buf.reset();
        buf.appendInt(255);
        assertEquals("255", buf.toStringUtf8());
    }

    @Test
    void testAppendIntLargeValues() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendInt(1000);
        assertEquals("1000", buf.toStringUtf8());

        buf.reset();
        buf.appendInt(65535);
        assertEquals("65535", buf.toStringUtf8());
    }

    @Test
    void testAppendIntNegative() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendInt(-1);
        assertEquals("-1", buf.toStringUtf8());

        buf.reset();
        buf.appendInt(-128);
        assertEquals("-128", buf.toStringUtf8());
    }

    @Test
    void testAppendIntMinValue() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendInt(Integer.MIN_VALUE);
        assertEquals(Integer.toString(Integer.MIN_VALUE), buf.toStringUtf8());
    }

    @Test
    void testAppendUtf8Ascii() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendUtf8('A');
        assertArrayEquals(new byte[] {'A'}, buf.toByteArray());
    }

    @Test
    void testAppendUtf8TwoByte() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendUtf8('\u00E9'); // é
        assertEquals("é", buf.toStringUtf8());
        assertArrayEquals("é".getBytes(StandardCharsets.UTF_8), buf.toByteArray());
    }

    @Test
    void testAppendUtf8ThreeByte() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendUtf8('\u4E16'); // 世
        assertEquals("世", buf.toStringUtf8());
        assertArrayEquals("世".getBytes(StandardCharsets.UTF_8), buf.toByteArray());
    }

    @Test
    void testAppendUtf8SupplementaryCodePoint() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        int cp = 0x1F600; // 😀
        buf.appendUtf8(cp);
        String expected = new String(Character.toChars(cp));
        assertEquals(expected, buf.toStringUtf8());
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), buf.toByteArray());
    }

    @Test
    void testAnsiColorSequence() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        // Build: \033[38;2;128;64;255m
        buf.csi()
                .appendInt(38)
                .appendAscii(";2;")
                .appendInt(128)
                .appendAscii(';')
                .appendInt(64)
                .appendAscii(';')
                .appendInt(255)
                .appendAscii('m');
        assertEquals("\033[38;2;128;64;255m", buf.toStringUtf8());
    }

    @Test
    void testReset() {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendAscii("first");
        assertEquals(5, buf.length());
        buf.reset();
        assertEquals(0, buf.length());
        buf.appendAscii("second");
        assertEquals("second", buf.toStringUtf8());
    }

    @Test
    void testWriteTo() throws IOException {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        buf.appendAscii("test");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        buf.writeTo(out);
        assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), out.toByteArray());
    }

    @Test
    void testGrowth() {
        ByteArrayBuilder buf = new ByteArrayBuilder(4);
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buf.appendAscii('x');
            expected.append('x');
        }
        assertEquals(1000, buf.length());
        assertEquals(expected.toString(), buf.toStringUtf8());
    }

    @Test
    void testAsAsciiAppendable() throws IOException {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        Appendable app = buf.asAsciiAppendable();
        app.append('A');
        app.append("BCD");
        app.append("xyzzy", 1, 4);
        assertEquals("ABCDyzz", buf.toStringUtf8());
    }

    @Test
    void testToAnsiBytesParity() {
        // Verify that toAnsiBytes produces the same output as toAnsi
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(128, 64, 255));
        asb.append("colored");
        asb.style(AttributedStyle.DEFAULT.bold());
        asb.append(" bold");
        asb.style(AttributedStyle.DEFAULT);
        asb.append(" plain");
        AttributedString str = asb.toAttributedString();

        String ansiString = str.toAnsi(AttributedCharSequence.TRUE_COLORS, AttributedCharSequence.ForceMode.None);

        ByteArrayBuilder buf = new ByteArrayBuilder();
        str.toAnsiBytes(
                buf, AttributedCharSequence.TRUE_COLORS, AttributedCharSequence.ForceMode.None, null, null, null);
        String bytesAsString = buf.toStringUtf8();

        assertEquals(ansiString, bytesAsString);
    }

    @Test
    void testToAnsiBytesParityWithIndexedColors() {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        asb.append("red");
        asb.style(AttributedStyle.DEFAULT.background(AttributedStyle.BLUE));
        asb.append("blue-bg");
        asb.style(AttributedStyle.DEFAULT.italic());
        asb.append("italic");
        asb.style(AttributedStyle.DEFAULT);
        AttributedString str = asb.toAttributedString();

        String ansiString = str.toAnsi(256, AttributedCharSequence.ForceMode.None);

        ByteArrayBuilder buf = new ByteArrayBuilder();
        str.toAnsiBytes(buf, 256, AttributedCharSequence.ForceMode.None, null, null, null);

        assertEquals(ansiString, buf.toStringUtf8());
    }

    @Test
    void testToAnsiBytesWithMultiByteChars() {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append("hello 世界");
        asb.style(AttributedStyle.DEFAULT);
        AttributedString str = asb.toAttributedString();

        String ansiString = str.toAnsi(256, AttributedCharSequence.ForceMode.None);

        ByteArrayBuilder buf = new ByteArrayBuilder();
        str.toAnsiBytes(buf, 256, AttributedCharSequence.ForceMode.None, null, null, null);

        assertEquals(ansiString, buf.toStringUtf8());
    }
}
