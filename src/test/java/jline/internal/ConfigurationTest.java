/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.internal;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link Configuration}.
 */
public class ConfigurationTest
{
    @Test
    public void initFromSystemProperty() {
        System.setProperty(Configuration.JLINE_CONFIGURATION, getClass().getResource("jlinerc1").toExternalForm());
        Configuration.reset();
        String value = Configuration.getString("a");
        assertEquals("b", value);
    }

    @Test
    public void getBooleanFromSystemProperty() {
        System.setProperty("test", "false");
        boolean value = Configuration.getBoolean("test", true);
        assertEquals(false, value);
    }

    @Test
    public void getIntegerFromSystemProperty() {
        System.setProperty("test", "1234");
        int value = Configuration.getInteger("test", 5678);
        assertEquals(1234, value);
    }

    @Test
    public void getIntegerUsingDefault() {
        System.getProperties().remove("test");
        int value = Configuration.getInteger("test", 1234);
        assertEquals(1234, value);
    }

    @Test
    public void resetReconfigures() {
        System.setProperty(Configuration.JLINE_CONFIGURATION, getClass().getResource("jlinerc1").toExternalForm());
        Configuration.reset();
        String value1 = Configuration.getString("a");
        assertEquals("b", value1);

        System.setProperty(Configuration.JLINE_CONFIGURATION, getClass().getResource("jlinerc2").toExternalForm());
        Configuration.reset();
        String value2 = Configuration.getString("c");
        assertEquals("d", value2);
    }

    @Test
    public void parseCtypeNull() {
        assertNull(Configuration.extractEncodingFromCtype(null));
    }

    @Test
    public void parseCtypeBlank() {
        assertNull(Configuration.extractEncodingFromCtype(""));
    }

    @Test
    public void parseCtypeNoEncoding() {
        assertNull(Configuration.extractEncodingFromCtype("fr_FR"));
    }

    @Test
    public void parseCtypeNoEncodingWithModifier() {
        assertNull(Configuration.extractEncodingFromCtype("fr_FR@euro"));
    }

    @Test
    public void parseCtypeWithEncoding() {
        assertEquals("UTF-8", Configuration.extractEncodingFromCtype("fr_FR.UTF-8"));
    }

    @Test
    public void parseCtypeWithEncodingAndModifier() {
        assertEquals("UTF-8", Configuration.extractEncodingFromCtype("fr_FR.UTF-8@euro"));
    }


}