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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link WCWidth} utility methods: {@code wcwidth},
 * {@code charCountForGraphemeCluster}, {@code charCountForDisplay},
 * and {@code isRegionalIndicator}.
 */
class WCWidthTest {

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

    // --- charCountForGraphemeClusterLegacy (JDK < 21 path) ---

    @Test
    void charCountForGraphemeClusterLegacy_familyEmoji() {
        assertEquals(11, WCWidth.charCountForGraphemeClusterLegacy(FAMILY_EMOJI, 0));
    }

    @Test
    void charCountForGraphemeClusterLegacy_flag() {
        assertEquals(4, WCWidth.charCountForGraphemeClusterLegacy(FLAG_FR, 0));
    }

    @Test
    void charCountForGraphemeClusterLegacy_skinTone() {
        assertEquals(4, WCWidth.charCountForGraphemeClusterLegacy(WAVE_SKIN, 0));
    }

    @Test
    void charCountForGraphemeClusterLegacy_womanScientist() {
        assertEquals(5, WCWidth.charCountForGraphemeClusterLegacy(WOMAN_SCIENTIST, 0));
    }

    @Test
    void charCountForGraphemeClusterLegacy_ascii() {
        assertEquals(1, WCWidth.charCountForGraphemeClusterLegacy("Hello", 0));
        assertEquals(1, WCWidth.charCountForGraphemeClusterLegacy("Hello", 1));
    }

    @Test
    void charCountForGraphemeClusterLegacy_cjk() {
        assertEquals(1, WCWidth.charCountForGraphemeClusterLegacy("中", 0));
    }

    @Test
    void charCountForGraphemeClusterLegacy_variationSelector() {
        assertEquals(2, WCWidth.charCountForGraphemeClusterLegacy(STAR_TEXT, 0));
    }

    @Test
    void charCountForGraphemeClusterLegacy_emptyAtEnd() {
        assertEquals(0, WCWidth.charCountForGraphemeClusterLegacy("A", 1));
    }

    // --- charCountForGraphemeClusterBreakIterator (JDK 21+ path) ---

    @Test
    void charCountForGraphemeClusterBreakIterator_familyEmoji() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(11, WCWidth.charCountForGraphemeClusterBreakIterator(FAMILY_EMOJI, 0));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_flag() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(4, WCWidth.charCountForGraphemeClusterBreakIterator(FLAG_FR, 0));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_skinTone() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(4, WCWidth.charCountForGraphemeClusterBreakIterator(WAVE_SKIN, 0));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_womanScientist() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(5, WCWidth.charCountForGraphemeClusterBreakIterator(WOMAN_SCIENTIST, 0));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_ascii() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(1, WCWidth.charCountForGraphemeClusterBreakIterator("Hello", 0));
        assertEquals(1, WCWidth.charCountForGraphemeClusterBreakIterator("Hello", 1));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_cjk() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(1, WCWidth.charCountForGraphemeClusterBreakIterator("中", 0));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_variationSelector() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(2, WCWidth.charCountForGraphemeClusterBreakIterator(STAR_TEXT, 0));
    }

    @Test
    void charCountForGraphemeClusterBreakIterator_emptyAtEnd() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        assertEquals(0, WCWidth.charCountForGraphemeClusterBreakIterator("A", 1));
    }

    @Test
    void charCountForGraphemeCluster_bothPathsAgree() {
        // On JDK 21+, both implementations should produce the same results
        // for standard emoji sequences
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+");
        for (String emoji : new String[] {FAMILY_EMOJI, FLAG_FR, WAVE_SKIN, WOMAN_SCIENTIST, STAR_TEXT}) {
            assertEquals(
                    WCWidth.charCountForGraphemeClusterLegacy(emoji, 0),
                    WCWidth.charCountForGraphemeClusterBreakIterator(emoji, 0),
                    "Mismatch for: " + emoji);
        }
    }

    // --- wcwidthForGraphemeCluster ---

    @Test
    void wcwidthForGraphemeCluster_singleAsciiChar() {
        // Single ASCII char — no cluster extensions, returns wcwidth('A') = 1
        assertEquals(1, WCWidth.wcwidthForGraphemeCluster("A", 0));
    }

    @Test
    void wcwidthForGraphemeCluster_singleCjk() {
        // Single CJK character — already wide, returns 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster("中", 0));
    }

    @Test
    void wcwidthForGraphemeCluster_vs16UpgradesWidth() {
        // White flag (wcwidth=1) + VS16 → emoji presentation = 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster("\uD83C\uDFF3\uFE0F", 0));
    }

    @Test
    void wcwidthForGraphemeCluster_vs15DowngradesWidth() {
        // Party popper (Emoji_Presentation=Yes, wcwidth=2) + VS15 → text presentation = 1
        assertEquals(1, WCWidth.wcwidthForGraphemeCluster("\uD83C\uDF89\uFE0E", 0));
    }

    @Test
    void wcwidthForGraphemeCluster_zjwWithoutVariationSelector() {
        // Family emoji — base 👨 has wcwidth=2, no VS in cluster, returns 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster(FAMILY_EMOJI, 0));
        // Woman scientist — base 👩 has wcwidth=2, no VS, returns 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster(WOMAN_SCIENTIST, 0));
    }

    @Test
    void wcwidthForGraphemeCluster_flagPair() {
        // Regional indicator pair — base has wcwidth=2, returns 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster(FLAG_FR, 0));
    }

    @Test
    void wcwidthForGraphemeCluster_skinTone() {
        // Waving hand + skin tone modifier — base has wcwidth=2, no VS, returns 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster(WAVE_SKIN, 0));
    }

    @Test
    void wcwidthForGraphemeCluster_rainbowFlag() {
        // White flag + VS16 + ZWJ + rainbow — VS16 found, returns 2
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster("\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08", 0));
    }

    @Test
    void wcwidthForGraphemeCluster_combiningMarkAlone() {
        // Combining acute accent alone — wcwidth=0, no VS, returns 0
        assertEquals(0, WCWidth.wcwidthForGraphemeCluster("\u0301", 0));
    }

    @Test
    void wcwidthForGraphemeCluster_atMiddleOfString() {
        // "A" + white flag + VS16 — cluster at index 1
        String text = "A\uD83C\uDFF3\uFE0F";
        assertEquals(1, WCWidth.wcwidthForGraphemeCluster(text, 0));
        assertEquals(2, WCWidth.wcwidthForGraphemeCluster(text, 1));
    }

    // --- charCountForDisplay ---

    @Test
    void charCountForDisplay_nullTerminal() {
        if (WCWidth.HAS_JDK_GRAPHEME_SUPPORT) {
            // JDK 21+: grapheme cluster segmentation used without terminal
            assertEquals(11, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, null));
        } else {
            // Older JDK: per-codepoint, surrogate pair = 2 chars
            assertEquals(2, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, null));
        }
        // ASCII and CJK unchanged regardless
        assertEquals(1, WCWidth.charCountForDisplay("Hello", 0, null));
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
        try (Terminal t = GraphemeClusterTestTerminal.create()) {
            assertEquals(11, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, t));
            assertEquals(4, WCWidth.charCountForDisplay(FLAG_FR, 0, t));
            assertEquals(4, WCWidth.charCountForDisplay(WAVE_SKIN, 0, t));
            assertEquals(5, WCWidth.charCountForDisplay(WOMAN_SCIENTIST, 0, t));
        }
    }

    @Test
    void charCountForDisplay_withGcMode_asciiUnchanged() throws Exception {
        try (Terminal t = GraphemeClusterTestTerminal.create()) {
            assertEquals(1, WCWidth.charCountForDisplay("Hello", 0, t));
            assertEquals(1, WCWidth.charCountForDisplay("Hello", 1, t));
        }
    }

    @Test
    void charCountForDisplay_withGcMode_cjkUnchanged() throws Exception {
        try (Terminal t = GraphemeClusterTestTerminal.create()) {
            assertEquals(1, WCWidth.charCountForDisplay("中文", 0, t));
        }
    }

    @Test
    void charCountForDisplay_withGcMode_variationSelector() throws Exception {
        try (Terminal t = GraphemeClusterTestTerminal.create()) {
            assertEquals(2, WCWidth.charCountForDisplay(STAR_TEXT, 0, t));
        }
    }

    // --- JDK 21+ grapheme cluster support without terminal ---

    @Test
    void wcwidthForDisplay_nullTerminal_emojiClusters() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+ grapheme support");
        // JDK 21+: grapheme cluster-aware widths without terminal
        assertEquals(2, WCWidth.wcwidthForDisplay(FAMILY_EMOJI, 0, null));
        assertEquals(2, WCWidth.wcwidthForDisplay(FLAG_FR, 0, null));
        assertEquals(2, WCWidth.wcwidthForDisplay(WAVE_SKIN, 0, null));
        assertEquals(2, WCWidth.wcwidthForDisplay(WOMAN_SCIENTIST, 0, null));
    }

    @Test
    void wcwidthForDisplay_nullTerminal_variationSelectors() {
        assumeTrue(WCWidth.HAS_JDK_GRAPHEME_SUPPORT, "Requires JDK 21+ grapheme support");
        // Rainbow flag: VS16 found → width 2
        String rainbowFlag = "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08";
        assertEquals(2, WCWidth.wcwidthForDisplay(rainbowFlag, 0, null));
        // Star + VS15 → text presentation = width 1
        assertEquals(1, WCWidth.wcwidthForDisplay(STAR_TEXT, 0, null));
    }
}
