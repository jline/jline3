package jline.internal;

import jline.console.Operation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigurationTest {

    @Test
    public void testInput() throws Exception {
        Configuration config = new Configuration("JLine", getClass().getResource("config1"));
        assertEquals(Operation.UNIVERSAL_ARGUMENT, config.getKeys().getBound("" + ((char)('U' - 'A' + 1))));
        assertEquals("Function Key \u2671", config.getKeys().getBound("\u001b[11~"));
        assertEquals(null, config.getKeys().getBound(((char)('X' - 'A' + 1)) + "q"));

        config = new Configuration("bash", getClass().getResource("config1"));
        assertEquals("\u001bb\"\u001bf\"", config.getKeys().getBound(((char)('X' - 'A' + 1)) + "q"));
    }

    @Test
    public void testInput2() throws Exception {
        Configuration config = new Configuration("JLine", getClass().getResource("config2"));
    }

}
