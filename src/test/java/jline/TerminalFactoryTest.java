/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

import jline.internal.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

/**
 * Tests for the {@link TerminalFactory}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TerminalFactory.class, Configuration.class})
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
            System.clearProperty(TerminalFactory.JLINE_TERMINAL);
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
            System.clearProperty(TerminalFactory.JLINE_TERMINAL);
        }
    }

    @Test
    public void testConfigureDumbTerminalEmacs() {
        mockStaticPartial(System.class, "getenv");
        mockStaticPartial(Configuration.class, "getOsName");

        String osString = System.getProperty("os.name").toLowerCase();
        String expectedTerminalClassName = osString.contains("windows") ?
                "jline.AnsiWindowsTerminal" :
                "jline.UnixTerminal";
        expect(Configuration.getOsName()).andReturn(osString).anyTimes();

        expect(System.getenv("TERM")).andReturn("dumb");
        expect(System.getenv("EMACS")).andReturn("t");
        expect(System.getenv("INSIDE_EMACS")).andReturn("24.3.1,comint");
        expect(System.getenv("OSV_CPUS")).andReturn(null);
        replayAll();

        Terminal t = TerminalFactory.get();
        verifyAll();
        assertNotNull(t);
        assertEquals(expectedTerminalClassName, t.getClass().getName());
    }

    @Test
    public void testConfigureDumbTerminalNoEmacs() {
        mockStaticPartial(System.class, "getenv");

        expect(System.getenv("TERM")).andReturn("dumb");
        expect(System.getenv("EMACS")).andReturn(null);
        expect(System.getenv("INSIDE_EMACS")).andReturn(null);
        replayAll();

        Terminal t = TerminalFactory.get();
        verifyAll();
        assertNotNull(t);
        assertEquals(UnsupportedTerminal.class.getName(), t.getClass().getName());
    }
}