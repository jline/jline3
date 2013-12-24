/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.console.history;

import jline.console.ConsoleReaderTestSupport;
import jline.console.KeyMap;
import org.junit.Test;

import static jline.console.Operation.*;

/**
 * Tests command history.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class HistoryTest
    extends ConsoleReaderTestSupport
{
    @Test
    public void testSingleHistory() throws Exception {
        Buffer b = new Buffer().
            append("test line 1").op(ACCEPT_LINE).
            append("test line 2").op(ACCEPT_LINE).
            append("test line 3").op(ACCEPT_LINE).
            append("test line 4").op(ACCEPT_LINE).
            append("test line 5").op(ACCEPT_LINE).
            append("");

        assertBuffer("", b);

        assertBuffer("test line 5", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 5", b = b.op(BACKWARD_CHAR));
        assertBuffer("test line 4", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 5", b = b.op(NEXT_HISTORY));
        assertBuffer("test line 4", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 3", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 2", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 1", b = b.op(PREVIOUS_HISTORY));

        // beginning of history
        assertBuffer("test line 1", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 1", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 1", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 1", b = b.op(PREVIOUS_HISTORY));

        assertBuffer("test line 2", b = b.op(NEXT_HISTORY));
        assertBuffer("test line 3", b = b.op(NEXT_HISTORY));
        assertBuffer("test line 4", b = b.op(NEXT_HISTORY));
        assertBuffer("test line 5", b = b.op(NEXT_HISTORY));

        // end of history
        assertBuffer("", b = b.op(NEXT_HISTORY));
        assertBuffer("", b = b.op(NEXT_HISTORY));
        assertBuffer("", b = b.op(NEXT_HISTORY));

        assertBuffer("test line 5", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 4", b = b.op(PREVIOUS_HISTORY));
        b = b.op(BEGINNING_OF_LINE).append("XXX").op(ACCEPT_LINE);
        assertBuffer("XXXtest line 4", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 5", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 4", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("test line 5", b = b.op(NEXT_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(NEXT_HISTORY));
        assertBuffer("", b = b.op(NEXT_HISTORY));

        assertBuffer("XXXtest line 4", b = b.op(PREVIOUS_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(PREVIOUS_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(PREVIOUS_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(PREVIOUS_HISTORY));
        assertBuffer("XXXtest line 4", b = b.op(ACCEPT_LINE).op(PREVIOUS_HISTORY));
    }

    @Test
    public void testHistorySearchBackwardAndForward() throws Exception {
        KeyMap map = console.getKeys();

        // Map in HISTORY_SEARCH_BACKWARD.
        map.bind("\033[0A", HISTORY_SEARCH_BACKWARD);
        map.bind("\033[0B", HISTORY_SEARCH_FORWARD);

        Buffer b = new Buffer().
            append("toes").op(ACCEPT_LINE).
            append("the quick brown").op(ACCEPT_LINE).
            append("fox jumps").op(ACCEPT_LINE).
            append("over the").op(ACCEPT_LINE).
            append("lazy dog").op(ACCEPT_LINE).
            append("");

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
        assertBuffer("lazy dog", b = b.append("\033[0B"));

        // Try moving forward again.  We should be at our original input line,
        // which is just a plain 'l' at this point.
        assertBuffer("l", b = b.append("\033[0B"));

        // Now we should have more context and history-search-backward should
        // take us to "the quick brown" line.
        b = b.op(BACKWARD_DELETE_CHAR).append("t");
        assertBuffer("the quick brown", b = b.append("\033[0A"));

        // Try moving backward again.
        assertBuffer("toes", b = b.append("\033[0A"));

        assertBuffer("the quick brown", b = b.append("\033[0B"));

        b = b.op(BACKWARD_DELETE_CHAR);
        assertBuffer("fox jumps", b = b.append("\033[0B"));

        b = b.append("to");
        assertBuffer("toes", b = b.append("\033[0A"));
    }
}
