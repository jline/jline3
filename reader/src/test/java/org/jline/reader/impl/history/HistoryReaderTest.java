/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl.history;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Reference;
import org.jline.reader.impl.ReaderTestSupport;
import org.junit.jupiter.api.Test;

import static org.jline.reader.LineReader.ACCEPT_LINE;
import static org.jline.reader.LineReader.BACKWARD_CHAR;
import static org.jline.reader.LineReader.BEGINNING_OF_LINE;
import static org.jline.reader.LineReader.DOWN_HISTORY;
import static org.jline.reader.LineReader.DOWN_LINE_OR_HISTORY;
import static org.jline.reader.LineReader.HISTORY_SEARCH_BACKWARD;
import static org.jline.reader.LineReader.HISTORY_SEARCH_FORWARD;
import static org.jline.reader.LineReader.UP_HISTORY;
import static org.jline.reader.LineReader.UP_LINE_OR_HISTORY;

/**
 * Tests command history.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class HistoryReaderTest extends ReaderTestSupport {
    @Test
    public void testSingleHistory() throws Exception {
        KeyMap<Binding> map = reader.getKeys();

        // Map in HISTORY_SEARCH_BACKWARD.
        map.bind(new Reference(UP_LINE_OR_HISTORY), "\033[A");
        map.bind(new Reference(DOWN_LINE_OR_HISTORY), "\033[B");

        TestBuffer b = new TestBuffer()
                .append("test line 1")
                .op(ACCEPT_LINE)
                .append("test line 2")
                .op(ACCEPT_LINE)
                .append("test line 3")
                .op(ACCEPT_LINE)
                .append("test line 4")
                .op(ACCEPT_LINE)
                .append("test line 5")
                .op(ACCEPT_LINE)
                .append("ab");

        assertBuffer("ab", b);

        assertBuffer("test line 5", b = b.op(UP_HISTORY));
        assertBuffer("test line 5", b = b.op(BACKWARD_CHAR));
        assertBuffer("test line 4", b = b.op(UP_HISTORY));
        assertBuffer("test line 5", b = b.op(DOWN_HISTORY));
        assertBuffer("test line 4", b = b.op(UP_HISTORY));
        assertBuffer("test line 3", b = b.op(UP_HISTORY));
        assertBuffer("test line 2", b = b.op(UP_HISTORY));
        assertBuffer("test line 1", b = b.op(UP_HISTORY));

        // beginning of history
        assertBuffer("test line 1", b = b.op(UP_HISTORY));
        assertBuffer("test line 1", b = b.op(UP_HISTORY));
        assertBuffer("test line 1", b = b.op(UP_HISTORY));
        assertBuffer("test line 1", b = b.op(UP_HISTORY));

        assertBuffer("test line 2", b = b.op(DOWN_HISTORY));
        assertBuffer("test line 3", b = b.op(DOWN_HISTORY));
        assertBuffer("test line 4", b = b.op(DOWN_HISTORY));
        assertBuffer("test line 5", b = b.op(DOWN_HISTORY));

        // end of history
        assertBuffer("ab", b = b.op(DOWN_HISTORY));
        assertBuffer("ab", b = b.op(DOWN_HISTORY));
        assertBuffer("ab", b = b.op(DOWN_HISTORY));

        assertBuffer("test line 5", b = b.op(UP_HISTORY));
        assertBuffer("test line 4", b = b.op(UP_HISTORY));
        b = b.op(BEGINNING_OF_LINE).append("XXX").op(ACCEPT_LINE);
        assertBuffer("XXXtest line 4", b = b.op(UP_HISTORY));
        assertBuffer("test line 5", b = b.op(UP_HISTORY));
        assertBuffer("test line 4", b = b.op(UP_HISTORY));
        assertBuffer("test line 5", b = b.op(DOWN_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(DOWN_HISTORY));
        assertBuffer("", b = b.op(DOWN_HISTORY));

        assertBuffer("XXXtest line 4", b = b.op(UP_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(UP_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(UP_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(UP_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(UP_HISTORY));
    }

    @Test
    public void testHistorySearchBackwardAndForward() throws Exception {
        KeyMap<Binding> map = reader.getKeys();

        // Map in HISTORY_SEARCH_BACKWARD.
        map.bind(new Reference(HISTORY_SEARCH_BACKWARD), "\033[0A");
        map.bind(new Reference(HISTORY_SEARCH_FORWARD), "\033[0B");

        TestBuffer b = new TestBuffer()
                .append("toes")
                .op(ACCEPT_LINE)
                .append("the quick brown")
                .op(ACCEPT_LINE)
                .append("fox jumps")
                .op(ACCEPT_LINE)
                .append("over the")
                .op(ACCEPT_LINE)
                .append("lazy dog")
                .op(ACCEPT_LINE)
                .append("");

        assertBuffer("", b);

        // Using history-search-backward behaves like previous-history when
        // no input has been provided.
        assertBuffer("lazy dog", b = b.append("\033[0A"));
        assertBuffer("over the", b = b.append("\033[0A"));
        assertBuffer("fox jumps", b = b.append("\033[0A"));

        // history-search-forward should behave line next-history when no
        // input has been provided.
        assertBuffer("over the", b = b.append("\033[0B"));
        assertBuffer("lazy dog", b = b.append("\033[0B"));
        assertBuffer("", b = b.append("\033[0B"));

        // Make sure we go back correctly.
        assertBuffer("lazy dog", b = b.append("\033[0A"));
        assertBuffer("over the", b = b.append("\033[0A"));
        assertBuffer("fox jumps", b = b.append("\033[0A"));

        // Search forward on 'l'.
        b = b.append("l");
        assertBuffer("fox jumpsl", b = b.append("\033[0B"));

        // Try moving again.
        assertBuffer("fox jumpsl", b.append("\033[0B"));
        assertBuffer("fox jumps", b.append("\033[0A"));

        // Now we should have more context and history-search-backward should
        // take us to "the quick brown" line.
        b = b.back(100).append("t");
        assertBuffer("the quick brown", b = b.append("\033[0A"));

        // Try moving backward again.
        assertBuffer("toes", b = b.append("\033[0A"));

        assertBuffer("the quick brown", b = b.append("\033[0B"));

        b = b.back(100);
        assertBuffer("fox jumps", b = b.append("\033[0B"));

        b = b.back(100);
        b = b.append("to");
        assertBuffer("toes", b = b.append("\033[0A"));
    }
}
