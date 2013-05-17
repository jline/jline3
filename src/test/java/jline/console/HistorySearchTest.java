package jline.console;

import jline.console.history.MemoryHistory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HistorySearchTest {
    private ConsoleReader reader;
    private ByteArrayOutputStream output;

    @Before
    public void setUp() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[]{});
        output = new ByteArrayOutputStream();
        reader = new ConsoleReader("test console reader", in, output, null);
    }

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

        String readLineResult;
        reader.setInput(new ByteArrayInputStream(new byte[]{KeyMap.CTRL_R, 'f', '\n'}));
        readLineResult = reader.readLine();
        assertEquals("faddle", readLineResult);
        assertEquals(3, history.size());

        reader.setInput(new ByteArrayInputStream(new byte[]{
                KeyMap.CTRL_R, 'f', KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("foo", readLineResult);
        assertEquals(4, history.size());

        reader.setInput(new ByteArrayInputStream(new byte[]{KeyMap.CTRL_R, 'f', KeyMap.CTRL_R, KeyMap.CTRL_R, '\n'}));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(5, history.size());
    }

    @Test
    public void testForwardHistorySearch() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        reader.setInput(new ByteArrayInputStream(new byte[]{
                KeyMap.CTRL_R, 'f', KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(4, history.size());

        reader.setInput(new ByteArrayInputStream(new byte[]{
                KeyMap.CTRL_R, 'f', KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_S, KeyMap.CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("faddle", readLineResult);
        assertEquals(5, history.size());

        reader.setInput(new ByteArrayInputStream(new byte[]{
                KeyMap.CTRL_R, 'f', KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(6, history.size());
    }

    @Test
    public void testSearchHistoryAfterHittingEnd() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        reader.setInput(new ByteArrayInputStream(new byte[]{
                KeyMap.CTRL_R, 'f', KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_R, KeyMap.CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("fiddle", readLineResult);
        assertEquals(4, history.size());
    }

    @Test
    public void testSearchHistoryWithNoMatches() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        reader.setInput(new ByteArrayInputStream(new byte[]{
                'x', KeyMap.CTRL_S, KeyMap.CTRL_S, '\n'
        }));
        readLineResult = reader.readLine();
        assertEquals("", readLineResult);
        assertEquals(3, history.size());
    }

    @Test
    public void testAbortingSearchRetainsCurrentBufferAndPrintsDetails() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        reader.setInput(new ByteArrayInputStream(new byte[]{
                'f', KeyMap.CTRL_R, 'f', KeyMap.CTRL_G
        }));
        readLineResult = reader.readLine();
        assertEquals(null, readLineResult);
        assertTrue(output.toString().contains("(reverse-i-search)`ff':"));
        assertEquals("ff", reader.getCursorBuffer().toString());
        assertEquals(3, history.size());
    }

    @Test
    public void testAbortingAfterSearchingPreviousLinesGivesBlank() throws Exception {
        MemoryHistory history = setupHistory();

        String readLineResult;
        reader.setInput(new ByteArrayInputStream(new byte[]{
                'f', KeyMap.CTRL_R, 'f', '\n',
                'f', 'o', 'o', KeyMap.CTRL_G
        }));
        readLineResult = reader.readLine();
        assertEquals("", readLineResult);

        readLineResult = reader.readLine();
        assertEquals(null, readLineResult);
        assertEquals("", reader.getCursorBuffer().toString());
        assertEquals(3, history.size());
    }
}
