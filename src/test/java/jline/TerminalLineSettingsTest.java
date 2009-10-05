package jline;

import jline.internal.TerminalLineSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link TerminalLineSettings}.
 */
public class TerminalLineSettingsTest
{
    private TerminalLineSettings settings;

    @Before
    public void setUp() throws Exception {
        settings = new TerminalLineSettings();
    }

    @Test
    public void testGetConfig() {
        String config = settings.getConfig();
        System.out.println(config);
    }
}