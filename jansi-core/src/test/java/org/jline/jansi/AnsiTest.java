/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.jansi.Ansi.Color;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link Ansi} class.
 *
 */
public class AnsiTest {
    @Test
    public void testSetEnabled() throws Exception {
        Ansi.setEnabled(false);
        new Thread(() -> assertFalse(Ansi.isEnabled())).run();

        Ansi.setEnabled(true);
        new Thread(() -> assertTrue(Ansi.isEnabled())).run();
    }

    @Test
    public void testClone() throws CloneNotSupportedException {
        Ansi ansi = Ansi.ansi().a("Some text").bg(Color.BLACK).fg(Color.WHITE);
        Ansi clone = new Ansi(ansi);

        assertEquals(ansi.a("test").reset().toString(), clone.a("test").reset().toString());
    }

    @Test
    public void testApply() {
        assertEquals("test", Ansi.ansi().apply(ansi -> ansi.a("test")).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "-2147483648,ESC[2147483647T", "2147483647,ESC[2147483647S",
        "-100000,ESC[100000T", "100000,ESC[100000S"
    })
    public void testScrollUp(int x, String expected) {
        assertAnsi(expected, Ansi.ansi().scrollUp(x));
    }

    @ParameterizedTest
    @CsvSource({
        "-2147483648,ESC[2147483647S", "2147483647,ESC[2147483647T",
        "-100000,ESC[100000S", "100000,ESC[100000T"
    })
    public void testScrollDown(int x, String expected) {
        assertAnsi(expected, Ansi.ansi().scrollDown(x));
    }

    @ParameterizedTest
    @CsvSource({
        "-1,-1,ESC[1;1H", "-1,0,ESC[1;1H", "-1,1,ESC[1;1H", "-1,2,ESC[1;2H",
        "0,-1,ESC[1;1H", "0,0,ESC[1;1H", "0,1,ESC[1;1H", "0,2,ESC[1;2H",
        "1,-1,ESC[1;1H", "1,0,ESC[1;1H", "1,1,ESC[1;1H", "1,2,ESC[1;2H",
        "2,-1,ESC[2;1H", "2,0,ESC[2;1H", "2,1,ESC[2;1H", "2,2,ESC[2;2H"
    })
    public void testCursor(int x, int y, String expected) {
        assertAnsi(expected, new Ansi().cursor(x, y));
    }

    @ParameterizedTest
    @CsvSource({"-1,ESC[1G", "0,ESC[1G", "1,ESC[1G", "2,ESC[2G"})
    public void testCursorToColumn(int x, String expected) {
        assertAnsi(expected, new Ansi().cursorToColumn(x));
    }

    @ParameterizedTest
    @CsvSource({"-2,ESC[2B", "-1,ESC[1B", "0,''", "1,ESC[1A", "2,ESC[2A"})
    public void testCursorUp(int y, String expected) {
        assertAnsi(expected, new Ansi().cursorUp(y));
    }

    @ParameterizedTest
    @CsvSource({"-2,ESC[2A", "-1,ESC[1A", "0,''", "1,ESC[1B", "2,ESC[2B"})
    public void testCursorDown(int y, String expected) {
        assertAnsi(expected, new Ansi().cursorDown(y));
    }

    @ParameterizedTest
    @CsvSource({"-2,ESC[2D", "-1,ESC[1D", "0,''", "1,ESC[1C", "2,ESC[2C"})
    public void testCursorRight(int x, String expected) {
        assertAnsi(expected, new Ansi().cursorRight(x));
    }

    @ParameterizedTest
    @CsvSource({"-2,ESC[2C", "-1,ESC[1C", "0,''", "1,ESC[1D", "2,ESC[2D"})
    public void testCursorLeft(int x, String expected) {
        assertAnsi(expected, new Ansi().cursorLeft(x));
    }

    @ParameterizedTest
    @CsvSource({
        "-2,-2,ESC[2DESC[2A", "-2,-1,ESC[2DESC[1A", "-2,0,ESC[2D", "-2,1,ESC[2DESC[1B", "-2,2,ESC[2DESC[2B",
        "-1,-2,ESC[1DESC[2A", "-1,-1,ESC[1DESC[1A", "-1,0,ESC[1D", "-1,1,ESC[1DESC[1B", "-1,2,ESC[1DESC[2B",
        "0,-2,ESC[2A", "0,-1,ESC[1A", "0,0,''", "0,1,ESC[1B", "0,2,ESC[2B",
        "1,-2,ESC[1CESC[2A", "1,-1,ESC[1CESC[1A", "1,0,ESC[1C", "1,1,ESC[1CESC[1B", "1,2,ESC[1CESC[2B",
        "2,-2,ESC[2CESC[2A", "2,-1,ESC[2CESC[1A", "2,0,ESC[2C", "2,1,ESC[2CESC[1B", "2,2,ESC[2CESC[2B"
    })
    public void testCursorMove(int x, int y, String expected) {
        assertAnsi(expected, new Ansi().cursorMove(x, y));
    }

    @Test
    public void testCursorDownLine() {
        assertAnsi("ESC[E", new Ansi().cursorDownLine());
    }

    @ParameterizedTest
    @CsvSource({"-2,ESC[2F", "-1,ESC[1F", "0,ESC[0E", "1,ESC[1E", "2,ESC[2E"})
    public void testCursorDownLine(int n, String expected) {
        assertAnsi(expected, new Ansi().cursorDownLine(n));
    }

    @Test
    public void testCursorUpLine() {
        assertAnsi("ESC[F", new Ansi().cursorUpLine());
    }

    @ParameterizedTest
    @CsvSource({"-2,ESC[2E", "-1,ESC[1E", "0,ESC[0F", "1,ESC[1F", "2,ESC[2F"})
    public void testCursorUpLine(int n, String expected) {
        assertAnsi(expected, new Ansi().cursorUpLine(n));
    }

    @Test
    public void testColorDisabled() {
        Ansi.setEnabled(false);
        try {
            assertEquals(
                    "test",
                    Ansi.ansi()
                            .fg(32)
                            .a("t")
                            .fgRgb(0)
                            .a("e")
                            .bg(24)
                            .a("s")
                            .bgRgb(100)
                            .a("t")
                            .toString());
        } finally {
            Ansi.setEnabled(true);
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @Disabled("Does not really fail: launch `javaw -jar jansi-xxx.jar` directly instead")
    public void testAnsiMainWithNoConsole() throws Exception {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path java = javaHome.resolve("bin\\javaw.exe");
        String cp = System.getProperty("java.class.path");

        Process process = new ProcessBuilder()
                .command(java.toString(), "-cp", cp, AnsiMain.class.getName())
                .start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            while (true) {
                int nb = in.read(buffer);
                if (nb > 0) {
                    baos.write(buffer, 0, nb);
                } else {
                    break;
                }
            }
        }

        assertTrue(baos.toString().contains("test on System.out"), baos.toString());
    }

    private static void assertAnsi(String expected, Ansi actual) {
        assertEquals(expected.replace("ESC", "\033"), actual.toString());
    }
}
