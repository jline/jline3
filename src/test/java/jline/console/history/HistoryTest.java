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
}
