/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.keymap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jline.reader.ConsoleReaderImpl;
import org.jline.reader.Operation;
import org.junit.Assert;
import org.junit.Test;

import static org.jline.keymap.KeyMap.display;
import static org.jline.keymap.KeyMap.range;
import static org.jline.keymap.KeyMap.translate;
import static org.jline.reader.ConsoleReaderImpl.CTRL_OB;
import static org.jline.reader.ConsoleReaderImpl.CTRL_U;
import static org.jline.reader.ConsoleReaderImpl.ESCAPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class KeyMapTest {

    @Test
    public void testBound() throws Exception {
        KeyMap map = ConsoleReaderImpl.emacs();

        Assert.assertEquals(Operation.COMPLETE_WORD, map.getBound("\u001B" + CTRL_OB));
        assertEquals(Operation.BACKWARD_WORD, map.getBound(ESCAPE + "b"));

        map.bindIfNotBound("\033[0A", Operation.PREVIOUS_HISTORY);
        assertEquals(Operation.PREVIOUS_HISTORY, map.getBound("\033[0A"));

        map.bind("\033[0AB", Operation.NEXT_HISTORY);
        assertEquals(Operation.PREVIOUS_HISTORY, map.getBound("\033[0A"));
        assertEquals(Operation.NEXT_HISTORY, map.getBound("\033[0AB"));

        int[] remaining = new int[1];
        assertEquals(Operation.COMPLETE_WORD, map.getBound("\u001B" + CTRL_OB + "a", remaining));
        assertEquals(1, remaining[0]);

        map.bind(CTRL_U + "c", new Reference("anotherkey"));
        assertEquals(new Reference("anotherkey"), map.getBound(CTRL_U + "c", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(Operation.UNIX_LINE_DISCARD, map.getBound(CTRL_U + "a", remaining));
        assertEquals(1, remaining[0]);
    }

    @Test
    public void testRemaining() throws Exception {
        KeyMap map = new KeyMap();

        int[] remaining = new int[1];
        assertNull(map.getBound("ab", remaining));
        map.bind("ab", Operation.ABORT);
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(Operation.ABORT, map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(Operation.ABORT, map.getBound("abc", remaining));
        assertEquals(1, remaining[0]);

        map.bind("abc", Operation.ACCEPT_LINE);
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(Operation.ABORT, map.getBound("ab", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(Operation.ABORT, map.getBound("abd", remaining));
        assertEquals(1, remaining[0]);
        assertEquals(Operation.ACCEPT_LINE, map.getBound("abc", remaining));
        assertEquals(0, remaining[0]);

        map.unbind("abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(Operation.ABORT, map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(Operation.ABORT, map.getBound("abc", remaining));
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
