package jline.internal;

import jline.console.Operation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigurationTest {

    @Test
    public void testInput() throws Exception {
        System.setProperty(Configuration.JLINE_INPUTRC, getClass().getResource("config1").toExternalForm());
        try {
            Configuration config = new Configuration("JLine");
            assertEquals(Operation.UNIVERSAL_ARGUMENT, config.getKeys().getBound("" + ((char)('U' - 'A' + 1))));
            assertEquals("Function Key \u2671", config.getKeys().getBound("\u001b[11~"));
            assertEquals(null, config.getKeys().getBound(((char)('X' - 'A' + 1)) + "q"));

            config = new Configuration("bash", getClass().getResource("config1"));
            assertEquals("\u001bb\"\u001bf\"", config.getKeys().getBound(((char)('X' - 'A' + 1)) + "q"));
        } finally {
            System.clearProperty(Configuration.JLINE_INPUTRC);
        }
    }

    @Test
    public void testInput2() throws Exception {
        System.setProperty(Configuration.JLINE_INPUTRC, getClass().getResource("config2").toExternalForm());
        try {
            Configuration config = new Configuration("JLine");
        } finally {
            System.clearProperty(Configuration.JLINE_INPUTRC);
        }
    }

}
