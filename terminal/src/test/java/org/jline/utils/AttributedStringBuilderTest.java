/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AttributedStringBuilderTest {
    private static String TAB_SIZE_ERR_MSG = "Incorrect tab size";

    /**
     * Test single line with tabs in
     */
    @Test
    public void testTabSize() {
        AttributedStringBuilder sb;
        sb = new AttributedStringBuilder().tabs(4);
        sb.append("hello\tWorld");
        assertEquals("hello   World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(5);
        sb.append("hello\tWorld");
        assertEquals("hello     World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(Arrays.asList(5));
        sb.append("hello\tWorld");
        assertEquals("hello     World", sb.toString(), TAB_SIZE_ERR_MSG);

        sb = new AttributedStringBuilder().tabs(Arrays.asList(6, 13));
        sb.append("one\ttwo\tthree\tfour");
        assertEquals("one   two    three  four", sb.toString(), TAB_SIZE_ERR_MSG);
    }

    /**
     * Test multiple lines with tabs in
     */
    @Test
    public void testSplitLineTabSize() {
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
    public void testAppendToString() {
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
    public void testAppendNullToString() {
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
    public void testFromAnsiWithTabs() {
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
    public void testUnsetTabSize() {
        AttributedStringBuilder sb;
        String expected = "";
        sb = new AttributedStringBuilder();

        sb.append("hello\tWorld");
        expected += "hello\tWorld";
        assertEquals(expected, sb.toString(), TAB_SIZE_ERR_MSG);
    }

    @Test
    public void testChangingExistingTabSize() throws Exception {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("helloWorld");
        assertThrows(IllegalStateException.class, () -> sb.tabs(4));
    }

    @Test
    public void testNegativeTabSize() throws Exception {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        assertThrows(IllegalArgumentException.class, () -> sb.tabs(-1));
    }
}
