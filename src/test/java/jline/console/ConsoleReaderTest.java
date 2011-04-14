package jline.console;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import jline.TerminalFactory;
import jline.WindowsTerminal;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import jline.internal.Configuration;
import org.junit.Before;
import org.junit.Test;

import static jline.WindowsTerminal.WindowsKey.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the {@link ConsoleReader}.
 */
public class ConsoleReaderTest
{
    @Before
    public void setUp() throws Exception {
        TerminalFactory.configure(TerminalFactory.AUTO);
        TerminalFactory.reset();
        System.setProperty(WindowsTerminal.JLINE_WINDOWS_TERMINAL_DIRECT_CONSOLE, "false");
        Configuration.getConfig(getClass().getResource("/jline/empty-config"));
    }

    private void assertWindowsKeyBehavior(String expected, char[] input) throws Exception {
        StringBuilder buffer = new StringBuilder();
        buffer.append(input);
        ConsoleReader reader = createConsole(buffer.toString());
        assertNotNull(reader);
        String line = reader.readLine();
        assertEquals(expected, line);
    }

    private ConsoleReader createConsole(String chars) throws Exception {
        System.err.println(Configuration.getEncoding());
        System.err.println(chars);
        return createConsole(chars.getBytes(Configuration.getEncoding()));
    }

    private ConsoleReader createConsole(byte[] bytes) throws Exception {
        return createConsole(null, bytes);
    }
    private ConsoleReader createConsole(String appName, byte[] bytes) throws Exception {
        InputStream in = new ByteArrayInputStream(bytes);
        ConsoleReader reader = new ConsoleReader(appName, in, new ByteArrayOutputStream(), null);
        reader.setHistory(createSeededHistory());
        return reader;
    }

    private History createSeededHistory() {
        History history = new MemoryHistory();
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");
        return history;
    }

    @Test
    public void testReadline() throws Exception {
        ConsoleReader consoleReader = createConsole("Sample String\r\n");
        assertNotNull(consoleReader);
        String line = consoleReader.readLine();
        assertEquals("Sample String", line);
    }

    @Test
    public void testReadlineWithUnicode() throws Exception {
        System.setProperty("input.encoding", "UTF-8");
        ConsoleReader consoleReader = createConsole("\u6771\u00E9\u00E8\r\n");
        assertNotNull(consoleReader);
        String line = consoleReader.readLine();
        assertEquals("\u6771\u00E9\u00E8", line);
    }

    @Test
    public void testDeleteOnWindowsTerminal() throws Exception {
        // test only works on Windows
        if (!(TerminalFactory.get() instanceof WindowsTerminal)) {
            return;
        }

        char[] characters = new char[]{
            'S', 's',
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) LEFT_ARROW_KEY.code,
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) DELETE_KEY.code, '\r', 'n'
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
            (char) NUMPAD_KEY_INDICATOR.code,
            (char) LEFT_ARROW_KEY.code,
            (char) NUMPAD_KEY_INDICATOR.code,
            (char) DELETE_KEY.code, '\r', 'n'
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
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) HOME_KEY.code, 'x', '\r', '\n'
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
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) HOME_KEY.code, 'x',
            (char) SPECIAL_KEY_INDICATOR.code, (char) END_KEY.code,
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
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) PAGE_UP_KEY.code, '\r', '\n'
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
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) PAGE_DOWN_KEY.code, '\r', '\n'
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
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) ESCAPE_KEY.code, '\r', '\n'
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
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) HOME_KEY.code,
            (char) SPECIAL_KEY_INDICATOR.code,
            (char) INSERT_KEY.code, 'o', 'o', 'p', 's', '\r', '\n'
        };
        assertWindowsKeyBehavior("oops", characters);
    }

    @Test
    public void testExpansion() throws Exception {
        ConsoleReader reader = new ConsoleReader();
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(3);
        history.add("foo");
        history.add("dir");
        history.add("cd c:\\");
        history.add("mkdir monkey");
        reader.setHistory(history);

        assertEquals("echo a!", reader.expandEvents("echo a!"));
        assertEquals("mkdir monkey ; echo a!", reader.expandEvents("!! ; echo a!"));
        assertEquals("echo ! a", reader.expandEvents("echo ! a"));
        assertEquals("echo !\ta", reader.expandEvents("echo !\ta"));

        assertEquals("mkdir barey", reader.expandEvents("^monk^bar^"));
        assertEquals("mkdir barey", reader.expandEvents("^monk^bar"));
        assertEquals("a^monk^bar", reader.expandEvents("a^monk^bar"));

        assertEquals("mkdir monkey", reader.expandEvents("!!"));
        assertEquals("echo echo a", reader.expandEvents("echo !#a"));

        assertEquals("mkdir monkey", reader.expandEvents("!mk"));
        try {
            reader.expandEvents("!mz");
        } catch (IllegalArgumentException e) {
            assertEquals("!mz: event not found", e.getMessage());
        }

        assertEquals("mkdir monkey", reader.expandEvents("!?mo"));
        assertEquals("mkdir monkey", reader.expandEvents("!?mo?"));

        assertEquals("mkdir monkey", reader.expandEvents("!-1"));
        assertEquals("cd c:\\", reader.expandEvents("!-2"));
        assertEquals("cd c:\\", reader.expandEvents("!2"));
        assertEquals("mkdir monkey", reader.expandEvents("!3"));
        try {
            reader.expandEvents("!20");
        } catch (IllegalArgumentException e) {
            assertEquals("!20: event not found", e.getMessage());
        }
        try {
            reader.expandEvents("!-20");
        } catch (IllegalArgumentException e) {
            assertEquals("!-20: event not found", e.getMessage());
        }
    }

    @Test
    public void testMacro() throws Exception {
        ConsoleReader consoleReader = createConsole("\u0018(foo\u0018)\u0018e\r\n");
        assertNotNull(consoleReader);
        String line = consoleReader.readLine();
        assertEquals("foofoo", line);
    }

    @Test
    public void testInput() throws Exception {
        System.setProperty(Configuration.JLINE_INPUTRC, getClass().getResource("/jline/internal/config1").toExternalForm());
        try {
            ConsoleReader consoleReader = createConsole("\u0018(foo\u0018)\u0018e\r\n");
            assertNotNull(consoleReader);

            assertEquals(Operation.UNIVERSAL_ARGUMENT, consoleReader.getKeys().getBound("" + ((char)('U' - 'A' + 1))));
            assertEquals("Function Key \u2671", consoleReader.getKeys().getBound("\u001b[11~"));
            assertEquals(null, consoleReader.getKeys().getBound(((char)('X' - 'A' + 1)) + "q"));

            consoleReader = createConsole("bash", new byte[0]);
            assertNotNull(consoleReader);
            assertEquals("\u001bb\"\u001bf\"", consoleReader.getKeys().getBound(((char)('X' - 'A' + 1)) + "q"));
        } finally {
            System.clearProperty(Configuration.JLINE_INPUTRC);
        }
    }

    @Test
    public void testInput2() throws Exception {
        System.setProperty(Configuration.JLINE_INPUTRC, getClass().getResource("/jline/internal/config2").toExternalForm());
        try {
            ConsoleReader consoleReader = createConsole("Bash", new byte[0]);
            assertNotNull(consoleReader);
            assertNotNull(consoleReader.getKeys().getBound("\u001b" + ((char)('V' - 'A' + 1))));

        } finally {
            System.clearProperty(Configuration.JLINE_INPUTRC);
        }
    }

}
