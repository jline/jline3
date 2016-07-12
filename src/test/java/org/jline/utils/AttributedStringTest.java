package org.jline.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributedStringTest {


    @Test
    public void test() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("echo ");
        sb.append("foo", AttributedStyle.BOLD);

        assertEquals("echo \033[1mfoo\033[0m", sb.toAnsi());

        assertEquals("o f", sb.toString().substring(3, 6));
        assertEquals("o f", sb.columnSubSequence(3, 6).toString());
        assertEquals("o \033[1mf\033[0m", sb.columnSubSequence(3, 6).toAnsi());

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
    public void testCharWidth() {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("\u2329\u2329\u2329\u2329");  // 〈〈〈
        assertEquals(4, sb.length());
        assertEquals(8, sb.columnLength());

        assertEquals("", sb.columnSubSequence(0, 1).toString());
        assertEquals("\u2329", sb.columnSubSequence(1, 3).toString());
        assertEquals("\u2329\u2329\u2329", sb.columnSubSequence(3, 8).toString());
    }
}
