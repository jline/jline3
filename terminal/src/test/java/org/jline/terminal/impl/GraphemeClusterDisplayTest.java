/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.WCWidth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for grapheme-cluster-aware display methods:
 * {@link WCWidth#charCountForDisplay},
 * {@link org.jline.utils.AttributedCharSequence#columnLength(Terminal)},
 * {@link org.jline.utils.AttributedCharSequence#columnSubSequence(int, int, Terminal)},
 * and {@link org.jline.utils.AttributedCharSequence#columnSplitLength(int, boolean, boolean, Terminal)}.
 */
public class GraphemeClusterDisplayTest {

    // Family emoji: 👨‍👩‍👧‍👦 (man ZWJ woman ZWJ girl ZWJ boy) — 11 chars, 4 base codepoints
    private static final String FAMILY_EMOJI = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
    // French flag: 🇫🇷 (regional indicators F + R) — 4 chars
    private static final String FLAG_FR = "\uD83C\uDDEB\uD83C\uDDF7";
    // Waving hand with medium skin tone: 👋🏽 — 4 chars
    private static final String WAVE_SKIN = "\uD83D\uDC4B\uD83C\uDFFD";
    // Woman scientist: 👩‍🔬 — 5 chars
    private static final String WOMAN_SCIENTIST = "\uD83D\uDC69\u200D\uD83D\uDD2C";
    // Variation selector: star with text presentation ⭐︎ — 2 chars
    private static final String STAR_TEXT = "\u2B50\uFE0E";

    private LineDisciplineTerminal gcTerminal;

    @BeforeEach
    void setUp() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gcTerminal = new LineDisciplineTerminal("test", "xterm-256color", out, StandardCharsets.UTF_8);
        // Feed probe response indicating support
        gcTerminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
        gcTerminal.slaveInputPipe.flush();
        assertTrue(gcTerminal.setGraphemeClusterMode(true));
    }

    @AfterEach
    void tearDown() throws Exception {
        gcTerminal.close();
    }

    // --- WCWidth.charCountForDisplay ---

    @Test
    void charCountForDisplay_withGcMode_returnsClusterSize() {
        assertEquals(11, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, gcTerminal));
        assertEquals(4, WCWidth.charCountForDisplay(FLAG_FR, 0, gcTerminal));
        assertEquals(4, WCWidth.charCountForDisplay(WAVE_SKIN, 0, gcTerminal));
        assertEquals(5, WCWidth.charCountForDisplay(WOMAN_SCIENTIST, 0, gcTerminal));
    }

    @Test
    void charCountForDisplay_withoutGcMode_returnsSingleCodepointSize() {
        // null terminal = no gc mode
        // Family emoji starts with U+1F468 (surrogate pair = 2 chars)
        assertEquals(2, WCWidth.charCountForDisplay(FAMILY_EMOJI, 0, (Terminal) null));
        // ASCII
        assertEquals(1, WCWidth.charCountForDisplay("Hello", 0, (Terminal) null));
        // CJK BMP
        assertEquals(1, WCWidth.charCountForDisplay("中", 0, (Terminal) null));
    }

    @Test
    void charCountForDisplay_asciiUnchangedByGcMode() {
        assertEquals(1, WCWidth.charCountForDisplay("Hello", 0, gcTerminal));
        assertEquals(1, WCWidth.charCountForDisplay("Hello", 1, gcTerminal));
    }

    @Test
    void charCountForDisplay_cjkUnchangedByGcMode() {
        assertEquals(1, WCWidth.charCountForDisplay("中文", 0, gcTerminal));
    }

    @Test
    void charCountForDisplay_variationSelector() {
        // Star + variation selector = 2 chars as a cluster
        assertEquals(2, WCWidth.charCountForDisplay(STAR_TEXT, 0, gcTerminal));
        // Without gc mode, just the star (1 BMP char)
        assertEquals(1, WCWidth.charCountForDisplay(STAR_TEXT, 0, (Terminal) null));
    }

    // --- columnLength(Terminal) ---

    @Test
    void columnLength_withGcMode_treatsClustersAsSingleUnit() {
        // Family emoji: as one cluster the terminal renders it as width=2
        AttributedString family = new AttributedString(FAMILY_EMOJI);
        assertEquals(2, family.columnLength(gcTerminal));

        // Flag: as one cluster = width 2
        AttributedString flag = new AttributedString(FLAG_FR);
        assertEquals(2, flag.columnLength(gcTerminal));

        // Skin tone: base + modifier, as one cluster = width 2
        AttributedString wave = new AttributedString(WAVE_SKIN);
        assertEquals(2, wave.columnLength(gcTerminal));
    }

    @Test
    void columnLength_withoutGcMode_sumsIndividualCodepoints() {
        // Family emoji: 4 base emoji width=2 each + 3 ZWJ width=0 = 8
        AttributedString family = new AttributedString(FAMILY_EMOJI);
        assertEquals(8, family.columnLength(null));
        assertEquals(8, family.columnLength()); // no-arg delegates to null

        // Flag: 2 regional indicators width=2 each = 4
        AttributedString flag = new AttributedString(FLAG_FR);
        assertEquals(4, flag.columnLength(null));
    }

    @Test
    void columnLength_mixedContent() {
        // "Hi " (3) + family (2 in gc) + " end" (4) = 9
        AttributedString mixed = new AttributedString("Hi " + FAMILY_EMOJI + " end");
        assertEquals(9, mixed.columnLength(gcTerminal));

        // Without gc: 3 + 8 + 4 = 15
        assertEquals(15, mixed.columnLength(null));
    }

    @Test
    void columnLength_multipleEmoji() {
        // Two flags side by side
        AttributedString twoFlags = new AttributedString(FLAG_FR + FLAG_FR);
        assertEquals(4, twoFlags.columnLength(gcTerminal)); // 2 + 2
        assertEquals(8, twoFlags.columnLength(null)); // 4 + 4 (individual RI widths)
    }

    @Test
    void columnLength_cjkUnchanged() {
        AttributedString cjk = new AttributedString("中文");
        assertEquals(4, cjk.columnLength(gcTerminal));
        assertEquals(4, cjk.columnLength(null));
    }

    // --- columnSubSequence(start, stop, Terminal) ---

    @Test
    void columnSubSequence_gcMode_doesNotSplitCluster() {
        // "AB" + family + "CD"
        String text = "AB" + FAMILY_EMOJI + "CD";
        AttributedString as = new AttributedString(text);
        // gc mode: A(1) B(1) family(2) C(1) D(1) = columns 0..6

        // Extract just the family emoji (columns 2..4)
        AttributedString sub = as.columnSubSequence(2, 4, gcTerminal);
        assertEquals(FAMILY_EMOJI, sub.toString());

        // Extract A and B (columns 0..2)
        AttributedString ab = as.columnSubSequence(0, 2, gcTerminal);
        assertEquals("AB", ab.toString());

        // Extract C and D (columns 4..6)
        AttributedString cd = as.columnSubSequence(4, 6, gcTerminal);
        assertEquals("CD", cd.toString());
    }

    @Test
    void columnSubSequence_withoutGcMode_splitsClusterCodepoints() {
        // Without gc mode, family emoji is 8 columns wide (per-codepoint)
        String text = "A" + FAMILY_EMOJI + "B";
        AttributedString as = new AttributedString(text);

        // Just A (column 0..1)
        assertEquals("A", as.columnSubSequence(0, 1, null).toString());
    }

    @Test
    void columnSubSequence_multipleEmoji() {
        // flag1 + flag2 with gc mode
        String text = FLAG_FR + FLAG_FR;
        AttributedString as = new AttributedString(text);
        // gc: flag1(2) + flag2(2) = 4 columns

        // First flag (columns 0..2)
        assertEquals(FLAG_FR, as.columnSubSequence(0, 2, gcTerminal).toString());
        // Second flag (columns 2..4)
        assertEquals(FLAG_FR, as.columnSubSequence(2, 4, gcTerminal).toString());
    }

    @Test
    void columnSubSequence_noArgDelegatesToNull() {
        AttributedString as = new AttributedString("Hello");
        assertEquals(
                as.columnSubSequence(1, 3, null).toString(),
                as.columnSubSequence(1, 3).toString());
    }

    // --- columnSplitLength(..., Terminal) ---

    @Test
    void columnSplitLength_gcMode_keepsClusterTogether() {
        // "AB" + family + "CD" = 6 columns in gc mode
        String text = "AB" + FAMILY_EMOJI + "CD";
        AttributedString as = new AttributedString(text);

        // Split at 4 columns: "AB" + family fits in 4, then "CD"
        List<AttributedString> lines = as.columnSplitLength(4, false, true, gcTerminal);
        assertEquals(2, lines.size());
        assertEquals("AB" + FAMILY_EMOJI, lines.get(0).toString());
        assertEquals("CD", lines.get(1).toString());
    }

    @Test
    void columnSplitLength_gcMode_wrapsWhenClusterDoesNotFit() {
        // family(2) + family(2) + family(2) = 6 columns in gc mode
        String text = FAMILY_EMOJI + FAMILY_EMOJI + FAMILY_EMOJI;
        AttributedString as = new AttributedString(text);

        // Split at 5 columns: first two fit (4), third wraps
        List<AttributedString> lines = as.columnSplitLength(5, false, true, gcTerminal);
        assertEquals(2, lines.size());
        assertEquals(FAMILY_EMOJI + FAMILY_EMOJI, lines.get(0).toString());
        assertEquals(FAMILY_EMOJI, lines.get(1).toString());
    }

    @Test
    void columnSplitLength_withNewlines() {
        String text = "AB\nCD";
        AttributedString as = new AttributedString(text);

        List<AttributedString> lines = as.columnSplitLength(80, false, true, gcTerminal);
        assertEquals(2, lines.size());
        assertEquals("AB", lines.get(0).toString());
        assertEquals("CD", lines.get(1).toString());
    }

    @Test
    void columnSplitLength_noArgDelegatesToNull() {
        AttributedString as = new AttributedString("Hello World");
        List<AttributedString> a = as.columnSplitLength(5, false, true, null);
        List<AttributedString> b = as.columnSplitLength(5, false, true);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).toString(), b.get(i).toString());
        }
    }
}
