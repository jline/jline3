/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultHighlighter}.
 */
public class DefaultHighlighterTest {

    @Test
    public void escapeControlCharsRendersCaretNotationAndDropsDelAndC1() {
        AttributedString result =
                DefaultHighlighter.escapeControlChars(new AttributedString("a\u001b]0;pwned\u0007b\u007fc\u009b2Jd"));
        assertEquals("a^[]0;pwned^Gbc2Jd", result.toString());
    }

    @Test
    public void escapeControlCharsKeepsTabAndLineFeed() {
        AttributedString result = DefaultHighlighter.escapeControlChars(new AttributedString("a\tb\nc\u0001"));
        assertEquals("a\tb\nc^A", result.toString());
    }

    @Test
    public void escapeControlCharsKeepsStyles() {
        AttributedString str = new AttributedStringBuilder()
                .styled(AttributedStyle.BOLD, "cmd")
                .append(" ")
                .styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED), "x\u001by")
                .toAttributedString();
        AttributedString result = DefaultHighlighter.escapeControlChars(str);
        assertEquals("cmd x^[y", result.toString());
        assertEquals(AttributedStyle.BOLD, result.styleAt(0));
        assertEquals(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED), result.styleAt(4));
        assertTrue(result.styleAt(5).getStyle() != 0, "the caret notation keeps the surrounding style");
        assertEquals(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED), result.styleAt(7));
    }

    @Test
    public void escapeControlCharsReturnsInputWithoutControls() {
        AttributedString str = new AttributedString("plain text");
        assertSame(str, DefaultHighlighter.escapeControlChars(str));
    }
}
