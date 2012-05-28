/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the {@link TerminalFactory}.
 */
public class TerminalFactoryTest
{
    @Before
    public void setUp() throws Exception {
        TerminalFactory.reset();
    }

    @After
    public void tearDown() throws Exception {
        TerminalFactory.reset();
    }

    @Test
    public void testConfigureNone() {
        try {
            TerminalFactory.configure(TerminalFactory.NONE);
            Terminal t = TerminalFactory.get();
            assertNotNull(t);
            assertEquals(UnsupportedTerminal.class.getName(), t.getClass().getName());
        } finally {
            TerminalFactory.configure(TerminalFactory.AUTO);
        }
    }

    @Test
    public void testConfigureUnsupportedTerminal() {
        try {
            TerminalFactory.configure(UnsupportedTerminal.class.getName());
            Terminal t = TerminalFactory.get();
            assertNotNull(t);
            assertEquals(UnsupportedTerminal.class.getName(), t.getClass().getName());
        } finally {
            TerminalFactory.configure(TerminalFactory.AUTO);
        }
    }
}