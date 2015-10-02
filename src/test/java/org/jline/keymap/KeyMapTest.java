/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.keymap;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.Console;
import org.jline.reader.ConsoleReaderImpl;
import org.jline.reader.DumbConsole;
import org.jline.reader.ReaderTestSupport.EofPipedInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.display;
import static org.jline.keymap.KeyMap.range;
import static org.jline.keymap.KeyMap.translate;
import static org.jline.reader.Operation.ABORT;
import static org.jline.reader.Operation.ACCEPT_LINE;
import static org.jline.reader.Operation.BACKWARD_WORD;
import static org.jline.reader.Operation.COMPLETE_WORD;
import static org.jline.reader.Operation.NEXT_HISTORY;
import static org.jline.reader.Operation.PREVIOUS_HISTORY;
import static org.jline.reader.Operation.UNIX_LINE_DISCARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class KeyMapTest {

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
    public void testBound() throws Exception {
        KeyMap map = new ConsoleReaderImpl(console).emacs();

        Assert.assertEquals(new Reference(COMPLETE_WORD), map.getBound("\u001B\u001B"));
        assertEquals(new Reference(BACKWARD_WORD), map.getBound(alt("b")));

        map.bindIfNotBound(new Reference(PREVIOUS_HISTORY), "\033[0A");
        assertEquals(new Reference(PREVIOUS_HISTORY), map.getBound("\033[0A"));

        map.bind(new Reference(NEXT_HISTORY), "\033[0AB");
        assertEquals(new Reference(PREVIOUS_HISTORY), map.getBound("\033[0A"));
        assertEquals(new Reference(NEXT_HISTORY), map.getBound("\033[0AB"));

        int[] remaining = new int[1];
        assertEquals(new Reference(COMPLETE_WORD), map.getBound("\u001B\u001Ba", remaining));
        assertEquals(1, remaining[0]);

        map.bind(new Reference("anotherkey"), translate("^Uc"));
        assertEquals(new Reference("anotherkey"), map.getBound(translate("^Uc"), remaining));
        assertEquals(0, remaining[0]);
        assertEquals(new Reference(UNIX_LINE_DISCARD), map.getBound(translate("^Ua"), remaining));
        assertEquals(1, remaining[0]);
    }

    @Test
    public void testRemaining() throws Exception {
        KeyMap map = new KeyMap();

        int[] remaining = new int[1];
        assertNull(map.getBound("ab", remaining));
        map.bind(new Reference(ABORT), "ab");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(ABORT), map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(new Reference(ABORT), map.getBound("abc", remaining));
        assertEquals(1, remaining[0]);

        map.bind(new Reference(ACCEPT_LINE), "abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(ABORT), map.getBound("ab", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(ABORT), map.getBound("abd", remaining));
        assertEquals(1, remaining[0]);
        assertEquals(new Reference(ACCEPT_LINE), map.getBound("abc", remaining));
        assertEquals(0, remaining[0]);

        map.unbind("abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(ABORT), map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(new Reference(ABORT), map.getBound("abc", remaining));
        assertEquals(1, remaining[0]);
    }

    @Test
    public void testSort() {
        List<String> strings = new ArrayList<>();
        strings.add("abc");
        strings.add("ab");
        strings.add("ad");
        Collections.sort(strings, KeyMap.KEYSEQ_COMPARATOR);
        assertEquals("ab", strings.get(0));
        assertEquals("ad", strings.get(1));
        assertEquals("abc", strings.get(2));
    }

    @Test
    public void testTranslate() {
        assertEquals("\\\u0007\b\u001b\u001b\f\n\r\t\u000b\u0053\u0045\u2345",
                translate("\\\\\\a\\b\\e\\E\\f\\n\\r\\t\\v\\123\\x45\\u2345"));
        assertEquals("\u0001\u0001\u0002\u0002\u0003\u0003\u007f^",
                translate("\\Ca\\CA\\C-B\\C-b^c^C^?^^"));
        assertEquals("\u001b3", translate("'\\e3'"));
        assertEquals("\u001b3", translate("\"\\e3\""));
    }

    @Test
    public void testDisplay() {
        assertEquals("\"\\\\^G^H^[^L^J^M^I\\u0098\\u2345\"",
                display("\\\u0007\b\u001b\f\n\r\t\u0098\u2345"));
        assertEquals("\"^A^B^C^?\\^\\\\\"",
                display("\u0001\u0002\u0003\u007f^\\"));
    }
    
    @Test
    public void testRange() {
        Collection<String> range = range("a^A-a^D");
        assertEquals(Arrays.asList(translate("a^A"), translate("a^B"), translate("a^C"), translate("a^D")), range);
    }

}
