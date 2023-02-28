/*
 * Copyright (c) 2002-2016, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
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

import org.jline.reader.Binding;
import org.jline.reader.Reference;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.ReaderTestSupport.EofPipedInputStream;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.display;
import static org.jline.keymap.KeyMap.range;
import static org.jline.keymap.KeyMap.translate;
import static org.jline.reader.LineReader.ACCEPT_LINE;
import static org.jline.reader.LineReader.BACKWARD_WORD;
import static org.jline.reader.LineReader.COMPLETE_WORD;
import static org.jline.reader.LineReader.DOWN_HISTORY;
import static org.jline.reader.LineReader.KILL_WHOLE_LINE;
import static org.jline.reader.LineReader.SEND_BREAK;
import static org.jline.reader.LineReader.UP_HISTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KeyMapTest {

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
        terminal = new DumbTerminal(in, out);
        terminal.setSize(new Size(160, 80));
    }

    @Test
    public void testBound() throws Exception {
        KeyMap<Binding> map = new LineReaderImpl(terminal).emacs();

        Assert.assertEquals(new Reference(COMPLETE_WORD), map.getBound("\u001B\u001B"));
        assertEquals(new Reference(BACKWARD_WORD), map.getBound(alt("b")));

        map.bindIfNotBound(new Reference(UP_HISTORY), "\033[0A");
        assertEquals(new Reference(UP_HISTORY), map.getBound("\033[0A"));

        map.bind(new Reference(DOWN_HISTORY), "\033[0AB");
        assertEquals(new Reference(UP_HISTORY), map.getBound("\033[0A"));
        assertEquals(new Reference(DOWN_HISTORY), map.getBound("\033[0AB"));

        int[] remaining = new int[1];
        assertEquals(new Reference(COMPLETE_WORD), map.getBound("\u001B\u001Ba", remaining));
        assertEquals(1, remaining[0]);

        map.bind(new Reference("anotherkey"), translate("^Uc"));
        assertEquals(new Reference("anotherkey"), map.getBound(translate("^Uc"), remaining));
        assertEquals(0, remaining[0]);
        assertEquals(new Reference(KILL_WHOLE_LINE), map.getBound(translate("^Ua"), remaining));
        assertEquals(1, remaining[0]);
    }

    @Test
    public void testRemaining() throws Exception {
        KeyMap<Binding> map = new KeyMap<>();

        int[] remaining = new int[1];
        assertNull(map.getBound("ab", remaining));
        map.bind(new Reference(SEND_BREAK), "ab");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(SEND_BREAK), map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(new Reference(SEND_BREAK), map.getBound("abc", remaining));
        assertEquals(1, remaining[0]);

        map.bind(new Reference(ACCEPT_LINE), "abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(SEND_BREAK), map.getBound("ab", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(SEND_BREAK), map.getBound("abd", remaining));
        assertEquals(1, remaining[0]);
        assertEquals(new Reference(ACCEPT_LINE), map.getBound("abc", remaining));
        assertEquals(0, remaining[0]);

        map.unbind("abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(new Reference(SEND_BREAK), map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(new Reference(SEND_BREAK), map.getBound("abc", remaining));
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
        assertEquals(
                "\\\u0007\b\u001b\u001b\f\n\r\t\u000b\u0053\u0045\u2345",
                translate("\\\\\\a\\b\\e\\E\\f\\n\\r\\t\\v\\123\\x45\\u2345"));
        assertEquals("\u0001\u0001\u0002\u0002\u0003\u0003\u007f^", translate("\\Ca\\CA\\C-B\\C-b^c^C^?^^"));
        assertEquals("\u001b3", translate("'\\e3'"));
        assertEquals("\u001b3", translate("\"\\e3\""));
    }

    @Test
    public void testDisplay() {
        assertEquals("\"\\\\^G^H^[^L^J^M^I\\u0098\\u2345\"", display("\\\u0007\b\u001b\f\n\r\t\u0098\u2345"));
        assertEquals("\"^A^B^C^?\\^\\\\\"", display("\u0001\u0002\u0003\u007f^\\"));
    }

    @Test
    public void testRange() {
        Collection<String> range = range("a^A-a^D");
        assertEquals(Arrays.asList(translate("a^A"), translate("a^B"), translate("a^C"), translate("a^D")), range);
    }
}
