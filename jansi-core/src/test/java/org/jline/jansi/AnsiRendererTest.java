/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.jline.jansi.Ansi.*;
import static org.jline.jansi.Ansi.Attribute.*;
import static org.jline.jansi.Ansi.Color.*;
import static org.jline.jansi.AnsiRenderer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link AnsiRenderer} class.
 */
class AnsiRendererTest {
    @BeforeAll
    static void setUp() {
        Ansi.setEnabled(true);
    }

    @Test
    void testTest() {
        assertFalse(test("foo"));
        assertTrue(test("@|foo|"));
        assertTrue(test("@|foo"));
    }

    @Test
    void testRender() {
        String str = render("@|bold foo|@");
        System.out.println(str);
        assertEquals(ansi().a(INTENSITY_BOLD).a("foo").reset().toString(), str);
        assertEquals(ansi().bold().a("foo").reset().toString(), str);
    }

    @Test
    void testRenderCodes() {
        String str = renderCodes("bold red");
        System.out.println(str);
        assertEquals(ansi().bold().fg(Color.RED).toString(), str);
    }

    @Test
    void testRender2() {
        String str = render("@|bold,red foo|@");
        System.out.println(str);
        assertEquals(Ansi.ansi().a(INTENSITY_BOLD).fg(RED).a("foo").reset().toString(), str);
        assertEquals(Ansi.ansi().bold().fgRed().a("foo").reset().toString(), str);
    }

    @Test
    void testRender3() {
        String str = render("@|bold,red foo bar baz|@");
        System.out.println(str);
        assertEquals(ansi().a(INTENSITY_BOLD).fg(RED).a("foo bar baz").reset().toString(), str);
        assertEquals(ansi().bold().fgRed().a("foo bar baz").reset().toString(), str);
    }

    @Test
    void testRender4() {
        String str = render("@|bold,red foo bar baz|@ ick @|bold,red foo bar baz|@");
        System.out.println(str);
        assertEquals(
                ansi().a(INTENSITY_BOLD)
                        .fg(RED)
                        .a("foo bar baz")
                        .reset()
                        .a(" ick ")
                        .a(INTENSITY_BOLD)
                        .fg(RED)
                        .a("foo bar baz")
                        .reset()
                        .toString(),
                str);
    }

    @Test
    void testRender5() {
        // Check the ansi() render method.
        String str = ansi().render("@|bold Hello|@").toString();
        System.out.println(str);
        assertEquals(ansi().a(INTENSITY_BOLD).a("Hello").reset().toString(), str);
    }

    @Test
    void testRenderNothing() {
        assertEquals("foo", render("foo"));
    }

    @Test
    void testRenderInvalidMissingEnd() {
        String str = render("@|bold foo");
        assertEquals("@|bold foo", str);
    }

    @Test
    void testRenderInvalidEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> render("@|@"));
    }

    @Test
    void testRenderInvalidMissingText() {
        String str = render("@|bold|@");
        assertEquals("@|bold|@", str);
    }
}
