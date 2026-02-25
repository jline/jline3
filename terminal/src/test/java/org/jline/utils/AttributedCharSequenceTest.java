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

import org.jline.terminal.impl.ExternalTerminal;
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

    @Test
    public void testGraphemeClusterColumnLength() {
        // Test per-codepoint widths without grapheme cluster mode.
        // See GraphemeClusterDisplayTest for terminal-aware tests with gc mode enabled.

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
    public void testCharCountForGraphemeCluster() {
        // Family emoji: 11 chars total (4 surrogates + 3 ZWJ)
        assertEquals(11, WCWidth.charCountForGraphemeCluster(FAMILY_EMOJI, 0));

        // Flag: 4 chars (2 surrogate pairs for regional indicators)
        assertEquals(4, WCWidth.charCountForGraphemeCluster(FLAG_FR, 0));

        // Skin tone: 4 chars (surrogate pair + surrogate pair modifier)
        assertEquals(4, WCWidth.charCountForGraphemeCluster(WAVE_SKIN, 0));

        // Plain ASCII: 1 char
        assertEquals(1, WCWidth.charCountForGraphemeCluster("Hello", 0));

        // CJK character: 1 char (BMP)
        assertEquals(1, WCWidth.charCountForGraphemeCluster("中", 0));
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
}
