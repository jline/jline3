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

import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttributedStringTest {

    @Test
    public void test() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("echo ");
        sb.append("foo", AttributedStyle.BOLD);

        assertEquals("echo \033[1mfoo\033[0m", sb.toAnsi());

        assertEquals(sb.toString().substring(3, 6), "o f");
        assertEquals(sb.columnSubSequence(3, 6).toString(), "o f");
        assertEquals(sb.columnSubSequence(3, 6).toAnsi(), "o \033[1mf\033[0m");

        sb.append(" ");
        sb.style(AttributedStyle.DEFAULT.background(3));
        sb.append("blue");
        sb.style(AttributedStyle.DEFAULT.backgroundOff());
        sb.append(" ");

        assertEquals("echo \033[1mfoo\033[0m \033[43mblue\033[0m ", sb.toAnsi());

        sb.setLength(0);
        sb.append("echo ");
        sb.style(AttributedStyle.BOLD);
        sb.append("foo");
        sb.append(" ");
        sb.style(sb.style().background(3));
        sb.append("blue");
        sb.style(sb.style().backgroundOff());
        sb.append(" ");

        assertEquals("echo \033[1mfoo \033[43mblue\033[49m \033[0m", sb.toAnsi());

        sb.setLength(0);
        sb.style(AttributedStyle.DEFAULT);
        sb.append("plain");
        sb.style(sb.style().hidden());
        sb.append("\033[38;5;120m");
        sb.style(sb.style().hiddenOff());
        sb.append("green");
        sb.style(sb.style().hidden());
        sb.append("\033[39m");
        sb.style(sb.style().hiddenOff());
        sb.append("plain");
        assertEquals("plain\033[38;5;120mgreen\033[39mplain", sb.toAnsi());
        assertEquals("plaingreenplain".length(), sb.toAttributedString().columnLength());
    }

    @Test
    public void testRuns() {
        String ansi = "echo \033[1mfoo\033[0m \033[43mblue\033[0m ";
        AttributedString sb = AttributedString.fromAnsi(ansi);

        assertEquals(0, sb.runStart(2));
        assertEquals(5, sb.runLimit(2));
        assertEquals(5, sb.runStart(5));
        assertEquals(8, sb.runLimit(6));
        assertEquals(8, sb.runStart(8));
        assertEquals(9, sb.runLimit(8));
        assertEquals(9, sb.runStart(9));
        assertEquals(13, sb.runLimit(9));
    }

    @Test
    public void testAnsi() {
        String ansi = "echo \033[1mfoo \033[43mblue\033[0m ";
        AttributedString str = AttributedString.fromAnsi(ansi);
        assertEquals(ansi, str.toAnsi());
    }

    @Test
    public void testBoldThenFaint() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.styled(AttributedStyle::bold, "bold ");
        sb.styled(AttributedStyle::faint, "faint");
        assertEquals("\u001b[1mbold \u001b[22;2mfaint\u001b[0m", sb.toAnsi());
    }

    @Test
    public void testBoldAndFaint() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.styled(AttributedStyle::bold, s -> s.append("bold ").styled(AttributedStyle::faint, "faint"));
        assertEquals("\u001b[1mbold \u001b[2mfaint\u001b[0m", sb.toAnsi());
    }

    @Test
    public void test256Colors() throws IOException {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(sb.style().background(254));
        sb.append("Hello");
        assertEquals(
                "\033[48;5;254mHello\033[0m",
                sb.toAnsi(new DumbTerminal(
                        "dumb",
                        "xterm-256color",
                        new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(),
                        null)));
    }

    @Test
    public void testCharWidth() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("\u2329\u2329\u2329\u2329"); // 〈〈〈
        assertEquals(4, sb.length());
        assertEquals(8, sb.columnLength());

        assertEquals("", sb.columnSubSequence(0, 1).toString());
        assertEquals(sb.columnSubSequence(1, 3).toString(), "\u2329");
        assertEquals(sb.columnSubSequence(3, 8).toString(), "\u2329\u2329\u2329");
    }

    @Test
    public void testColors() {
        String ansiStr = new AttributedStringBuilder()
                .append("This i")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                .append("s")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                .append(" a")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                .append(" Test.")
                .toAnsi();
        assertEquals("This i\u001B[34ms\u001B[33m a\u001B[31m Test.\u001B[0m", ansiStr);
    }

    @Test
    public void testColumns() {
        AttributedString message = new AttributedString("👍");
        int messageLength = message.columnLength();
        assertEquals(2, messageLength);
        AttributedString messageAgain = message.columnSubSequence(0, messageLength);
        assertEquals("👍", messageAgain.toString());

        message = new AttributedString("\uD83D\uDC46" + "\uD83D\uDC46\uD83C\uDFFB"
                + "\uD83D\uDC46\uD83C\uDFFC"
                + "\uD83D\uDC46\uD83C\uDFFD"
                + "\uD83D\uDC46\uD83C\uDFFE"
                + "\uD83D\uDC46\uD83C\uDFFF");
        messageLength = message.columnLength();
        assertEquals(12, messageLength);
    }

    /**
     * BMP characters with Emoji_Presentation=Yes should have column width 2.
     * These are legacy Unicode characters (Dingbats, Miscellaneous Symbols, etc.)
     * that modern terminals render as 2 columns wide.
     * See https://github.com/jline/jline3/issues/1648
     */
    @Test
    public void testBmpEmojiWidth() {
        // SMP emoji (0x1F000+) — already handled
        assertEquals(2, new AttributedString("\uD83D\uDE00").columnLength()); // 😀 U+1F600
        assertEquals(2, new AttributedString("\uD83D\uDD34").columnLength()); // 🔴 U+1F534

        // BMP Emoji_Presentation=Yes — Miscellaneous Technical
        assertEquals(2, new AttributedString("\u231A").columnLength()); // ⌚ U+231A
        assertEquals(2, new AttributedString("\u231B").columnLength()); // ⌛ U+231B
        assertEquals(2, new AttributedString("\u23E9").columnLength()); // ⏩ U+23E9
        assertEquals(2, new AttributedString("\u23F0").columnLength()); // ⏰ U+23F0
        assertEquals(2, new AttributedString("\u23F3").columnLength()); // ⏳ U+23F3

        // BMP Emoji_Presentation=Yes — Miscellaneous Symbols
        assertEquals(2, new AttributedString("\u2614").columnLength()); // ☔ U+2614
        assertEquals(2, new AttributedString("\u2615").columnLength()); // ☕ U+2615
        assertEquals(2, new AttributedString("\u267F").columnLength()); // ♿ U+267F
        assertEquals(2, new AttributedString("\u2693").columnLength()); // ⚓ U+2693
        assertEquals(2, new AttributedString("\u26A1").columnLength()); // ⚡ U+26A1
        assertEquals(2, new AttributedString("\u26BD").columnLength()); // ⚽ U+26BD
        assertEquals(2, new AttributedString("\u26D4").columnLength()); // ⛔ U+26D4
        assertEquals(2, new AttributedString("\u26FD").columnLength()); // ⛽ U+26FD

        // BMP Emoji_Presentation=Yes — Dingbats
        assertEquals(2, new AttributedString("\u2705").columnLength()); // ✅ U+2705
        assertEquals(2, new AttributedString("\u270A").columnLength()); // ✊ U+270A
        assertEquals(2, new AttributedString("\u2728").columnLength()); // ✨ U+2728
        assertEquals(2, new AttributedString("\u274C").columnLength()); // ❌ U+274C
        assertEquals(2, new AttributedString("\u274E").columnLength()); // ❎ U+274E
        assertEquals(2, new AttributedString("\u2753").columnLength()); // ❓ U+2753
        assertEquals(2, new AttributedString("\u2757").columnLength()); // ❗ U+2757
        assertEquals(2, new AttributedString("\u2795").columnLength()); // ➕ U+2795

        // BMP Emoji_Presentation=Yes — Miscellaneous Symbols and Arrows
        assertEquals(2, new AttributedString("\u2B1B").columnLength()); // ⬛ U+2B1B
        assertEquals(2, new AttributedString("\u2B50").columnLength()); // ⭐ U+2B50
        assertEquals(2, new AttributedString("\u2B55").columnLength()); // ⭕ U+2B55

        // Non-emoji neighbors should remain width 1
        assertEquals(1, new AttributedString("\u2713").columnLength()); // ✓ U+2713
        assertEquals(1, new AttributedString("\u2717").columnLength()); // ✗ U+2717
        assertEquals(1, new AttributedString("\u274B").columnLength()); // ❋ U+274B
        assertEquals(1, new AttributedString("\u2756").columnLength()); // ❖ U+2756

        // Zodiac signs (Emoji_Presentation=Yes)
        assertEquals(2, new AttributedString("\u2648").columnLength()); // ♈ U+2648
        assertEquals(2, new AttributedString("\u2653").columnLength()); // ♓ U+2653

        // Multiple BMP emoji in one string
        assertEquals(4, new AttributedString("\u2705\u274C").columnLength()); // ✅❌
    }

    /**
     * Test updated combining (zero-width) table covers Unicode 16.0 characters.
     * The old table (from ~Unicode 5.1) missed many newer combining marks.
     */
    @Test
    public void testUnicode16Combining() {
        // Vedic Extensions (U+1CD0-1CF9) — added in Unicode 5.2+
        assertEquals(0, WCWidth.wcwidth(0x1CD0)); // VEDIC TONE KARSHANA
        assertEquals(0, WCWidth.wcwidth(0x1CF4)); // VEDIC TONE CANDRA ABOVE

        // Combining Diacritical Marks Extended (U+1AB0-1ACE) — added in Unicode 7.0+
        assertEquals(0, WCWidth.wcwidth(0x1AB0)); // COMBINING DOUBLED CIRCUMFLEX ACCENT
        assertEquals(0, WCWidth.wcwidth(0x1ACE)); // last in block

        // Myanmar Extended combining marks
        assertEquals(0, WCWidth.wcwidth(0x103D)); // MYANMAR CONSONANT SIGN MEDIAL WA
        assertEquals(0, WCWidth.wcwidth(0x103E)); // MYANMAR CONSONANT SIGN MEDIAL HA
        assertEquals(0, WCWidth.wcwidth(0x105E)); // MYANMAR CONSONANT SIGN MON MEDIAL NA

        // Combining Diacritical Marks Supplement extended range (U+20D0-20F0)
        assertEquals(0, WCWidth.wcwidth(0x20F0)); // COMBINING ASTERISK ABOVE

        // Cyrillic Extended-A combining (U+2DE0-2DFF) — added in Unicode 5.1
        assertEquals(0, WCWidth.wcwidth(0x2DE0)); // COMBINING CYRILLIC LETTER BE

        // Mongolian Free Variation Selectors (U+180E-180F) — was missing 180E-180F
        assertEquals(0, WCWidth.wcwidth(0x180E)); // MONGOLIAN VOWEL SEPARATOR
        assertEquals(0, WCWidth.wcwidth(0x180F)); // MONGOLIAN FREE VARIATION SELECTOR FOUR

        // SMP combining marks from newer scripts
        assertEquals(0, WCWidth.wcwidth(0x10AE5)); // MANICHAEAN ABBREVIATION MARK ABOVE
        assertEquals(0, WCWidth.wcwidth(0x11038)); // BRAHMI VOWEL SIGN AA
        assertEquals(0, WCWidth.wcwidth(0x1CF00)); // ZNAMENNY COMBINING MARK

        // Bidi/Format characters added after Unicode 5.1
        assertEquals(0, WCWidth.wcwidth(0x2066)); // LEFT-TO-RIGHT ISOLATE
        assertEquals(0, WCWidth.wcwidth(0x2069)); // POP DIRECTIONAL ISOLATE

        // Non-combining neighbors should still be width 1
        assertEquals(1, WCWidth.wcwidth(0x1ACF)); // after combining block
        assertEquals(1, WCWidth.wcwidth(0x0904)); // DEVANAGARI LETTER SHORT A (Lo, not Mn/Me/Cf)
    }

    /**
     * Test updated East Asian Width table covers Unicode 16.0 ranges.
     * The old code missed Hangul Jamo Extended-A, Tangut, Kana extensions, etc.
     */
    @Test
    public void testUnicode16EastAsianWidth() {
        // Hangul Jamo Extended-A (U+A960-A97C) — was missing
        assertEquals(2, WCWidth.wcwidth(0xA960)); // HANGUL CHOSEONG TIKEUT-MIEUM
        assertEquals(2, WCWidth.wcwidth(0xA97C)); // HANGUL CHOSEONG SSANGYEORINHIEUH

        // Tangut (U+17000-187F7) — was missing
        assertEquals(2, WCWidth.wcwidth(0x17000)); // TANGUT IDEOGRAPH
        assertEquals(2, WCWidth.wcwidth(0x187F7)); // last Tangut ideograph

        // Tangut Components (U+18800-18CD5) — was missing
        assertEquals(2, WCWidth.wcwidth(0x18800)); // TANGUT COMPONENT

        // Kana Supplement (U+1B000-1B122) — was missing
        assertEquals(2, WCWidth.wcwidth(0x1B000)); // KATAKANA LETTER ARCHAIC E
        assertEquals(2, WCWidth.wcwidth(0x1B122)); // KATAKANA LETTER ARCHAIC WU

        // Nushu (U+1B170-1B2FB) — was missing
        assertEquals(2, WCWidth.wcwidth(0x1B170)); // NUSHU CHARACTER

        // Tai Xuan Jing Symbols (U+1D300-1D356) — was missing
        assertEquals(2, WCWidth.wcwidth(0x1D300)); // MONOGRAM FOR EARTH

        // Trigrams (U+2630-2637) — was missing
        assertEquals(2, WCWidth.wcwidth(0x2630)); // TRIGRAM FOR HEAVEN ☰
        assertEquals(2, WCWidth.wcwidth(0x2637)); // TRIGRAM FOR EARTH ☷

        // Yijing mono/digrams (U+268A-268F) — was missing
        assertEquals(2, WCWidth.wcwidth(0x268A)); // MONOGRAM FOR YANG

        // SMP emoji — more precise ranges than old 0x1F000-0x1FEEE
        assertEquals(2, WCWidth.wcwidth(0x1F600)); // 😀
        assertEquals(2, WCWidth.wcwidth(0x1F4A9)); // 💩
        assertEquals(2, WCWidth.wcwidth(0x1FAF8)); // 🫸 (last emoji in Unicode 16.0)

        // CJK basics still work
        assertEquals(2, WCWidth.wcwidth(0x4E00)); // 一
        assertEquals(2, WCWidth.wcwidth(0xAC00)); // 가 (Hangul)
        assertEquals(2, WCWidth.wcwidth(0x3041)); // ぁ (Hiragana)
        assertEquals(2, WCWidth.wcwidth(0xFF01)); // ！ (Fullwidth)

        // Extension B..F and G..J
        assertEquals(2, WCWidth.wcwidth(0x20000)); // CJK Extension B
        assertEquals(2, WCWidth.wcwidth(0x30000)); // CJK Extension G

        // Non-wide characters should still be 1
        assertEquals(1, WCWidth.wcwidth(0x0041)); // A
        assertEquals(1, WCWidth.wcwidth(0x00E9)); // é
    }
}
