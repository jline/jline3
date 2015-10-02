/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.keymap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.Console;
import org.jline.reader.DumbConsole;
import org.jline.reader.ReaderTestSupport.EofPipedInputStream;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BindingReaderTest {

    protected Console console;
    protected EofPipedInputStream in;
    protected ByteArrayOutputStream out;

    @Before
    public void setUp() throws Exception {
        Handler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        Logger logger = Logger.getLogger("org.jline");
        logger.addHandler(ch);
        // Set the handler log level
        logger.setLevel(Level.INFO);

        in = new EofPipedInputStream();
        out = new ByteArrayOutputStream();
        console = new DumbConsole(null, in, out);
    }

    @Test
    public void testBindingReaderNoUnicode() {
        in.setIn(new ByteArrayInputStream("\uD834\uDD21abc".getBytes()));
        BindingReader reader = new BindingReader(console, null);
        KeyMap keyMap = new KeyMap();
        keyMap.bind("b", new WidgetRef("foo"));
        assertEquals(new WidgetRef("foo"), reader.readBinding(keyMap));
        assertEquals("b", reader.getLastBinding());
        assertNull(reader.readBinding(keyMap));
    }

    @Test
    public void testBindingReaderUnicode() {
        in.setIn(new ByteArrayInputStream("\uD834\uDD21abc".getBytes()));
        BindingReader reader = new BindingReader(console, new WidgetRef("insert"));
        KeyMap keyMap = new KeyMap();
        keyMap.bind("b", new WidgetRef("foo"));
        assertEquals(new WidgetRef("insert"), reader.readBinding(keyMap));
        assertEquals("\uD834\uDD21", reader.getLastBinding());
        assertEquals(new WidgetRef("foo"), reader.readBinding(keyMap));
        assertEquals("b", reader.getLastBinding());
        assertNull(reader.readBinding(keyMap));
    }
}
