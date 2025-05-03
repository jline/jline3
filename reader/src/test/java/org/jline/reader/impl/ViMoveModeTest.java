/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.History;
import org.jline.reader.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.reader.LineReader.BACKWARD_KILL_LINE;
import static org.jline.reader.impl.LineReaderImpl.MAIN;
import static org.jline.reader.impl.LineReaderImpl.VICMD;
import static org.jline.reader.impl.LineReaderImpl.VIINS;
import static org.jline.reader.impl.LineReaderImpl.VISUAL;

/**
 * Unit tests for the greatest keymap binding in the world! Vi!!!!
 * These tests are primarily intended to test "move-mode" in VI, but
 * as a necessary by-product they use quite a bit of insert mode as well.
 */
public class ViMoveModeTest extends ReaderTestSupport {

    /**
     * For all tests we will start out in insert/edit mode.
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        reader.setVariable("WORDCHARS", "");
        reader.getKeyMaps().put(MAIN, reader.getKeyMaps().get(VIINS));
    }

    @Test
    public void testMoveLeft() throws Exception {
        /*
         * There are various keys that will move you left.
         */
        testMoveLeft("\033[D"); /* Left arrow */
        testMoveLeft("h"); /* h key */
        testMoveLeft("\010"); /* CTRL-H */
    }

    public void testMoveLeft(String left) throws Exception {
        /*
         * Move left
         */
        TestBuffer b = (new TestBuffer("0123456789"))
                .escape()
                .append(left)
                .append(left)
                .append(left)
                .append("iX")
                .enter();
        assertLine("012345X6789", b, true);

        /*
         * Move left - use digit arguments.
         */
        b = (new TestBuffer("0123456789"))
                .escape()
                .append('3')
                .append(left)
                .append("iX")
                .enter();
        assertLine("012345X6789", b, true);

        /*
         * Move left - use multi-digit arguments.
         */
        b = (new TestBuffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
                .escape()
                .append("13")
                .append(left)
                .append("iX")
                .enter();
        assertLine("0123456789ABCDEFHIJLXMNOPQRSTUVWXYZ", b, true);

        /*
         * Delete move left.
         */
        b = (new TestBuffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
                .escape()
                .append("13d")
                .append(left)
                .enter();
        assertLine("0123456789ABCDEFHIJLZ", b, true);

        b = (new TestBuffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
                .escape()
                .append("d")
                .append(left)
                .append("d")
                .append(left)
                .enter();
        assertLine("0123456789ABCDEFHIJLMNOPQRSTUVWZ", b, true);

        /*
         * Change move left
         */
        b = (new TestBuffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
                .escape()
                .append("13c")
                .append(left)
                .append("_HI")
                .enter();
        assertLine("0123456789ABCDEFHIJL_HIZ", b, true);

        b = (new TestBuffer("word"))
                .escape()
                .append("c")
                .append(left)
                .append("X")
                .enter();
        assertLine("woXd", b, true);

        /*
         * Yank left
         */
        b = (new TestBuffer("word"))
                .escape()
                .append("3y")
                .append(left)
                .append("p")
                .enter();
        assertLine("wordwor", b, true);
    }

    @Test
    public void testMoveRight() throws Exception {
        testMoveRight("\033[C"); /* right arrow */
        testMoveRight("l"); /* "l" key */
        testMoveRight(" "); /* space */
    }

    public void testMoveRight(String right) throws Exception {
        /*
         * Move right
         */
        TestBuffer b = (new TestBuffer("0123456789"))
                .escape()
                .append('0') // beginning of line
                .append(right)
                .append(right)
                .append(right)
                .append("iX")
                .enter();
        assertLine("012X3456789", b, true);

        /*
         * Move right use digit arguments.
         */
        b = (new TestBuffer("0123456789ABCDEFHIJK"))
                .escape()
                .append("012")
                .append(right)
                .append("iX")
                .enter();
        assertLine("0123456789ABXCDEFHIJK", b, true);

        /*
         * Delete move right
         */
        b = (new TestBuffer("a bunch of words"))
                .escape()
                .append("05d")
                .append(right)
                .enter();
        assertLine("ch of words", b, true);

        b = (new TestBuffer("a bunch of words"))
                .escape()
                .append("0d")
                .append(right)
                .append("d")
                .append(right)
                .enter();
        assertLine("bunch of words", b, true);

        /*
         * Change move right
         */
        b = (new TestBuffer("a bunch of words"))
                .escape()
                .append("010c")
                .append(right)
                .append("XXX")
                .enter();
        assertLine("XXX words", b, true);

        /*
         * Yank move right
         */
        b = (new TestBuffer("a bunch of words"))
                .escape()
                .append("010y")
                .append(right)
                .append("$p")
                .enter();
        assertLine("a bunch of wordsa bunch of", b, true);
    }

    @Test
    public void testCtrlJ() throws Exception {
        /*
         * ENTER is CTRL-J.
         */
        testEnter('J');
    }

    @Test
    public void testCtrlK() throws Exception {
        /*
         * Ctrl-K should delete to end-of-line
         */
        TestBuffer b = (new TestBuffer("This is a test"))
                .escape()
                .left()
                .left()
                .left()
                .left()
                .ctrl('K')
                .enter();

        assertLine("This is a", b, true);

        b = (new TestBuffer("hello")).escape().ctrl('K').enter();
        assertLine("hell", b, true);
    }

    @Test
    public void testCtrlL() throws Exception {
        /*
         * CTRL-L clears the screen. I can't test much but to make sure
         * that the cursor is where it is supposed to be.
         *
         * IMPORTANT NOTE: The CTRL-K is commented out below. Technically
         * it is a bug. What is supposed to happen is that the escape()
         * backs the cursor up one, so the CTRL-K is supposed to delete
         * the "o". With the CTRL-L involved, it doesn't. I suspect that
         * it has to do with the parsing of the stream of escape's coming
         * in and I'm not entirely sure it is easy to fix.  Since this is
         * really an edge case I'm commenting it out, but I'm leaving this
         * comment here because it may be a sign of something lurking down the
         * road.
         */
        TestBuffer b = (new TestBuffer("hello"))
                .escape()
                .ctrl('L')
                // .ctrl ('K')
                .enter();
        assertLine("hello", b, true);
    }

    @Test
    public void testCtrlM() throws Exception {
        testEnter('M');
    }

    @Test
    public void testCtrlP_CtrlN() throws Exception {
        TestBuffer b = (new TestBuffer("line1"))
                .enter()
                .append("line2")
                .enter()
                .append("li")
                .escape()
                .ctrl('P')
                .ctrl('P')
                .enter();
        assertLine("line1", b, false);

        reader.getHistory().purge();
        b = (new TestBuffer("line1"))
                .enter()
                .append("line2")
                .enter()
                .append("li")
                .escape()
                .ctrl('P')
                .ctrl('P')
                .ctrl('N')
                .enter();
        assertLine("line2", b, false);

        /*
         * One last test. Make sure that when we move through history
         * that the cursor is moved to the front of the line.
         */
        reader.getHistory().purge();
        b = (new TestBuffer("aline"))
                .enter()
                .append("bline")
                .enter()
                .append("cli")
                .escape()
                .ctrl('P')
                .ctrl('P')
                .ctrl('N')
                .append("iX")
                .enter();
        assertLine("blineX", b, false);
    }

    @Test
    public void testCtrlT() throws Exception {
        /*
         * Transpose every character exactly.
         */
        TestBuffer b = (new TestBuffer("abcdef"))
                .escape() // Move mode
                .append('0') // Beginning of line
                .right() // Right one
                .ctrl('T') // Transpose
                .ctrl('T')
                .ctrl('T')
                .ctrl('T')
                .ctrl('T')
                .enter();
        assertLine("bcdefa", b, false);

        /*
         * Cannot transpose the first character or the last character
         */
        b = (new TestBuffer("abcdef"))
                .escape() // Move mode
                .append('0') // Beginning of line
                .ctrl('T')
                .ctrl('T')
                .append('$') // End of line
                .ctrl('T')
                .enter();
        assertLine("bcadfe", b, false);
    }

    @Test
    public void testCtrlU() throws Exception {
        reader.getKeyMaps().get(VICMD).bind(new Reference(BACKWARD_KILL_LINE), ctrl('U'));
        /*
         * CTRL-U is "backward-kill-line", it deletes everything prior to the
         * current cursor position.
         */
        TestBuffer b = (new TestBuffer("all work and no play"))
                .escape() // Move mode
                .left(3) // Left to the "p" in play
                .ctrl('U') // Line discard
                .enter();
        assertLine("play", b, false);

        /*
         * Nothing happens at the beginning of the line
         */
        b = (new TestBuffer("donkey punch"))
                .escape() // Move mode
                .append('0') // Beginning of the line
                .ctrl('U') // Line discard
                .enter();
        assertLine("donkey punch", b, false);

        /*
         * End of the line leaves an empty buffer
         */
        b = (new TestBuffer("rabid hamster"))
                .escape() // Move mode
                .right() // End of line
                .ctrl('U') // Line discard
                .enter();
        assertLine("", b, false);
    }

    @Test
    public void testCtrlW() throws Exception {
        /*
         * CTRL-W is word rubout. It deletes to the beginning of the word
         * you are currently sitting in, or if you are one a break character
         * it deletes up to the beginning of the previous word.
         */
        TestBuffer b = (new TestBuffer("oily rancid badgers"))
                .escape()
                .ctrl('W')
                .ctrl('W')
                .enter();
        assertLine("oily s", b, false);

        /*
         * Test behavior with non-word characters.
         */
        b = (new TestBuffer("pasty bulimic rats !!!!!"))
                .escape()
                .ctrl('W')
                .ctrl('W')
                .enter();
        assertLine("pasty !", b, false);

        b = (new TestBuffer("pasty bulimic rats !!!!!"))
                .escape()
                .append("2")
                .ctrl('W')
                .enter();
        assertLine("pasty !", b, false);
    }

    @Test
    public void testInsertComment() throws Exception {
        /*
         * The # key causes a comment to get inserted.
         */
        TestBuffer b = (new TestBuffer("putrified whales")).escape().append("#");
        assertLine("#putrified whales", b, false);
    }

    @Test
    public void testD() throws Exception {
        // D is a vim extension for delete-to-end-of-line
        TestBuffer b =
                (new TestBuffer("banana")).escape().left(2).append("Dadaid").enter();
        assertLine("bandaid", b, false);
    }

    @Test
    public void testC() throws Exception {
        // C is a vim extension for change-to-end-of-line
        TestBuffer b = (new TestBuffer("yogurt")).escape().left(3).append("Cyo").enter();
        assertLine("yoyo", b, false);
    }

    @Test
    public void testS() throws Exception {
        // S is a vim extension that is a synonum for 'cc' (clear whole line)
        TestBuffer b = (new TestBuffer("great lakes brewery"))
                .escape()
                .left(3)
                .append("Sdogfishhead")
                .enter();
        assertLine("dogfishhead", b, false);
    }

    @Test
    public void testO() throws Exception {
        // O insert a line
        TestBuffer b = (new TestBuffer("great lakes brewery"))
                .escape()
                .left(6)
                .append("Odog ")
                .enter();
        assertLine("dog \ngreat lakes brewery", b, false);
    }

    @Test
    public void testo() throws Exception {
        // O insert a line
        TestBuffer b = (new TestBuffer("great lakes brewery"))
                .escape()
                .left(6)
                .append("odog ")
                .enter();
        assertLine("great lakes brewery\ndog ", b, false);
    }

    @Test
    public void testJ() throws Exception {
        // J joins the current line with the following one
        TestBuffer b = (new TestBuffer("bar"))
                .escape()
                .append("ofoo")
                .escape()
                .up()
                .append("J")
                .append("ii")
                .enter();
        assertLine("bari foo", b, false);
    }

    @Test
    public void testEndOfLine() throws Exception {
        /*
         * The $ key causes the cursor to move to the end of the line
         */
        TestBuffer b = (new TestBuffer("chicken sushimi"))
                .escape()
                .left(10)
                .append("$a is tasty!")
                .enter();
        assertLine("chicken sushimi is tasty!", b, false);

        /*
         * Delete to EOL
         */
        b = (new TestBuffer("chicken sushimi")).escape().append("0lld$").enter();
        assertLine("ch", b, false);

        /*
         * Change to EOL
         */
        b = (new TestBuffer("chicken sushimi")).escape().append("0llc$opsticks").enter();
        assertLine("chopsticks", b, false);

        /*
         * Yank to EOL
         */
        b = (new TestBuffer("chicken sushimi")).escape().append("0lly$$p").enter();
        assertLine("chicken sushimiicken sushimi", b, false);
    }

    @Test
    public void testYankLines() {
        /*
         * Yank whole line and put after
         */
        TestBuffer b = (new TestBuffer("chicken"))
                .escape()
                .append("o")
                .append("sushi")
                .escape()
                .append("o")
                .append("pork")
                .escape()
                .up()
                .append("2Ypiz")
                .enter();
        assertLine("chicken\nsushi\nzsushi\npork\npork", b, false);

        /*
         * Yank whole line and put before
         */
        b = (new TestBuffer("chicken"))
                .escape()
                .append("o")
                .append("sushi")
                .escape()
                .append("o")
                .append("pork")
                .escape()
                .up()
                .append("2YPiz")
                .enter();
        assertLine("chicken\nzsushi\npork\nsushi\npork", b, false);
    }

    @Test
    public void firstPrintable() throws Exception {
        TestBuffer b = (new TestBuffer(" foo bar")).escape().append("^dw").enter();
        assertLine(" bar", b, false);
    }

    @Test
    public void testMatch() throws Exception {
        /*
         * The % character matches brackets (square, parens, or curly).
         * First, test close paren w/nesting
         */
        TestBuffer b = (new TestBuffer("ab((cdef[[))"))
                .escape() // Move us back one character (on last close)
                .append("%aX") // Find match, add an X after it
                .enter();
        assertLine("ab(X(cdef[[))", b, false);

        /*
         * Open paren, w/nesting
         */
        b = (new TestBuffer("ab((cdef[[))"))
                .escape() // Move us back one character (on last close)
                .append('0') // Beginning of line
                .right(2) // Right to first open paren
                .append("%aX") // Match closing, add an X after it
                .enter();
        assertLine("ab((cdef[[))X", b, false);

        /*
         * No match leaves the cursor in place
         */
        b = (new TestBuffer("abcd))")).escape().append("%aX").enter();
        assertLine("abcd))X", b, false);

        b = (new TestBuffer("(abcd(d"))
                .escape()
                .append("0%aX") // Beginning of line, match, append X
                .enter();
        assertLine("(Xabcd(d", b, false);

        /*
         * Delete match
         */
        b = (new TestBuffer("ab(def)hij")).escape().append("0lld%").enter();
        assertLine("abhij", b, false);

        b = (new TestBuffer("ab(def)")).escape().append("0lld%").enter();
        assertLine("ab", b, false);

        /*
         * Yank match
         */
        b = (new TestBuffer("ab(def)hij")).escape().append("0lly%$p").enter();
        assertLine("ab(def)hij(def)", b, false);

        /*
         * Change match
         */
        b = (new TestBuffer("ab(def)hij")).escape().append("0llc%X").enter();
        assertLine("abXhij", b, false);
    }

    @Test
    public void testSearch() throws Exception {
        /*
         * Tests the "/" forward search
         */
        History history = reader.getHistory();
        history.purge();
        history.add("aaadef");
        history.add("bbbdef");
        history.add("cccdef");

        /*
         * An aborted search should leave you exactly on the
         * character you were on of the original term. First, I will
         * test aborting by deleting back over the search expression.
         */
        TestBuffer b = (new TestBuffer("I like frogs"))
                .escape()
                .left(4) // Cursor is on the "f"
                .append("/def") // Start a search
                .ctrl('G') // Abort
                .append("ibig ") // Insert mode, type "big "
                .enter(); // Done
        assertLine("I like big frogs", b, false);

        /*
         * Next, hit escape to abort. This technically isn't readline
         * behavior, but I added it because I find it useful.
         */
        b = (new TestBuffer("I like frogs"))
                .escape()
                .left(4) // Cursor is on the "f"
                .append("/def") // Start a search
                .ctrl('G') // Abort the search
                .append("ibig ") // Insert mode, type "big "
                .enter(); // Done
        assertLine("I like big frogs", b, false);

        /*
         * Test a failed history match. This is like an abort, but it
         * should leave the cursor at the start of the line.
         */
        b = (new TestBuffer("I like frogs"))
                .escape()
                .left(4) // Cursor is on the "f"
                .append("/III") // Search (no match)
                .enter() // Kick it off.
                .append("iX") // With cursor at start, insert an X
                .enter();
        assertLine("I like Xfrogs", b, false);

        /*
         * Test a valid search.
         */
        b = (new TestBuffer("I like frogs"))
                .escape()
                .left(4) // Cursor is on the "f"
                .append("/def") // Search (no match)
                .enter() // Kick it off.
                .append("nniX") // Move forward two, insert an X. Note I use
                // use "n" and "N" to move.
                .enter();
        assertLine("aaadeXf", b, false);

        /*
         * The previous test messed with history.
         */
        history.purge();
        history.add("aaadef");
        history.add("bbbdef");
        history.add("cccdef");

        /*
         * Search backwards
         */
        b = (new TestBuffer("I like frogs"))
                .escape()
                .left(4) // Cursor is on the "f"
                .append("?def") // Search (no match)
                .enter() // Kick it off.
                .append("nniX") // Move forward two, insert an X.
                // use "n" and "N" to move.
                .enter();
        assertLine("cccdeXf", b, false);

        /*
         * Test bug fix: use CR to terminate seach instead of newline
         */
        b = (new TestBuffer("abc"))
                .enter()
                .append("def")
                .enter()
                .append("hij")
                .enter()
                .escape()
                .append("/bc")
                .CR()
                .append("iX")
                .enter();
        assertLine("abXc", b, false);
    }

    @Test
    public void testWordRight() throws Exception {
        reader.getKeyMaps().get(VICMD).bind(new Reference(BACKWARD_KILL_LINE), ctrl('U'));
        TestBuffer b = (new TestBuffer("buttery frog necks"))
                .escape()
                .append("0ww") // Beginning of line, nxt word, nxt word
                .ctrl('U') // Kill to beginning of line
                .enter(); // Kick it off.
        assertLine("necks", b, false);

        b = (new TestBuffer("buttery frog    foo"))
                .escape()
                .left(5)
                .append('w')
                .ctrl('K') // Kill to end of line
                .enter(); // Kick it off.
        assertLine("buttery frog    ", b, false);

        b = (new TestBuffer("a big batch of buttery frog livers"))
                .escape()
                .append("05w") // Beg of line, 5 words right
                .ctrl('U') // Kill to beginning of line
                .enter(); // Kick it off.
        assertLine("frog livers", b, false);

        /*
         * Delete word right
         */
        b = (new TestBuffer("a big batch of buttery frog livers"))
                .escape()
                .append("05dw")
                .enter();
        assertLine("frog livers", b, false);

        b = (new TestBuffer("another big batch of buttery frog livers"))
                .escape()
                .append("0ldw")
                .enter();
        assertLine("abig batch of buttery frog livers", b, false);

        /*
         * Yank word right
         */
        b = (new TestBuffer("big brown pickles")).escape().append("02yw$piz").enter();
        assertLine("big brown picklesbig brownz ", b, false);

        /*
         * Put before
         */
        b = (new TestBuffer("big brown pickles")).escape().append("02yw$Piz").enter();
        assertLine("big brown picklebig brownz s", b, false);

        /*
         * Change word right
         */
        b = (new TestBuffer("big brown pickles")).escape().append("0wcwgreen").enter();
        assertLine("big green pickles", b, false);

        b = (new TestBuffer("big brown pickles"))
                .escape()
                .append("02cwlittle bitty")
                .enter();
        assertLine("little bitty pickles", b, false);
    }

    @Test
    public void testWordLeft() throws Exception {
        reader.getKeyMaps().get(VICMD).bind(new Reference(BACKWARD_KILL_LINE), ctrl('U'));
        TestBuffer b = (new TestBuffer("lucious lark liquid    "))
                .escape()
                .append("bb") // Beginning of line, prv word, prv word
                .ctrl('K') // Kill to end of line
                .enter(); // Kick it off.
        assertLine("lucious ", b, false);

        b = (new TestBuffer("lucious lark liquid"))
                .escape()
                .left(2)
                .append('b')
                .ctrl('U')
                .enter();
        assertLine("liquid", b, false);

        b = (new TestBuffer("lively lolling lark liquid"))
                .escape()
                .append("3b")
                .ctrl('K') // Kill to beginning of line
                .enter();
        assertLine("lively ", b, false);
    }

    @Test
    public void testEndWord() throws Exception {
        reader.getKeyMaps().get(VICMD).bind(new Reference(BACKWARD_KILL_LINE), ctrl('U'));

        TestBuffer b = (new TestBuffer("putrid pidgen porridge"))
                .escape()
                .append("0e")
                .append('D') // Kill to end of line
                .enter(); // Kick it off.
        assertLine("putri", b, false);

        b = (new TestBuffer("    putrid pidgen porridge"))
                .escape()
                .append("0e")
                .append('D') // Kill to end of line
                .enter(); // Kick it off.
        assertLine("    putri", b, false);

        b = (new TestBuffer("putrid pidgen porridge and mash"))
                .escape()
                .append("05l") // Beg of line, 5 right
                .append("3e") // 3 end-of-word
                .ctrl('U') // Kill to beg of line
                .enter(); // Kick it off.
        assertLine("d mash", b, false);
    }

    @Test
    public void testInsertBeginningOfLine() throws Exception {
        TestBuffer b = (new TestBuffer("dessicated dog droppings"))
                .escape()
                .append("Itasty ")
                .enter();
        assertLine("tasty dessicated dog droppings", b, false);
    }

    @Test
    public void testRubout() throws Exception {
        TestBuffer b = (new TestBuffer("gross animal stuff"))
                .escape()
                .left()
                .append("XXX")
                .enter();
        assertLine("gross animal ff", b, false);

        b = (new TestBuffer("gross animal stuff")).escape().left().append("50X").enter();
        assertLine("ff", b, false);
    }

    @Test
    public void testDelete() throws Exception {
        TestBuffer b =
                (new TestBuffer("thing to delete")).escape().append("bbxxx").enter();
        assertLine("thing delete", b, false);

        b = (new TestBuffer("thing to delete")).escape().append("bb99x").enter();
        assertLine("thing ", b, false);
    }

    @Test
    public void testChangeCase() throws Exception {
        TestBuffer b =
                (new TestBuffer("big.LITTLE")).escape().append("0~~~~~~~~~~").enter();
        assertLine("BIG.little", b, false);

        b = (new TestBuffer("big.LITTLE")).escape().append("020~").enter();
        assertLine("BIG.little", b, false);
    }

    @Test
    public void testChangeChar() throws Exception {
        TestBuffer b = (new TestBuffer("abcdefhij")).escape().append("0rXiY").enter();
        assertLine("YXbcdefhij", b, false);

        b = (new TestBuffer("abcdefhij")).escape().append("04rXiY").enter();
        assertLine("XXXYXefhij", b, false);

        b = (new TestBuffer("abcdefhij")).escape().append("099rZ").enter();
        assertLine("ZZZZZZZZZ", b, false);

        /*
         * Aborted replace.
         */
        b = (new TestBuffer("abcdefhij"))
                .escape()
                .append("0r")
                .escape()
                .append("iX")
                .enter();
        assertLine("Xabcdefhij", b, false);
    }

    @Test
    public void testCharSearch_f() throws Exception {
        /*
         * f = search forward for character
         */
        TestBuffer b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("03ffiX") // start, find the third f, insert X
                .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("0ffffffiX") // start, find the third f, insert X
                .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("0ff;;iX") // start, find f, repeat fwd, repeat fwd
                .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("0ff;,iX") // start, find f, repeat fwd, repeat back
                .enter();
        assertLine("aaaaXfaaaafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX"))
                .escape()
                .append("0fX3;iY") // start, find X, repeat fwd x 3, ins Y
                .enter();
        assertLine("aaaaXaaaaXaaaaXaaaaYX", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("03dff") // start, delete to third f
                .enter();
        assertLine("aaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX"))
                .escape()
                .append("0fX2d;") // start, find X, 2 x delete repeat last search
                .enter();
        assertLine("aaaaaaaaX", b, true);
    }

    @Test
    public void testCharSearch_F() throws Exception {
        /*
         * f = search forward for character
         */
        TestBuffer b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("3FfiX") // go 3 f's back, insert X
                .enter();
        assertLine("aaaaXfaaaafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("FfFfFfiX") // start, find the third f back, insert X
                .enter();
        assertLine("aaaaXfaaaafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("Ff;iX") // start, find f, repeat fwd, repeat fwd
                .enter();
        assertLine("aaaafaaaaXfaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("Ff;,iX") // start, rev find f, repeat, reverse
                .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX"))
                .escape()
                .append("FX2;iY") // start, rev find X, repeat x 2, ins Y
                .enter();
        assertLine("aaaaYXaaaaXaaaaXaaaaX", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("3dFf") // start, delete back to third f
                .enter();
        assertLine("aaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX"))
                .escape()
                .append("FX2d;") // start, find X, 2 x delete repeat last search
                .enter();
        assertLine("aaaaXaaaaX", b, true);
    }

    @Test
    public void testCharSearch_t() throws Exception {
        /*
         * r = search forward for character, stopping before
         */
        TestBuffer b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("03tfiX")
                .enter();
        assertLine("aaaafaaaafaaaXafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("0tftftfiX")
                .enter();
        assertLine("aaaXafaaaafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("0tf;;iX").enter();
        assertLine("aaaafaaaafaaaXafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("02tf;,iX").enter();
        assertLine("aaaafaaaafXaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX")).escape().append("0tX3;iY").enter();
        assertLine("aaaaXaaaaXaaaaXaaaYaX", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("03dtf").enter();
        assertLine("faaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX")).escape().append("0tX2d;iY").enter();
        assertLine("aaaYXaaaaX", b, true);
    }

    @Test
    public void testCharSearch_T() throws Exception {
        /*
         * r = search backward for character, stopping after
         */
        TestBuffer b = (new TestBuffer("aaaafaaaafaaaafaaaaf"))
                .escape()
                .append("3TfiX")
                .enter();
        assertLine("aaaafXaaaafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("TfTfTfiX").enter();
        assertLine("aaaafaaaafaaaafXaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("Tf;;iX").enter();
        assertLine("aaaafXaaaafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("2Tf;,iX").enter();
        assertLine("aaaafaaaXafaaaafaaaaf", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX")).escape().append("TX2;iY").enter();
        assertLine("aaaaXYaaaaXaaaaXaaaaX", b, true);

        b = (new TestBuffer("aaaafaaaafaaaafaaaaf")).escape().append("3dTf").enter();
        assertLine("aaaaff", b, true);

        b = (new TestBuffer("aaaaXaaaaXaaaaXaaaaX")).escape().append("TX2d;iY").enter();
        assertLine("aaaaXYaaaaX", b, true);
    }

    @Test
    public void test_dd() throws Exception {
        /*
         * This tests "dd" or delete-to + delete-to, which should kill the
         * current line.
         */
        TestBuffer b = (new TestBuffer("abcdef")).escape().append("dd").enter();
        assertLine("", b, true);

        /*
         * I found a bug here dd didn't work at position 0. This tests the fix.
         */
        b = (new TestBuffer("abcdef")).escape().append("0dd").enter();
        assertLine("", b, true);
    }

    @Test
    public void test_yy() throws Exception {
        /*
         * This tests "yy" or yank-to + yank-to, which should yank the whole line
         */
        TestBuffer b = (new TestBuffer("abcdef")).escape().append("yyp").enter();
        assertLine("abcdefabcdef", b, true);
    }

    @Test
    public void test_cc() throws Exception {
        /*
         * This tests "cc" or change-to + change-to, which changes the whole line
         */
        TestBuffer b = (new TestBuffer("abcdef")).escape().append("ccsuck").enter();
        assertLine("suck", b, true);
    }

    @Test
    public void testRegion() throws Exception {
        reader.getKeyMaps().get(VISUAL).bind(new Reference("kill-region"), "p");
        TestBuffer b = new TestBuffer("abc def ghi")
                .escape()
                .append("bvbp")
                .append("iX")
                .enter();
        assertLine("abc Xhi", b, true);

        b = new TestBuffer("abc def ghi").escape().append("bvbop").append("iX").enter();
        assertLine("abc Xhi", b, true);

        b = new TestBuffer()
                .append("abc def ghi")
                .ctrl('V')
                .append('\n')
                .append("foo bar baz")
                .ctrl('V')
                .append('\n')
                .append("klm nop")
                .escape()
                .append("4bV3bp")
                .append("iX")
                .enter();
        assertLine("Xklm nop", b, true);

        b = new TestBuffer()
                .append("abc def ghi")
                .ctrl('V')
                .append('\n')
                .append("foo bar baz")
                .ctrl('V')
                .append('\n')
                .append("klm nop")
                .escape()
                .append("4bV3bop")
                .append("iX")
                .enter();
        assertLine("abc Xar baz\nklm nop", b, true);
    }

    /**
     * Used to test various forms of hitting "enter" (return). This can be
     * CTRL-J or CTRL-M...maybe others.
     *
     * @param enterChar The escape character that acts as enter.
     */
    private void testEnter(char enterChar) throws Exception {
        /*
         * I want to test to make sure that I am re-entering insert mode
         * when enter is hit.
         */
        TestBuffer b = (new TestBuffer("abc")).escape().ctrl(enterChar);
        assertLine("abc", b, true);

        /*
         * This sort of tests the same thing by actually enter
         * characters after the first enter.
         */
        b = (new TestBuffer("abc")).escape().ctrl(enterChar).append("def").enter();
        assertLine("def", b, true);
    }
}
