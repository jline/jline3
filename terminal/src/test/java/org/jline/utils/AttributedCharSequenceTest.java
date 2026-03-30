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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.jline.terminal.impl.GraphemeClusterTestTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributedCharSequenceTest {

    // Family emoji: 👨‍👩‍👧‍👦 (man ZWJ woman ZWJ girl ZWJ boy)
    private static final String FAMILY_EMOJI = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
    // French flag: 🇫🇷 (regional indicators F + R)
    private static final String FLAG_FR = "\uD83C\uDDEB\uD83C\uDDF7";
    // Waving hand with medium skin tone: 👋🏽
    private static final String WAVE_SKIN = "\uD83D\uDC4B\uD83C\uDFFD";
    // Woman scientist: 👩‍🔬
    private static final String WOMAN_SCIENTIST = "\uD83D\uDC69\u200D\uD83D\uDD2C";
    // Rainbow flag: 🏳️‍🌈 (white flag + VS16 + ZWJ + rainbow)
    private static final String RAINBOW_FLAG = "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08";
    // White flag + VS16: 🏳️
    private static final String WHITE_FLAG_VS16 = "\uD83C\uDFF3\uFE0F";
    // White flag (no VS): 🏳
    private static final String WHITE_FLAG = "\uD83C\uDFF3";
    // Party popper: 🎉 (Emoji_Presentation=Yes)
    private static final String PARTY_POPPER = "\uD83C\uDF89";
    // Party popper + VS15 (text presentation): 🎉︎
    private static final String PARTY_POPPER_VS15 = "\uD83C\uDF89\uFE0E";
    // German flag: 🇩🇪 (regional indicators D + E)
    private static final String FLAG_DE = "\uD83C\uDDE9\uD83C\uDDEA";

    @Test
    public void testGraphemeClusterColumnLength() {
        // Test per-codepoint widths without grapheme cluster mode.

        // Family emoji: per-codepoint = 2+0+2+0+2+0+2 = 8
        AttributedString family = new AttributedString(FAMILY_EMOJI);
        assertEquals(8, family.columnLength());

        // Flag: per-codepoint = 2+2 = 4
        AttributedString flag = new AttributedString(FLAG_FR);
        assertEquals(4, flag.columnLength());

        // Skin tone: per-codepoint = 2+0 = 2
        AttributedString wave = new AttributedString(WAVE_SKIN);
        assertEquals(2, wave.columnLength());

        // Woman scientist: per-codepoint = 2+0+2 = 4
        AttributedString scientist = new AttributedString(WOMAN_SCIENTIST);
        assertEquals(4, scientist.columnLength());

        // Mixed: "Hi " + family + " end"
        AttributedString mixed = new AttributedString("Hi " + FAMILY_EMOJI + " end");
        assertEquals(3 + 8 + 4, mixed.columnLength());
    }

    @Test
    public void testUnderline() throws IOException {
        AttributedString as = AttributedString.fromAnsi("\33[38;5;0m\33[48;5;15mtest\33[0m");
        assertEquals(as.toAnsi(256, AttributedCharSequence.ForceMode.Force256Colors), "\33[38;5;0;48;5;15mtest\33[0m");
    }

    @Test
    public void testBoldOnWindows() throws IOException {
        String HIC = "\33[36;1m";
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.appendAnsi(HIC);
        sb.append("the buffer");
        // sb.append(NOR);
        AttributedString as = sb.toAttributedString();

        assertEquals("\33[36;1mthe buffer\33[0m", as.toAnsi(null));
    }

    @Test
    public void testBold() throws IOException {
        ExternalTerminal terminal = new ExternalTerminal(
                "my term",
                "windows",
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);

        assertEquals(
                "\33[34;47;1mblue on white\33[31;42;1mred on green\33[0m",
                AttributedString.fromAnsi(
                                "\33[34m\33[47m\33[1mblue on white\33[0m\33[31m\33[42m\33[1mred on green\33[0m")
                        .toAnsi(terminal));

        assertEquals(
                "\33[32;1mtest\33[0m",
                AttributedString.fromAnsi("\33[32m\33[1mtest\33[0m").toAnsi(terminal));
        assertEquals(
                "\33[32;1mtest\33[0m",
                AttributedString.fromAnsi("\33[1m\33[32mtest\33[0m").toAnsi(terminal));
    }

    @Test
    public void testRoundTrip() throws IOException {
        ExternalTerminal terminal = new ExternalTerminal(
                "my term",
                "xterm",
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8);

        AttributedString org = new AttributedStringBuilder().append("─").toAttributedString();

        AttributedString rndTrip = AttributedString.fromAnsi(org.toAnsi(terminal), terminal);

        assertEquals(org, rndTrip);
    }

    // --- grapheme cluster mode tests (columnLength, columnSubSequence, columnSplitLength) ---

    @Test
    public void testColumnLengthWithGcMode() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            // Family emoji: as one cluster = width 2
            assertEquals(2, new AttributedString(FAMILY_EMOJI).columnLength(t));

            // Flag: as one cluster = width 2
            assertEquals(2, new AttributedString(FLAG_FR).columnLength(t));

            // Skin tone: base + modifier = width 2
            assertEquals(2, new AttributedString(WAVE_SKIN).columnLength(t));

            // Mixed: "Hi " (3) + family (2) + " end" (4) = 9
            assertEquals(9, new AttributedString("Hi " + FAMILY_EMOJI + " end").columnLength(t));

            // Two flags: 2 + 2 = 4
            assertEquals(4, new AttributedString(FLAG_FR + FLAG_FR).columnLength(t));

            // CJK unchanged
            assertEquals(4, new AttributedString("中文").columnLength(t));
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnLengthVS16WithGcMode() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            // Rainbow flag: VS16 upgrades white flag to emoji presentation → width 2
            assertEquals(2, new AttributedString(RAINBOW_FLAG).columnLength(t));

            // White flag + VS16: emoji presentation → width 2
            assertEquals(2, new AttributedString(WHITE_FLAG_VS16).columnLength(t));

            // White flag without VS16: text presentation → width 1
            assertEquals(1, new AttributedString(WHITE_FLAG).columnLength(t));

            // Party popper: Emoji_Presentation=Yes → width 2
            assertEquals(2, new AttributedString(PARTY_POPPER).columnLength(t));

            // Party popper + VS15: text presentation downgrades → width 1
            assertEquals(1, new AttributedString(PARTY_POPPER_VS15).columnLength(t));

            // Flag DE: regional indicator pair → width 2
            assertEquals(2, new AttributedString(FLAG_DE).columnLength(t));

            // Mixed: "Hi " (3) + rainbow flag (2) + " end" (4) = 9
            assertEquals(9, new AttributedString("Hi " + RAINBOW_FLAG + " end").columnLength(t));

            // Without terminal: per-codepoint widths (no cluster awareness)
            // Rainbow flag: wcwidth(0x1F3F3)=1 + 0(FE0F) + 0(ZWJ) + wcwidth(0x1F308)=2 = 3
            assertEquals(3, new AttributedString(RAINBOW_FLAG).columnLength());
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnSubSequenceWithGcMode() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            // "AB" + family + "CD" — gc: A(1) B(1) family(2) C(1) D(1) = 6 columns
            String text = "AB" + FAMILY_EMOJI + "CD";
            AttributedString as = new AttributedString(text);

            assertEquals("AB", as.columnSubSequence(0, 2, t).toString());
            assertEquals(FAMILY_EMOJI, as.columnSubSequence(2, 4, t).toString());
            assertEquals("CD", as.columnSubSequence(4, 6, t).toString());

            // Two flags: extract each one
            String twoFlags = FLAG_FR + FLAG_FR;
            AttributedString flags = new AttributedString(twoFlags);
            assertEquals(FLAG_FR, flags.columnSubSequence(0, 2, t).toString());
            assertEquals(FLAG_FR, flags.columnSubSequence(2, 4, t).toString());
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnSubSequenceVS16WithGcMode() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            // "AB" + rainbow flag (2 cols) + "CD" = 6 columns
            String text = "AB" + RAINBOW_FLAG + "CD";
            AttributedString as = new AttributedString(text);

            assertEquals("AB", as.columnSubSequence(0, 2, t).toString());
            assertEquals(RAINBOW_FLAG, as.columnSubSequence(2, 4, t).toString());
            assertEquals("CD", as.columnSubSequence(4, 6, t).toString());
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnSubSequenceNoArgDelegatesToNull() {
        AttributedString as = new AttributedString("Hello");
        assertEquals(
                as.columnSubSequence(1, 3, null).toString(),
                as.columnSubSequence(1, 3).toString());
    }

    @Test
    public void testColumnSplitLengthWithGcMode() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            // "AB" + family + "CD" = 6 columns; split at 4
            String text = "AB" + FAMILY_EMOJI + "CD";
            AttributedString as = new AttributedString(text);
            List<AttributedString> lines = as.columnSplitLength(4, false, true, t);
            assertEquals(2, lines.size());
            assertEquals("AB" + FAMILY_EMOJI, lines.get(0).toString());
            assertEquals("CD", lines.get(1).toString());

            // Three families = 6 columns; split at 5 → [family+family, family]
            String three = FAMILY_EMOJI + FAMILY_EMOJI + FAMILY_EMOJI;
            AttributedString as3 = new AttributedString(three);
            List<AttributedString> lines3 = as3.columnSplitLength(5, false, true, t);
            assertEquals(2, lines3.size());
            assertEquals(FAMILY_EMOJI + FAMILY_EMOJI, lines3.get(0).toString());
            assertEquals(FAMILY_EMOJI, lines3.get(1).toString());
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnSplitLengthVS16WithGcMode() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            // "AB" + rainbow flag (2 cols) + "CD" = 6 columns; split at 4
            String text = "AB" + RAINBOW_FLAG + "CD";
            AttributedString as = new AttributedString(text);
            List<AttributedString> lines = as.columnSplitLength(4, false, true, t);
            assertEquals(2, lines.size());
            assertEquals("AB" + RAINBOW_FLAG, lines.get(0).toString());
            assertEquals("CD", lines.get(1).toString());

            // Three rainbow flags = 6 columns; split at 5 → [flag+flag, flag]
            String three = RAINBOW_FLAG + RAINBOW_FLAG + RAINBOW_FLAG;
            AttributedString as3 = new AttributedString(three);
            List<AttributedString> lines3 = as3.columnSplitLength(5, false, true, t);
            assertEquals(2, lines3.size());
            assertEquals(RAINBOW_FLAG + RAINBOW_FLAG, lines3.get(0).toString());
            assertEquals(RAINBOW_FLAG, lines3.get(1).toString());
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnSplitLengthWithNewlines() throws Exception {
        Terminal t = GraphemeClusterTestTerminal.create();
        try {
            AttributedString as = new AttributedString("AB\nCD");
            List<AttributedString> lines = as.columnSplitLength(80, false, true, t);
            assertEquals(2, lines.size());
            assertEquals("AB", lines.get(0).toString());
            assertEquals("CD", lines.get(1).toString());
        } finally {
            t.close();
        }
    }

    @Test
    public void testColumnSplitLengthNoArgDelegatesToNull() {
        AttributedString as = new AttributedString("Hello World");
        List<AttributedString> a = as.columnSplitLength(5, false, true, null);
        List<AttributedString> b = as.columnSplitLength(5, false, true);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).toString(), b.get(i).toString());
        }
    }
}
