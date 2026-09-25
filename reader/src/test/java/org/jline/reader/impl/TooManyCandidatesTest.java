/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link LineReader#TOO_MANY_CANDIDATES} variable
 * that controls behavior when completion candidates exceed {@link LineReader#LIST_MAX}.
 */
class TooManyCandidatesTest extends ReaderTestSupport {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Create a completer with 10 candidates all starting with "item"
        reader.setCompleter(new StringsCompleter(
                "item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10"));
        // Set the threshold low so it triggers with our 10 candidates
        reader.setVariable(LineReader.LIST_MAX, 5);
        reader.setOpt(Option.AUTO_LIST);
        reader.unsetOpt(Option.AUTO_MENU);
        reader.unsetOpt(Option.MENU_COMPLETE);
    }

    @Test
    void testAskBehaviorDefault() throws IOException {
        // Default behavior ("ask"): prompts "do you wish to see all N possibilities"
        // Pressing 'n' aborts the listing, so the buffer stays at the common prefix
        assertBuffer("item", new TestBuffer("i\tn"));
        // doList was invoked but user declined
        assertTrue(reader.list);
    }

    @Test
    void testAskBehaviorExplicit() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "ask");
        // 'n' declines the prompt
        assertBuffer("item", new TestBuffer("i\tn"));
        assertTrue(reader.list);
    }

    @Test
    void testAskBehaviorAcceptWithY() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "ask");
        // 'y' accepts — candidates are shown, then the buffer is left at the common prefix
        assertBuffer("item", new TestBuffer("i\ty"));
        assertTrue(reader.list);
    }

    @Test
    void testShowBehavior() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "show");
        // "show" mode shows all candidates without prompting — no character is swallowed
        assertBuffer("item", new TestBuffer("i\t"));
        assertTrue(reader.list);
    }

    @Test
    void testShowBehaviorDoesNotSwallowInput() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "show");
        // The next character typed ('2') should be appended to the buffer,
        // not swallowed by a prompt
        assertBuffer("item2", new TestBuffer("i\t2"));
        assertTrue(reader.list);
    }

    @Test
    void testHideBehavior() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "hide");
        // "hide" mode silently suppresses the candidate list
        assertBuffer("item", new TestBuffer("i\t"));
        assertTrue(reader.list);
    }

    @Test
    void testHideBehaviorDoesNotSwallowInput() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "hide");
        // The next character typed should be appended to the buffer
        assertBuffer("item3", new TestBuffer("i\t3"));
        assertTrue(reader.list);
    }

    @Test
    void testPartialBehavior() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "partial");
        // "partial" mode shows up to list-max candidates with a "... and N more" indicator
        assertBuffer("item", new TestBuffer("i\t"));
        assertTrue(reader.list);
        // Verify the output contains the "... and N more" indicator
        assertConsoleOutputContains("... and 5 more");
    }

    @Test
    void testPartialBehaviorDoesNotSwallowInput() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "partial");
        // Partial mode should not swallow the next typed character
        assertBuffer("item4", new TestBuffer("i\t4"));
        assertTrue(reader.list);
    }

    @Test
    void testBelowThresholdShowsNormally() throws IOException {
        // When candidates are below the threshold, all modes should show normally
        reader.setVariable(LineReader.LIST_MAX, 20);
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "ask");
        assertBuffer("item", new TestBuffer("i\t"));
        assertTrue(reader.list);
    }

    @Test
    void testAtExactThresholdShowsNormally() throws IOException {
        // When possibleSize == LIST_MAX, candidates should display normally
        // (overflow triggers only when possibleSize > LIST_MAX)
        reader.setVariable(LineReader.LIST_MAX, 10);
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "hide");
        // With 10 candidates and LIST_MAX=10, "hide" must NOT suppress the list
        assertBuffer("item", new TestBuffer("i\t"));
        assertTrue(reader.list);
    }

    @Test
    void testCaseInsensitiveVariableValue() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "SHOW");
        // Variable value should be case-insensitive
        assertBuffer("item", new TestBuffer("i\t"));
        assertTrue(reader.list);
    }

    @Test
    void testUnknownValueDefaultsToAsk() throws IOException {
        reader.setVariable(LineReader.TOO_MANY_CANDIDATES, "unknown-value");
        // Unknown value should fall back to "ask" behavior
        // 'n' declines the prompt
        assertBuffer("item", new TestBuffer("i\tn"));
        assertTrue(reader.list);
    }
}
