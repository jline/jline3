/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributedStringBuilderTest {
    private static final String TAB_SIZE_ERR_MSG = "Incorrect tab size";

    /**
     * Test single line with tabs in
     */
    @Test
    void testTabSize() {
        AttributedStringBuilder sb;
        sb = new AttributedStringBuilder().tabs(4);
        sb.append("hello\tWorld");
        assertEquals("hello   World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(5);
        sb.append("hello\tWorld");
        assertEquals("hello     World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(List.of(5));
        sb.append("hello\tWorld");
        assertEquals("hello     World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(List.of(6, 13));
        sb.append("one\ttwo\tthree\tfour");
        assertEquals("one   two    three  four", sb.toString(), TAB_SIZE_ERR_MSG);
    }

    /**
     * Test multiple lines with tabs in
     */
    @Test
    void testSplitLineTabSize() {
        AttributedStringBuilder sb;
        sb = new AttributedStringBuilder().tabs(4);
        sb.append("hello\n\tWorld");
        assertEquals("hello\n    World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(4);
        sb.append("hello\tWorld\n\tfoo\tbar");
        assertEquals("hello   World\n    foo bar", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(5);
        sb.append("hello\n\tWorld");
        assertEquals("hello\n     World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(5);
        sb.append("hello\tWorld\n\tfoo\tbar");
        assertEquals("hello     World\n     foo  bar", sb.toString(), TAB_SIZE_ERR_MSG);
    }

    @Test
    void testAppendToString() {
        AttributedStringBuilder sb;
        String expected = "";
        sb = new AttributedStringBuilder().tabs(4);

        sb.append("hello");
        expected += "hello";
        sb.append("\tWorld");
        expected += "   World"; // append to first line
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);

        sb.append("\nfoo\tbar");
        expected += "\nfoo bar"; // append new line
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);

        sb.append("lorem\tipsum");
        expected += "lorem    ipsum"; // append to second line
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);
    }

    /**
     * Test that methods overriding {@code Appendable.append} correctly handle
     * {@code null -> "null"}.
     */
    @Test
    void testAppendNullToString() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        String expected = "";

        sb.append("foo");
        expected += "foo";
        sb.append((CharSequence) null);
        expected += "null";
        assertEquals(expected, sb.toString());

        sb.append("bar");
        expected += "bar";
        sb.append((CharSequence) null, 1, 3);
        expected += "ul"; // Indices apply to "null"
        assertEquals(expected, sb.toString());
    }

    @Test
    void testFromAnsiWithTabs() {
        AttributedStringBuilder sb;
        String expected = "";
        sb = new AttributedStringBuilder().tabs(4);

        sb.appendAnsi("hello\tWorld");
        expected += "hello   World";
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);

        sb.appendAnsi("\033[38;5;120mgreen\tfoo\033[39m");
        expected += "green  foo";
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);
        sb.appendAnsi("\n\033[38;5;120mbar\tbaz\033[39m");
        expected += "\nbar baz";
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);
    }

    /**
     * Test that tabs are not expanded in strings if tab size has not been set
     */
    @Test
    void testUnsetTabSize() {
        AttributedStringBuilder sb;
        String expected = "";
        sb = new AttributedStringBuilder();

        sb.append("hello\tWorld");
        expected += "hello\tWorld";
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);
    }

    @Test
    void testChangingExistingTabSize() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("helloWorld");
        assertThrows(IllegalStateException.class, () -> sb.tabs(4));
    }

    @Test
    void testNegativeTabSize() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        assertThrows(IllegalArgumentException.class, () -> sb.tabs(-1));
    }

    @Test
    void testAppendCodePointBmp() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.appendCodePoint('A');
        sb.appendCodePoint(0x00E9); // é
        sb.appendCodePoint('Z');
        assertEquals("AéZ", sb.toString());
    }

    @Test
    void testAppendCodePointSupplementary() {
        // U+1F600 = 😀 (GRINNING FACE) — a supplementary code point
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("hi ");
        sb.appendCodePoint(0x1F600);
        sb.append(" bye");
        String result = sb.toString();
        assertEquals("hi 😀 bye", result);
        // Verify the code point round-trips correctly
        assertEquals(0x1F600, result.codePointAt(3));
    }

    @Test
    void testAppendCodePointPreservesStyle() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        sb.appendCodePoint(0x1F600); // supplementary
        sb.style(AttributedStyle.DEFAULT);
        sb.appendCodePoint('!');
        AttributedString str = sb.toAttributedString();
        // Both surrogate chars of the supplementary code point should carry the red style
        long style0 = str.styleCodeAt(0);
        long style1 = str.styleCodeAt(1);
        long styleExcl = str.styleCodeAt(2);
        assertEquals(style0, style1, "Both surrogates of a supplementary code point should have the same style");
        assertNotEquals(style0, styleExcl, "Style should change after the supplementary code point");
    }

    @Test
    void testAppendCodePointInvalidThrows() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        assertThrows(IllegalArgumentException.class, () -> sb.appendCodePoint(0x110000));
        assertThrows(IllegalArgumentException.class, () -> sb.appendCodePoint(-1));
    }

    @Test
    void testAppendCodePointTab() {
        // BMP tab character should still be expanded when tab stops are set
        AttributedStringBuilder sb = new AttributedStringBuilder().tabs(4);
        sb.append("ab");
        sb.appendCodePoint('\t');
        sb.append("c");
        assertEquals("ab  c", sb.toString());
    }

    @Test
    void styleMatchesGuardsAgainstCatastrophicBacktracking() {
        // (.*a){30} over a run of 'a's forces exponential backtracking in
        // java.util.regex (still true on current JVMs). styleMatches feeds
        // arbitrary text through this matcher when highlighting, so without a
        // deadline the call runs for tens of seconds. SafeRegex caps it at ~1.5s.
        Pattern evil = Pattern.compile("(.*a){30}");
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append('a');
        }
        assertTimeoutPreemptively(
                Duration.ofSeconds(8),
                () -> sb.styleMatches(evil, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)));

        // Non-pathological matching still applies the style and leaves text intact.
        AttributedStringBuilder ok = new AttributedStringBuilder();
        ok.append("Error: boom");
        ok.styleMatches(Pattern.compile("Error"), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        assertEquals("Error: boom", ok.toString());
        assertTrue(ok.toAnsi().indexOf('\u001b') >= 0, "expected the matched region to be styled");
    }

    @Test
    void styleMatchesGroupsGuardsAgainstCatastrophicBacktracking() {
        // Same evil pattern as above, driven through the multi-group
        // styleMatches(Pattern, List<AttributedStyle>) overload.
        Pattern evil = Pattern.compile("(.*a){30}");
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append('a');
        }
        assertTimeoutPreemptively(
                Duration.ofSeconds(8),
                () -> sb.styleMatches(evil, List.of(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))));

        // Non-pathological matching still styles each capture group.
        AttributedStringBuilder ok = new AttributedStringBuilder();
        ok.append("Error: boom");
        ok.styleMatches(
                Pattern.compile("(Error): (boom)"),
                List.of(
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.RED),
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)));
        assertEquals("Error: boom", ok.toString());
        assertTrue(ok.toAnsi().indexOf('\u001b') >= 0, "expected the matched groups to be styled");
    }
}
