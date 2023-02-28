/*
 * Copyright (c) 2002-2016, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.keymap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.reader.Binding;
import org.jline.reader.Reference;
import org.jline.reader.impl.ReaderTestSupport.EofPipedInputStream;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BindingReaderTest {

    protected Terminal terminal;
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
        terminal = new DumbTerminal("dumb", "dumb", in, out, StandardCharsets.UTF_8);
        terminal.setSize(new Size(160, 80));
    }

    @Test
    public void testBindingReaderNoUnicode() {
        in.setIn(new ByteArrayInputStream("\uD834\uDD21abc".getBytes(StandardCharsets.UTF_8)));
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Binding> keyMap = new KeyMap<>();
        keyMap.bind(new Reference("foo"), "b");
        assertEquals(new Reference("foo"), reader.readBinding(keyMap));
        assertEquals("b", reader.getLastBinding());
        assertNull(reader.readBinding(keyMap));
    }

    @Test
    public void testBindingReaderUnicode() {
        in.setIn(new ByteArrayInputStream("\uD834\uDD21abc".getBytes(StandardCharsets.UTF_8)));
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Binding> keyMap = new KeyMap<>();
        keyMap.setUnicode(new Reference("insert"));
        keyMap.bind(new Reference("foo"), "b");
        assertEquals(new Reference("insert"), reader.readBinding(keyMap));
        assertEquals("\uD834\uDD21", reader.getLastBinding());
        assertEquals(new Reference("foo"), reader.readBinding(keyMap));
        assertEquals("b", reader.getLastBinding());
        assertNull(reader.readBinding(keyMap));
    }

    @Test
    public void testBindingReaderReadString() {
        in.setIn(new ByteArrayInputStream("\uD834\uDD21abc0123456789defg".getBytes(StandardCharsets.UTF_8)));
        BindingReader reader = new BindingReader(terminal.reader());
        String str = reader.readStringUntil("fg");
        assertEquals("\uD834\uDD21abc0123456789de", str);
    }
}
