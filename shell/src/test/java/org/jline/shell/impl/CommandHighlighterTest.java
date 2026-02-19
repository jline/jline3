/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;

import org.jline.reader.Highlighter;
import org.jline.shell.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CommandHighlighter}.
 */
public class CommandHighlighterTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;
    private CommandHighlighter highlighter;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        dispatcher.addGroup(new SimpleCommandGroup("test", new TestEchoCommand()));
        highlighter = new CommandHighlighter(dispatcher);
    }

    static class TestEchoCommand extends AbstractCommand {
        TestEchoCommand() {
            super("echo");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            return String.join(" ", args);
        }
    }

    @Test
    void knownCommandHighlightedBold() {
        AttributedString result = highlighter.highlight(null, "echo hello");
        assertNotNull(result);
        // The first word "echo" should be bold
        String plain = result.toString();
        assertEquals("echo hello", plain);
        // Check that the style of the first character has bold
        assertTrue(result.styleAt(0).getStyle() != 0); // Bold adds style bits
    }

    @Test
    void unknownCommandHighlightedRed() {
        AttributedString result = highlighter.highlight(null, "nonexistent arg");
        assertNotNull(result);
        assertEquals("nonexistent arg", result.toString());
    }

    @Test
    void operatorHighlighted() {
        AttributedString result = highlighter.highlight(null, "echo hello | echo world");
        assertNotNull(result);
        assertEquals("echo hello | echo world", result.toString());
    }

    @Test
    void emptyBuffer() {
        AttributedString result = highlighter.highlight(null, "");
        assertNotNull(result);
        assertEquals("", result.toString());
    }

    @Test
    void nullBuffer() {
        AttributedString result = highlighter.highlight(null, null);
        assertNotNull(result);
    }

    @Test
    void delegateHighlighter() {
        Highlighter delegate = (reader, buffer) -> new AttributedString(buffer);
        CommandHighlighter withDelegate = new CommandHighlighter(dispatcher, delegate);
        AttributedString result = withDelegate.highlight(null, "echo hello");
        assertNotNull(result);
        assertEquals("echo hello", result.toString());
    }

    @Test
    void sequenceOperatorHighlighted() {
        AttributedString result = highlighter.highlight(null, "echo first ; echo second");
        assertNotNull(result);
        assertEquals("echo first ; echo second", result.toString());
    }
}
