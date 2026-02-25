/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.GraphemeClusterTestTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WCWidth} utility methods: {@code wcwidth},
 * {@code charCountForGraphemeCluster}, {@code charCountForDisplay},
 * and {@code isRegionalIndicator}.
 */
public class WCWidthTest {

    // Family emoji: 👨‍👩‍👧‍👦 (man ZWJ woman ZWJ girl ZWJ boy)
    private static final String FAMILY_EMOJI = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
    // French flag: 🇫🇷 (regional indicators F + R)
    private static final String FLAG_FR = "\uD83C\uDDEB\uD83C\uDDF7";
    // Waving hand with medium skin tone: 👋🏽
    private static final String WAVE_SKIN = "\uD83D\uDC4B\uD83C\uDFFD";
    // Woman scientist: 👩‍🔬
    private static final String WOMAN_SCIENTIST = "\uD83D\uDC69\u200D\uD83D\uDD2C";
    // Star with text variation selector: ⭐︎
    private static final String STAR_TEXT = "\u2B50\uFE0E";

    // --- wcwidth ---

    @Test
    void wcwidth_ascii() {
        assertEquals(1, WCWidth.wcwidth('A'));
        assertEquals(1, WCWidth.wcwidth('z'));
        assertEquals(1, WCWidth.wcwidth(' '));
    }

    @Test
    void wcwidth_cjk() {
        assertEquals(2, WCWidth.wcwidth('中'));
        assertEquals(2, WCWidth.wcwidth('文'));
        assertEquals(2, WCWidth.wcwidth('日'));
    }

    @Test
    void wcwidth_controlChars() {
        assertEquals(0, WCWidth.wcwidth(0)); // NUL is special-cased to 0
        assertEquals(-1, WCWidth.wcwidth('\n')); // control chars < 32 return -1
        assertEquals(-1, WCWidth.wcwidth(1)); // SOH
        assertEquals(-1, WCWidth.wcwidth(0x7F)); // DEL
    }

    @Test
    void wcwidth_combiningMarks() {
        // Combining acute accent
        assertEquals(0, WCWidth.wcwidth(0x0301));
    }

    @Test
    void wcwidth_zwj() {
        assertEquals(0, WCWidth.wcwidth(0x200D));
    }

    @Test
    void wcwidth_emoji() {
        // Emoji are width 2
        assertEquals(2, WCWidth.wcwidth(0x1F468)); // man
        assertEquals(2, WCWidth.wcwidth(0x1F1EB)); // regional indicator F
    }

    // --- isRegionalIndicator ---

    @Test
    void isRegionalIndicator_valid() {
        assertTrue(WCWidth.isRegionalIndicator(0x1F1E6)); // A
        assertTrue(WCWidth.isRegionalIndicator(0x1F1FF)); // Z
        assertTrue(WCWidth.isRegionalIndicator(0x1F1EB)); // F
    }

    @Test
    void isRegionalIndicator_invalid() {
        assertFalse(WCWidth.isRegionalIndicator('A'));
        assertFalse(WCWidth.isRegionalIndicator(0x1F1E5)); // below range
        assertFalse(WCWidth.isRegionalIndicator(0x1F200)); // above range
    }

    // --- charCountForGraphemeCluster ---

    @Test
    void charCountForGraphemeCluster_familyEmoji() {
        // 4 surrogate pairs + 3 ZWJ = 11 chars
        assertEquals(11, WCWidth.charCountForGraphemeCluster(FAMILY_EMOJI, 0));
    }

    @Test
    void charCountForGraphemeCluster_flag() {
        // 2 surrogate pairs = 4 chars
        assertEquals(4, WCWidth.charCountForGraphemeCluster(FLAG_FR, 0));
    }

    @Test
    void charCountForGraphemeCluster_skinTone() {
        // surrogate pair + surrogate pair modifier = 4 chars
        assertEquals(4, WCWidth.charCountForGraphemeCluster(WAVE_SKIN, 0));
    }

    @Test
    void charCountForGraphemeCluster_womanScientist() {
        // 2 surrogate pairs + 1 ZWJ = 5 chars
        assertEquals(5, WCWidth.charCountForGraphemeCluster(WOMAN_SCIENTIST, 0));
    }

    @Test
    void charCountForGraphemeCluster_ascii() {
        assertEquals(1, WCWidth.charCountForGraphemeCluster("Hello", 0));
        assertEquals(1, WCWidth.charCountForGraphemeCluster("Hello", 1));
    }

    @Test
    void charCountForGraphemeCluster_cjk() {
        assertEquals(1, WCWidth.charCountForGraphemeCluster("中", 0));
    }

    @Test
    void charCountForGraphemeCluster_variationSelector() {
        // Star + variation selector = 2 chars as a cluster
        assertEquals(2, WCWidth.charCountForGraphemeCluster(STAR_TEXT, 0));
    }

    @Test
    void charCountForGraphemeCluster_atMiddleOfString() {
        // "A" + family — starting at index 1 should find the family cluster
        String text = "A" + FAMILY_EMOJI;
        assertEquals(1, WCWidth.charCountForGraphemeCluster(text, 0));
        assertEquals(11, WCWidth.charCountForGraphemeCluster(text, 1));
    }

    @Test
    void charCountForGraphemeCluster_emptyAtEnd() {
        assertEquals(0, WCWidth.charCountForGraphemeCluster("A", 1));
    }

    // --- charCountForDisplay ---

    @Test
    void charCountForDisplay_nullTerminal_returnsSingleCodepointSize() {
        // Surrogate pair = 2 chars for the first codepoint
        assertEquals(2, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, null));
        // ASCII
        assertEquals(1, WCWidth.charCountForDisplay("Hello", 0, null));
        // CJK BMP
        assertEquals(1, WCWidth.charCountForDisplay("中", 0, null));
    }

    @Test
    void charCountForDisplay_nullTerminal_asciiAndCjk() {
        assertEquals(1, WCWidth.charCountForDisplay("AB", 0, null));
        assertEquals(1, WCWidth.charCountForDisplay("AB", 1, null));
        assertEquals(1, WCWidth.charCountForDisplay("中文", 0, null));
    }

    @Test
    void charCountForDisplay_withGcMode_returnsClusterSize() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            assertEquals(11, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, t));
            assertEquals(4, WCWidth.charCountForDisplay(FLAG_FR, 0, t));
            assertEquals(4, WCWidth.charCountForDisplay(WAVE_SKIN, 0, t));
            assertEquals(5, WCWidth.charCountForDisplay(WOMAN_SCIENTIST, 0, t));
        } finally {
            t.close();
        }
    }

    @Test
    void charCountForDisplay_withGcMode_asciiUnchanged() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            assertEquals(1, WCWidth.charCountForDisplay("Hello", 0, t));
            assertEquals(1, WCWidth.charCountForDisplay("Hello", 1, t));
        } finally {
            t.close();
        }
    }

    @Test
    void charCountForDisplay_withGcMode_cjkUnchanged() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            assertEquals(1, WCWidth.charCountForDisplay("中文", 0, t));
        } finally {
            t.close();
        }
    }

    @Test
    void charCountForDisplay_withGcMode_variationSelector() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            assertEquals(2, WCWidth.charCountForDisplay(STAR_TEXT, 0, t));
        } finally {
            t.close();
        }
    }
}
