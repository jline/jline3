/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedStyle;
import org.jline.utils.Colors;
import org.junit.Before;
import org.junit.Test;

import static org.jline.utils.AttributedStyle.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link StyleResolver}.
 */
public class StyleResolverTest extends StyleTestSupport {

    private StyleResolver underTest;

    @Before
    public void setUp() {
        super.setUp();
        this.underTest = new StyleResolver(source, "test");
    }

    @Test
    public void resolveBold() {
        AttributedStyle style = underTest.resolve("bold");
        assertEquals(BOLD, style);
    }

    @Test
    public void resolveFgRed() {
        AttributedStyle style = underTest.resolve("fg:red");
        assertEquals(DEFAULT.foreground(RED), style);
    }

    @Test
    public void resolveFgRedWithWhitespace() {
        AttributedStyle style = underTest.resolve(" fg:  red ");
        assertEquals(DEFAULT.foreground(RED), style);
    }

    @Test
    public void resolveBgRed() {
        AttributedStyle style = underTest.resolve("bg:red");
        assertEquals(DEFAULT.background(RED), style);
    }

    @Test
    public void resolveInvalidColorMode() {
        AttributedStyle style = underTest.resolve("invalid:red");
        assertEquals(DEFAULT, style);
    }

    @Test
    public void resolveInvalidColorName() {
        AttributedStyle style = underTest.resolve("fg:invalid");
        assertEquals(DEFAULT, style);
    }

    @Test
    public void resolveBoldFgRed() {
        AttributedStyle style = underTest.resolve("bold,fg:red");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveWithWhitespace() {
        AttributedStyle style = underTest.resolve("  bold ,   fg:red   ");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveWithMissingValues() {
        AttributedStyle style = underTest.resolve("bold,,,,,fg:red");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveReferencedStyle() {
        source.set("test", "very-red", "bold,fg:red");
        AttributedStyle style = underTest.resolve(".very-red");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveReferencedStyleMissingWithDefaultDirect() {
        AttributedStyle style = underTest.resolve(".very-red:-bold,fg:red");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveReferencedStyleMissingWitDirectAndWhitespace() {
        AttributedStyle style = underTest.resolve(".very-red   :-   bold,fg:red");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveReferencedStyleMissingWithDefaultReferenced() {
        source.set("test", "more-red", "bold,fg:red");
        AttributedStyle style = underTest.resolve(".very-red:-.more-red");
        assertEquals(BOLD.foreground(RED), style);
    }

    @Test
    public void resolveFgBrightRed() {
        AttributedStyle style = underTest.resolve("fg:bright-red");
        assertEquals(DEFAULT.foreground(BRIGHT + RED), style);
    }

    @Test
    public void resolveFgNotRed() {
        AttributedStyle style = underTest.resolve("fg:!red");
        assertEquals(DEFAULT.foreground(BRIGHT + RED), style);
    }

    @Test
    public void resolveFgOlive() {
        AttributedStyle style = underTest.resolve("fg:~olive");
        assertEquals(DEFAULT.foreground(Colors.rgbColor("olive")), style);
    }

    @Test
    public void resolveFgRgbOrchid() {
        AttributedStyle style = underTest.resolve("fg-rgb:~orchid");
        assertEquals(DEFAULT.foreground(0xD7, 0x5F, 0xD7), style);
    }

    @Test
    public void resolveFgRgbHexa() {
        AttributedStyle style = underTest.resolve("fg-rgb:xaf740b");
        assertEquals(DEFAULT.foreground(0xAF, 0x74, 0x0B), style);
    }

    @Test
    public void resolveFgRgbHexaHash() {
        AttributedStyle style = underTest.resolve("fg-rgb:#af740b");
        assertEquals(DEFAULT.foreground(0xAF, 0x74, 0x0B), style);
    }

    @Test
    public void resolveInvalidXterm256Syntax() {
        AttributedStyle style = underTest.resolve("fg:~");
        assertEquals(DEFAULT, style);
    }

    @Test
    public void resolveInvalidXterm256ColorName() {
        AttributedStyle style = underTest.resolve("fg:~foo");
        assertEquals(DEFAULT, style);
    }

    @Test
    public void checkColorOrdinal() {
        assertEquals(86, Colors.rgbColor("aquamarine1").longValue());
    }
}
