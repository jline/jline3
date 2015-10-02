package org.jline.reader;

import java.io.ByteArrayInputStream;

import org.jline.reader.history.MemoryHistory;
import org.junit.Test;

import static org.jline.reader.ConsoleReaderImpl.CTRL_G;
import static org.jline.reader.ConsoleReaderImpl.CTRL_R;
import static org.jline.reader.ConsoleReaderImpl.CTRL_S;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistorySearchTest extends ReaderTestSupport {

    private MemoryHistory setupHistory() {
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(10);
        history.add("foo");
        history.add("fiddle");
        history.add("faddle");
        reader.setHistory(history);
        return history;
    }

    @Test
    public void testReverseHistorySearch() throws Exception {
        MemoryHistory history = setupHistory();

        // TODO: use assertBuffer
        String readLineResult;
        in.setIn(new ByteArrayInputStream(new byte[]{CTRL_R, 'f', '\n'}));
        readLineResult = reader.readLine();
        assertEquals("faddle", readLineResult);
        assertEquals(3, history.size());

        in.setIn(new ByteArrayInputStream(new byte[]{
                CTRL_R, 'f', CTRL_R, CTRL_R, CTRL_R, CTRL_R, CTRL_R, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("foo", readLineResult);
        assertEquals(4, history.size());

        in.setIn(new ByteArrayInputStream(new byte[]{CTRL_R, 'f', CTRL_R, CTRL_R, '\n'}));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(5, history.size());
    }

    @Test
    public void testForwardHistorySearch() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        in.setIn(new ByteArrayInputStream(new byte[]{
                CTRL_R, 'f', CTRL_R, CTRL_R, CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(4, history.size());

        in.setIn(new ByteArrayInputStream(new byte[]{
                CTRL_R, 'f', CTRL_R, CTRL_R, CTRL_R, CTRL_S, CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("faddle", readLineResult);
        assertEquals(5, history.size());

        in.setIn(new ByteArrayInputStream(new byte[]{
                CTRL_R, 'f', CTRL_R, CTRL_R, CTRL_R, CTRL_R, CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(6, history.size());
    }

    @Test
    public void testSearchHistoryAfterHittingEnd() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        in.setIn(new ByteArrayInputStream(new byte[]{
                CTRL_R, 'f', CTRL_R, CTRL_R, CTRL_R, CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(4, history.size());
    }

    @Test
    public void testSearchHistoryWithNoMatches() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        in.setIn(new ByteArrayInputStream(new byte[]{
                'x', CTRL_S, CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("", readLineResult);
        assertEquals(3, history.size());
    }

    @Test
    public void testAbortingSearchRetainsCurrentBufferAndPrintsDetails() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        in.setIn(new ByteArrayInputStream(new byte[]{
                'f', CTRL_R, 'f', CTRL_G
        }));
        readLineResult = reader.readLine();
        assertEquals(null, readLineResult);
        assertTrue(out.toString().contains("bck-i-search: f_"));
        assertFalse(out.toString().contains("bck-i-search: ff_"));
        assertEquals("f", reader.getCursorBuffer().toString());
        assertEquals(3, history.size());
    }

    @Test
    public void testAbortingAfterSearchingPreviousLinesGivesBlank() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        in.setIn(new ByteArrayInputStream(new byte[]{
                'f', CTRL_R, 'f', '\n',
                'f', 'o', 'o', CTRL_G
        }));
        readLineResult = reader.readLine();
        assertEquals("", readLineResult);

        readLineResult = reader.readLine();
        assertEquals(null, readLineResult);
        assertEquals("", reader.getCursorBuffer().toString());
        assertEquals(3, history.size());
    }
}
