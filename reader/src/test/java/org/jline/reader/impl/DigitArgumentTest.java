/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.Reference;
import org.junit.jupiter.api.Test;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.translate;
import static org.jline.reader.impl.LineReaderImpl.EMACS;

public class DigitArgumentTest extends ReaderTestSupport {

    @Test
    public void testMoveChar() throws Exception {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer("0123456789"))
                .append(alt('8'))
                .append(ctrl('B'))
                .append(alt('2'))
                .append(ctrl('F'))
                .append(ctrl('K'))
                .enter();
        assertLine("0123", b, true);
    }

    @Test
    public void testSelfInsert() throws Exception {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer())
                .append(alt('4'))
                .append("0")
                .append(alt('2'))
                .append(alt('\r'))
                .enter();
        assertLine("0000\n\n", b, true);
    }

    @Test
    public void testMoveWord() throws Exception {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer("abc def ghi klm nop"))
                .append(alt('2'))
                .append(alt('b'))
                .append(alt('-'))
                .append(alt('2'))
                .append(alt('f'))
                .append(ctrl('K'))
                .enter();
        assertLine("abc ", b, true);
    }

    @Test
    public void testCaseTransform() throws Exception {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer("abc def ghi klm nop"))
                .append(ctrl('A'))
                .append(alt('3'))
                .append(alt('u'))
                .append(alt('b'))
                .append(alt('3'))
                .append(alt('c'))
                .append(alt('b'))
                .append(alt('l'))
                .enter();
        assertLine("ABC DEF Ghi Klm nop", b, true);
    }

    @Test
    public void testTransposeChars() throws Exception {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('2'))
                .append(alt('b'))
                .append(ctrl('T'))
                .enter();
        assertLine("bacd\nefgh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('2'))
                .append(alt('b'))
                .append(alt('2'))
                .append(ctrl('T'))
                .enter();
        assertLine("bcad\nefgh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('2'))
                .append(alt('b'))
                .append(alt('3'))
                .append(ctrl('T'))
                .enter();
        assertLine("bcda\nefgh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('2'))
                .append(alt('b'))
                .append(alt('4'))
                .append(ctrl('T'))
                .enter();
        assertLine("bcad\nefgh", b, true);

        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nfegh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(alt('2'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nfgeh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(alt('3'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nfghe", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(alt('4'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nfgeh", b, true);

        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(ctrl('T'))
                .enter();
        assertLine("abdc\nefgh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(alt('2'))
                .append(ctrl('T'))
                .enter();
        assertLine("adbc\nefgh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(alt('3'))
                .append(ctrl('T'))
                .enter();
        assertLine("dabc\nefgh", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('b'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(alt('4'))
                .append(ctrl('T'))
                .enter();
        assertLine("adbc\nefgh", b, true);

        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('-'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nefhg", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('-'))
                .append(alt('2'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nehfg", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('-'))
                .append(alt('3'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nhefg", b, true);
        b = (new TestBuffer(translate("abcd\\E\refgh")))
                .append(alt('-'))
                .append(alt('4'))
                .append(ctrl('T'))
                .enter();
        assertLine("abcd\nehfg", b, true);
    }

    @Test
    public void testTransposeWords() throws Exception {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('t'))
                .enter();
        assertLine("def abc ghi\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('2'))
                .append(alt('t'))
                .enter();
        assertLine("def ghi abc\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('3'))
                .append(alt('t'))
                .enter();
        assertLine("def abc ghi\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(ctrl('A'))
                .append(alt('t'))
                .enter();
        assertLine("abc def ghi\nnop klm qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(ctrl('A'))
                .append(alt('2'))
                .append(alt('t'))
                .enter();
        assertLine("abc def ghi\nnop qrs klm", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(ctrl('A'))
                .append(alt('3'))
                .append(alt('t'))
                .enter();
        assertLine("abc def ghi\nnop klm qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(ctrl('A'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(alt('t'))
                .enter();
        assertLine("abc ghi def\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(ctrl('A'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(alt('2'))
                .append(alt('t'))
                .enter();
        assertLine("ghi abc def\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(ctrl('A'))
                .append(ctrl('B'))
                .append(alt('-'))
                .append(alt('3'))
                .append(alt('t'))
                .enter();
        assertLine("abc ghi def\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('-'))
                .append(alt('t'))
                .enter();
        assertLine("abc def ghi\nklm qrs nop", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('-'))
                .append(alt('2'))
                .append(alt('t'))
                .enter();
        assertLine("abc def ghi\nqrs klm nop", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('-'))
                .append(alt('3'))
                .append(alt('t'))
                .enter();
        assertLine("abc def ghi\nklm qrs nop", b, true);
    }

    @Test
    public void testKillLine() {
        reader.setKeyMap(EMACS);
        reader.getKeys().bind(new Reference("backward-kill-line"), ctrl('U'));
        TestBuffer b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('1'))
                .append(ctrl('U'))
                .enter();
        assertLine("abc def ghi\n", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('U'))
                .enter();
        assertLine("abc def ghi", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('3'))
                .append(ctrl('U'))
                .enter();
        assertLine("", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('1'))
                .append(ctrl('K'))
                .enter();
        assertLine("\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('2'))
                .append(ctrl('K'))
                .enter();
        assertLine("klm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('3'))
                .append(ctrl('K'))
                .enter();
        assertLine("", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('-'))
                .append(alt('1'))
                .append(ctrl('K'))
                .enter();
        assertLine("abc def ghi\n", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('-'))
                .append(alt('2'))
                .append(ctrl('K'))
                .enter();
        assertLine("abc def ghi", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('-'))
                .append(alt('3'))
                .append(ctrl('K'))
                .enter();
        assertLine("", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('-'))
                .append(alt('1'))
                .append(ctrl('U'))
                .enter();
        assertLine("\nklm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('-'))
                .append(alt('2'))
                .append(ctrl('U'))
                .enter();
        assertLine("klm nop qrs", b, true);

        b = (new TestBuffer(translate("abc def ghi\\E\rklm nop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('-'))
                .append(alt('3'))
                .append(ctrl('U'))
                .enter();
        assertLine("", b, true);
    }

    @Test
    public void testKillWholeLine() {
        reader.setKeyMap(EMACS);
        TestBuffer b = (new TestBuffer(translate("abc def\\E\rghi klm\\E\rnop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('2'))
                .append(ctrl('U'))
                .append('X')
                .enter();
        assertLine("abc def\nX", b, true);

        b = (new TestBuffer(translate("abc def\\E\rghi klm\\E\rnop qrs")))
                .append(alt('2'))
                .append(ctrl('A'))
                .append(alt('-'))
                .append(alt('2'))
                .append(ctrl('U'))
                .append('X')
                .enter();
        assertLine("Xnop qrs", b, true);
    }
}
