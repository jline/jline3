package jline.console;

import jline.TerminalFactory;
import jline.WindowsTerminal;
import static jline.console.Operation.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Tests for the {@link ConsoleReader}.
 */
public class ConsoleReaderTest
{
    @Before
    public void setUp() throws Exception {
        System.setProperty("jline.WindowsTerminal.directConsole", "false");
    }

    private void assertWindowsKeyBehavior(String expected, char[] input) throws Exception {
        StringBuilder buffer = new StringBuilder();
        buffer.append(input);
        ConsoleReader reader = createConsole(buffer.toString().getBytes());
        assertNotNull(reader);
        String line = reader.readLine();
        assertEquals(expected, line);
    }

    private ConsoleReader createConsole(byte[] bytes) throws Exception {
        InputStream in = new ByteArrayInputStream(bytes);
        Writer writer = new StringWriter();
        ConsoleReader reader = new ConsoleReader(in, writer);
        reader.setHistory(createSeededHistory());
        return reader;
    }

    private History createSeededHistory() {
        History history = new SimpleHistory();
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");
        return history;
    }
    
    @Test
    public void testDeleteAndBackspaceKeymappings() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        ConsoleReader consoleReader = new ConsoleReader();
        assertNotNull(consoleReader);
        assertEquals(127, consoleReader.getKeyForAction(DELETE_NEXT_CHAR));
        assertEquals(8, consoleReader.getKeyForAction(DELETE_PREV_CHAR));
    }

    @Test
    public void testReadline() throws Exception {
        ConsoleReader consoleReader = createConsole("Sample String\r\n".getBytes());
        assertNotNull(consoleReader);
        String line = consoleReader.readLine();
        assertEquals("Sample String", line);
    }

    @Test
    public void testDeleteOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                'S', 's',
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.LEFT_ARROW_KEY,
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.DELETE_KEY, '\r', 'n'
        };
        assertWindowsKeyBehavior("S", characters);
    }

    @Test
    public void testNumpadDeleteOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                'S', 's',
                WindowsTerminal.NUMPAD_KEY_INDICATOR,
                WindowsTerminal.LEFT_ARROW_KEY,
                WindowsTerminal.NUMPAD_KEY_INDICATOR,
                WindowsTerminal.DELETE_KEY, '\r', 'n'
        };
        assertWindowsKeyBehavior("S", characters);
    }

    @Test
    public void testHomeKeyOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                'S', 's',
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.HOME_KEY, 'x', '\r', '\n'
        };
        assertWindowsKeyBehavior("xSs", characters);

    }

    @Test
    public void testEndKeyOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                'S', 's',
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.HOME_KEY, 'x',
                WindowsTerminal.SPECIAL_KEY_INDICATOR, WindowsTerminal.END_KEY,
                'j', '\r', '\n'
        };
        assertWindowsKeyBehavior("xSsj", characters);
    }

    @Test
    public void testPageUpOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.PAGE_UP_KEY, '\r', '\n'
        };
        assertWindowsKeyBehavior("dir", characters);
    }

    @Test
    public void testPageDownOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.PAGE_DOWN_KEY, '\r', '\n'
        };
        assertWindowsKeyBehavior("mkdir monkey", characters);
    }

    @Test
    public void testEscapeOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                's', 's', 's',
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.ESCAPE_KEY, '\r', '\n'
        };
        assertWindowsKeyBehavior("", characters);
    }

    @Test
    public void testInsertOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
                'o', 'p', 's',
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.HOME_KEY,
                WindowsTerminal.SPECIAL_KEY_INDICATOR,
                WindowsTerminal.INSERT_KEY, 'o', 'o', 'p', 's', '\r', '\n'
        };
        assertWindowsKeyBehavior("oops", characters);
    }
}
