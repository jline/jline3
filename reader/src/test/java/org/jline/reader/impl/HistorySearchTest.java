/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jline.keymap.KeyMap.translate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class HistorySearchTest extends ReaderTestSupport {

    private DefaultHistory history;

    @BeforeEach
    public void setupHistory() {
        history = new DefaultHistory();
        reader.setVariable(LineReader.HISTORY_SIZE, 10);
        reader.setHistory(history);
        history.add("foo");
        history.add("fiddle");
        history.add("faddle");
    }

    @Test
    public void testZshLikeBackspaceNavigation() throws Exception {
        // Test zsh-like behavior: Ctrl+R, type "f", Ctrl+R to go deeper, then backspace should move back up
        // History: ["foo", "fiddle", "faddle"] - "faddle" is most recent
        var zshLikeNavigation = new TestBuffer()
                .ctrl('R') // Start search
                .append("f") // Type "f" - should find "faddle" (most recent match)
                .ctrl('R') // Go deeper - should find "fiddle" (next older match)
                .back() // Backspace should move back to "faddle", not delete "f"
                .enter();

        assertLine("faddle", zshLikeNavigation, false);
    }

    @Test
    public void testBackspaceWithFailingSearch() throws Exception {
        // Test zsh behavior with failing search
        // History: ["foo", "fiddle", "faddle"]
        var failingSearchBackspace = new TestBuffer()
                .ctrl('R') // Start search
                .append("fi") // Type "fi" - should find "fiddle" (only match for "fi")
                .ctrl('R') // Go deeper - no more matches for "fi", search fails
                .back() // First backspace: clears failing state, stays on "fiddle"
                .back() // Second backspace: deletes "i", search term becomes "f"
                .back() // Third backspace: deletes "f", search term empty
                .enter();

        assertLine("", failingSearchBackspace, false); // Should restore original buffer (empty)
    }

    @Test
    public void testTypingAndBackspaceNavigation() throws Exception {
        // Test zsh behavior: typing changes search term, backspace navigates then deletes
        // History: ["foo", "fiddle", "faddle"]
        var typingAndBackspace = new TestBuffer()
                .ctrl('R') // Start search
                .append("f") // Type "f" - should find "faddle" (most recent)
                .ctrl('R') // Go deeper - should find "fiddle" (next older), push "faddle" to history
                .append("o") // Type "o" - search term becomes "fo", finds "foo"
                .back() // Backspace deletes "o", search term becomes "f", finds "fiddle" (current position)
                .back() // Backspace navigates back to "faddle" (from history for term "f")
                .back() // Backspace deletes "f", search term empty, restore original
                .enter();

        assertLine("", typingAndBackspace, false); // Should restore original buffer (empty)
    }

    @Test
    public void testExactZshBehaviorCase3() throws Exception {
        // Test the exact case 3 behavior you described
        // History: ["foo", "fiddle", "faddle"]
        var exactZshCase3 = new TestBuffer()
                .ctrl('R') // Start search
                .append("f") // 'f' -> finds "faddle"
                .ctrl('R') // Ctrl+R -> finds "fiddle"
                .append("o") // 'o' -> finds "foo"
                .back() // backspace -> deletes 'o', search term becomes 'f', finds "fiddle"
                .back() // backspace -> finds "faddle"
                .back() // backspace -> deletes 'f', search term empty, restore original
                .enter();

        assertLine("", exactZshCase3, false); // Should restore original buffer (empty)
    }

    @Test
    public void testCaseInsensitive() throws Exception {
        reader.setOpt(LineReader.Option.CASE_INSENSITIVE_SEARCH);
        try {
            assertLine("fiddle", new TestBuffer().ctrl('R').append("I").enter(), false);
        } finally {
            reader.unsetOpt(LineReader.Option.CASE_INSENSITIVE_SEARCH);
        }
    }

    @Test
    public void testReverseHistorySearch() throws Exception {
        // TODO: use assertBuffer
        String readLineResult;
        in.setIn(new ByteArrayInputStream(translate("^Rf\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("faddle", readLineResult);
        assertEquals(3, history.size());

        in.setIn(new ByteArrayInputStream(translate("^Rf^R^R^R^R^R\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("foo", readLineResult);
        assertEquals(4, history.size());

        in.setIn(new ByteArrayInputStream(translate("^Rf^R^R\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(5, history.size());
    }

    @Test
    public void testForwardHistorySearch() throws Exception {
        String readLineResult;
        in.setIn(new ByteArrayInputStream(translate("^Rf^R^R^S\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(4, history.size());

        in.setIn(new ByteArrayInputStream(translate("^Rf^R^R^R^S^S\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("faddle", readLineResult);
        assertEquals(5, history.size());

        in.setIn(new ByteArrayInputStream(translate("^Rf^R^R^R^R^S\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(6, history.size());
    }

    @Test
    public void testSearchHistoryAfterHittingEnd() throws Exception {
        String readLineResult;
        in.setIn(new ByteArrayInputStream(translate("^Rf^R^R^R^S\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(4, history.size());
    }

    @Test
    public void testSearchHistoryWithNoMatches() throws Exception {
        String readLineResult;
        in.setIn(new ByteArrayInputStream(translate("x^S^S\n").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("x", readLineResult);
        assertEquals(4, history.size());
    }

    @Test
    public void testAbortingSearchRetainsCurrentBufferAndPrintsDetails() throws Exception {
        in.setIn(new ByteArrayInputStream(translate("f^Rf^G").getBytes()));
        try {
            reader.readLine();
            fail("Expected an EndOfFileException to be thrown");
        } catch (EndOfFileException e) {
            // expected
        }
        assertEquals("f", reader.getBuffer().toString());
        assertEquals(3, history.size());
    }

    @Test
    public void testAbortingAfterSearchingPreviousLinesGivesBlank() throws Exception {
        String readLineResult;
        in.setIn(new ByteArrayInputStream(translate("f^Rf\nfoo^G").getBytes()));
        readLineResult = reader.readLine();
        assertEquals("f", readLineResult);
        assertEquals(4, history.size());

        try {
            reader.readLine();
            fail("Expected an EndOfFileException to be thrown");
        } catch (EndOfFileException e) {
            // expected
        }
        assertEquals("", reader.getBuffer().toString());
        assertEquals(4, history.size());
    }

    @Test
    public void testSearchOnEmptyHistory() throws Exception {
        history.purge();

        in.setIn(new ByteArrayInputStream(translate("^Sa").getBytes()));
        try {
            reader.readLine();
            fail("Expected an EndOfFileException to be thrown");
        } catch (EndOfFileException e) {
            // expected
        }
    }
}
