/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jline.builtins.ConfigurationPath;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SystemHighlighter}.
 */
class SystemHighlighterTest {

    private static final String OSC_TITLE = "\u001b]0;pwned\u0007";

    private ByteArrayOutputStream out;
    private Terminal terminal;
    private SystemRegistryImpl registry;

    @BeforeEach
    void setUp() throws IOException {
        out = new ByteArrayOutputStream();
        // "cat rep", Tab (the single candidate is inserted into the buffer), Enter
        ByteArrayInputStream in = new ByteArrayInputStream("cat rep\t\r".getBytes(StandardCharsets.UTF_8));
        terminal = new DumbTerminal("terminal", "ansi", in, out, StandardCharsets.UTF_8);
        terminal.setSize(Size.of(160, 40));
        Path cfgPath = Path.of(".");
        registry = new SystemRegistryImpl(
                new DefaultParser(),
                terminal,
                () -> Path.of(System.getProperty("user.dir")),
                new ConfigurationPath(cfgPath, cfgPath));
        registry.setCommandRegistries();
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            if (terminal != null) {
                terminal.close();
            }
        } finally {
            if (registry != null) {
                registry.close();
            }
        }
    }

    @Test
    void completedControlCharactersNotWrittenToTerminal() {
        // a file name carrying OSC 0 (set window title), offered verbatim as FilesCompleter does
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(new SystemHighlighter(null, null, null))
                .completer((r, l, candidates) -> candidates.add(new Candidate("report" + OSC_TITLE + ".txt")))
                .build();

        String line = reader.readLine("> ");

        assertTrue(line.contains(OSC_TITLE), "the accepted line keeps the completed text");
        String output = out.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains(OSC_TITLE), "raw OSC 0 reached the terminal");
        assertTrue(output.contains("^["), "ESC should be shown in caret notation");
    }

    @Test
    void controlCharactersShownAsCaretNotation() throws IOException {
        // Outside readLine() the region state is still null, which would send highlight() down the
        // DefaultHighlighter branch; report NONE so the systemHighlight branch is exercised directly.
        LineReader reader = new LineReaderImpl(terminal) {
            @Override
            public RegionType getRegionActive() {
                return RegionType.NONE;
            }
        };
        SystemHighlighter highlighter = new SystemHighlighter(null, null, null);

        AttributedString result = highlighter.highlight(reader, "cat 'report" + OSC_TITLE + ".txt' x\u009b2J");

        assertEquals("cat 'report^[]0;pwned^G.txt' x2J", result.toString());
    }
}
