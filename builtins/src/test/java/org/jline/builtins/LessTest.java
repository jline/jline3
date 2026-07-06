/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@code -P}/{@code --prompt} option and the {@link Less#defaultPrompt(String)}
 * feature added to {@link Less}.
 */
class LessTest {

    private static final Pattern ANSI_ESCAPE = Pattern.compile("\033\\[[0-9;]*[A-Za-z]");

    /** Strip ANSI escape sequences to get plain visible text. */
    private static String stripAnsi(String s) {
        return ANSI_ESCAPE.matcher(s).replaceAll("");
    }

    private LineDisciplineTerminal newTerminal(ByteArrayOutputStream output) throws IOException {
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("less", "xterm", output, StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));
        return terminal;
    }

    /**
     * Builds a {@link Less} instance with the minimal internal state required to safely
     * invoke the package-private {@code display(boolean)} method directly, without going
     * through the full {@code run()}/{@code openSource()} machinery (which requires a real
     * file/source and an interactive read loop).
     */
    private Less newDisplayableLess(LineDisciplineTerminal terminal) {
        Less less = new Less(terminal, Path.of("."));
        less.size = terminal.getSize();
        // Avoid touching the (null) reader field's owning BufferedReader by providing an
        // empty source, so getLine() cleanly reports EOF instead of throwing an NPE.
        less.reader = new BufferedReader(new StringReader(""));
        // "highlight" defaults to true and is exercised unconditionally by display(), so a
        // real (but file-less) SyntaxHighlighter instance is required to avoid an NPE.
        less.syntaxHighlighter = SyntaxHighlighter.build(new ArrayList<>(), null, "none");
        return less;
    }

    @Test
    void usageIncludesPromptOption() {
        String[] usage = Less.usage();
        assertTrue(
                Arrays.stream(usage).anyMatch(line -> line.contains("--prompt=string")),
                "usage() should document the -P/--prompt option");
    }

    @Test
    void constructorParsesShortPromptOption() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Options opts = Options.compile(Less.usage()).parse(new String[] {"-P", "my-prompt"});
            Less less = new Less(terminal, Path.of("."), opts);
            assertEquals("my-prompt", less.defaultPrompt);
        }
    }

    @Test
    void constructorParsesLongPromptOption() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Options opts = Options.compile(Less.usage()).parse(new String[] {"--prompt=custom>"});
            Less less = new Less(terminal, Path.of("."), opts);
            assertEquals("custom>", less.defaultPrompt);
        }
    }

    @Test
    void constructorWithoutPromptOptionLeavesDefaultPromptNull() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Options opts = Options.compile(Less.usage()).parse(new String[] {});
            Less less = new Less(terminal, Path.of("."), opts);
            assertNull(less.defaultPrompt);
        }
    }

    @Test
    void constructorWithNullOptionsLeavesDefaultPromptNull() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = new Less(terminal, Path.of("."));
            assertNull(less.defaultPrompt);
        }
    }

    @Test
    void defaultPromptSetterUpdatesFieldAndReturnsSameInstance() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = new Less(terminal, Path.of("."));
            Less returned = less.defaultPrompt("some-prompt");
            assertSame(less, returned, "defaultPrompt() should return 'this' for chaining");
            assertEquals("some-prompt", less.defaultPrompt);
        }
    }

    @Test
    void defaultPromptSetterOverridesConstructorOption() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Options opts = Options.compile(Less.usage()).parse(new String[] {"-P", "from-option"});
            Less less = new Less(terminal, Path.of("."), opts);
            assertEquals("from-option", less.defaultPrompt);
            less.defaultPrompt("from-setter");
            assertEquals("from-setter", less.defaultPrompt);
        }
    }

    @Test
    @Timeout(5)
    void displayRendersDefaultPromptWhenSet() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);
            less.defaultPrompt("CUSTOM-PROMPT>");

            less.display(false);

            String rendered = output.toString(StandardCharsets.UTF_8);
            assertTrue(
                    rendered.contains("CUSTOM-PROMPT>"), "Expected the rendered output to contain the custom prompt");
        }
    }

    @Test
    @Timeout(5)
    void displayFallsBackToColonWhenNoPromptSet() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);

            less.display(false);

            // Strip ANSI escape sequences so we match the actual visible ':' prompt
            // rather than a stray colon inside an escape sequence.
            String plainText = stripAnsi(output.toString(StandardCharsets.UTF_8));
            assertTrue(plainText.contains(":"), "Expected the default ':' prompt in the visible output");
        }
    }

    @Test
    @Timeout(5)
    void displayMessageTakesPrecedenceOverDefaultPrompt() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);
            less.defaultPrompt("CUSTOM-PROMPT>");
            less.message = "Some status message";

            less.display(false);

            String rendered = output.toString(StandardCharsets.UTF_8);
            assertTrue(rendered.contains("Some status message"));
            assertFalse(
                    rendered.contains("CUSTOM-PROMPT>"), "An active message should take precedence over defaultPrompt");
        }
    }

    @Test
    @Timeout(5)
    void displayPatternTakesPrecedenceOverDefaultPrompt() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);
            less.defaultPrompt("CUSTOM-PROMPT>");
            less.displayPattern = "foo";

            less.display(false);

            String rendered = output.toString(StandardCharsets.UTF_8);
            assertFalse(
                    rendered.contains("CUSTOM-PROMPT>"),
                    "An active displayPattern should take precedence over defaultPrompt");
        }
    }

    /**
     * Reproduces the NPE described in GitHub issue #2040: when firstLineToDisplay is
     * beyond the available content (e.g. after scrolling past EOF in a
     * SequenceInputStream), the pre-highlighting loop in display() called
     * syntaxHighlighter.highlight(getLine(i)) with a null line, causing an NPE in
     * SyntaxHighlighter.splitAndHighlight().
     */
    @Test
    @Timeout(5)
    void displayDoesNotThrowWhenFirstLineToDisplayExceedsContent() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);
            // Provide a short source (3 lines) but set the display position past it
            less.reader = new BufferedReader(new StringReader("line1\nline2\nline3\n"));
            less.firstLineToDisplay = 10;

            // Before the fix, this threw NullPointerException in
            // SyntaxHighlighter.splitAndHighlight because getLine() returned null
            less.display(false);
        }
    }

    @Test
    @Timeout(5)
    void displayHandlesEmptyDefaultPromptWithoutError() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal = newTerminal(output)) {
            Less less = newDisplayableLess(terminal);
            less.defaultPrompt("");

            // An empty (but non-null) prompt is still "set", so it must be handled without
            // throwing and without falling back to the ":" default rendering path.
            boolean fitsOneScreen = less.display(false);

            assertFalse(fitsOneScreen);
            // Verify that the empty prompt does not fall back to the ':' default.
            // With an empty source and empty prompt, ':' should not appear in the visible output.
            String plainText = stripAnsi(output.toString(StandardCharsets.UTF_8));
            assertFalse(plainText.contains(":"), "Empty defaultPrompt should not fall back to the ':' default");
        }
    }
}
