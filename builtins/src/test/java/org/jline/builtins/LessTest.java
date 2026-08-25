/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.jline.builtins.Source.InputStreamSource;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the control-character stripping security fix in {@link Less}.
 */
class LessTest {

    private static final Pattern ANSI_ESCAPE = Pattern.compile("\033\\[[0-9;]*[A-Za-z]");

    /** Strip ANSI escape sequences to get plain visible text. */
    private static String stripAnsi(String s) {
        return ANSI_ESCAPE.matcher(s).replaceAll("");
    }

    private LineDisciplineTerminal newTerminal(ByteArrayOutputStream output) throws IOException {
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("less", "xterm", output, StandardCharsets.UTF_8);
        terminal.setSize(new Size(80, 24));
        return terminal;
    }

    /**
     * Builds a {@link Less} instance with the minimal internal state required to safely
     * invoke the package-private {@code display(boolean)} method directly, without going
     * through the full {@code run()}/{@code openSource()} machinery (which requires a real
     * file/source and an interactive read loop).
     */
    private Less newDisplayableLess(LineDisciplineTerminal terminal) {
        Less less = new Less(terminal, Paths.get("."));
        less.size.copy(terminal.getSize());
        less.reader = new BufferedReader(new StringReader(""));
        less.syntaxHighlighter = SyntaxHighlighter.build(new ArrayList<>(), null, "none");
        return less;
    }

    @Test
    @Timeout(5)
    void displayStripsControlCharactersFromMessage() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);
            less.message = "réport\u001b]0;pwned\u0007.txt";

            less.display(false);

            String rendered = output.toString(StandardCharsets.UTF_8.name());
            assertFalse(rendered.contains("\u001b]0;"), "OSC introducer must not reach the terminal");
            assertFalse(rendered.contains("\u0007"), "BEL must not reach the terminal");
            String plainText = stripAnsi(rendered);
            assertTrue(plainText.contains("réport"), "non-ASCII file name text should be preserved");
            assertTrue(plainText.contains("pwned.txt"), "surrounding file name text should be preserved");
        }
    }

    @Test
    @Timeout(5)
    void openSourceStripsControlCharactersFromFileNames() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = new Less(terminal, Paths.get("."));
            less.size.copy(terminal.getSize());
            String name = "réport\u001b]0;pwned\u0007.txt";
            Source missing = new Source() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public InputStream read() throws IOException {
                    throw new FileNotFoundException(name);
                }

                @Override
                public Long lines() {
                    return null;
                }
            };
            Source next = new InputStreamSource(new ByteArrayInputStream(new byte[0]), true, name);
            less.sources = new ArrayList<>(Arrays.asList(
                    new InputStreamSource(new ByteArrayInputStream(new byte[0]), true, "help"), missing, next));
            less.sourceIdx = 1;
            terminal.processInputByte('\n');

            less.openSource();

            String rendered = output.toString(StandardCharsets.UTF_8.name());
            assertFalse(rendered.contains("\u001b]0;"), "OSC introducer must not reach the terminal");
            assertFalse(rendered.contains("\u0007"), "BEL must not reach the terminal");
            String plainText = stripAnsi(rendered);
            assertTrue(plainText.contains("réport"), "non-ASCII file name text should be preserved");
            assertTrue(plainText.contains("pwned.txt not found!"), "not-found line should keep printable text");
            assertTrue(plainText.contains("pwned.txt (press RETURN)"), "press-RETURN line should keep printable text");
        }
    }
}
