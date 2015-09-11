package org.jline.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConsoleKeysTest extends ReaderTestSupport {

    @Test
    public void testInputBadConfig() throws Exception {
        ConsoleKeys keys = new ConsoleKeys("Bash", getClass().getResource("/org/jline/internal/config-bad"));
        assertEquals(new Macro("\u001bb\"\u001bf\""),
                keys.getKeys().getBound(ctrl('X') + "q"));
    }

    @Test
    public void testInput() throws Exception {
        ConsoleKeys keys = new ConsoleKeys("JLine", getClass().getResource("/org/jline/internal/config1"));
        assertEquals(Operation.UNIVERSAL_ARGUMENT, keys.getKeys().getBound("" + ctrl('U')));
        assertEquals(new Macro("Function Key \u2671"), keys.getKeys().getBound("\u001b[11~"));
        assertEquals(null, keys.getKeys().getBound(ctrl('X') + "q"));

        keys = new ConsoleKeys("bash", getClass().getResource("/org/jline/internal/config1"));
        assertEquals(new Macro("\u001bb\"\u001bf\""),
                keys.getKeys().getBound(ctrl('X') + "q"));
    }

    @Test
    public void testInput2() throws Exception {
        ConsoleKeys keys = new ConsoleKeys("Bash", getClass().getResource("/org/jline/internal/config2"));
        assertNotNull(keys.getKeys().getBound("\u001b" + ctrl('V')));
    }

}
