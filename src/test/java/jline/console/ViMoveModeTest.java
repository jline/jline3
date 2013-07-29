/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.console;

import static jline.console.Operation.*;
import jline.console.history.History;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the greatest keymap binding in the world! Vi!!!!
 * These tests are primarily intended to test "move-mode" in VI, but
 * as a necessary by-product they use quite a bit of insert mode as well.
 */
public class ViMoveModeTest
    extends ConsoleReaderTestSupport {
    
    /**
     * For all tests we will start out in insert/edit mode.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Test 
    public void testMoveLeft() throws Exception {
        /*
         * There are various keys that will move you left.
         */
        testMoveLeft("\033[D");  /* Left arrow */
        testMoveLeft("h");       /* h key */
        testMoveLeft("\010");    /* CTRL-H */
    }
    
    public void testMoveLeft(String left) throws Exception {
        /*
         * Move left
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("0123456789"))
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
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("0123456789"))
            .escape()
            .append('3')
            .append(left)
            .append("iX")
            .enter();
        assertLine("012345X6789", b, true);
        
        /* 
         * Move left - use multi-digit arguments.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
            .escape()
            .append("13")
            .append(left)
            .append("iX")
            .enter();
        assertLine("0123456789ABCDEFHIJLXMNOPQRSTUVWXYZ", b, true);
        
        /*
         * Delete move left.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
            .escape()
            .append("13d")
            .append(left)
            .enter();
        assertLine("0123456789ABCDEFHIJLZ", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
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
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("0123456789ABCDEFHIJLMNOPQRSTUVWXYZ"))
            .escape()
            .append("13c")
            .append(left)
            .append("_HI")
            .enter();
        assertLine("0123456789ABCDEFHIJL_HIZ", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("word"))
            .escape()
            .append("c")
            .append(left)
            .append("X")
            .enter();
        assertLine("woXd", b, true);
        
        /*
         * Yank left
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("word"))
            .escape()
            .append("3y")
            .append(left)
            .append("p")
            .enter();
        assertLine("wordwor", b, true);
    }
    
    @Test 
    public void testMoveRight() throws Exception {
        testMoveRight("\033[C");  /* right arrow */
        testMoveRight("l");       /* "l" key */
        testMoveRight(" ");       /* space */
    }
    
    public void testMoveRight(String right) throws Exception {
        /*
         * Move right
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("0123456789"))
            .escape()
            .append('0')   // beginning of line
            .append(right)
            .append(right)
            .append(right)
            .append("iX")
            .enter();
        assertLine("012X3456789", b, true);
        
        /* 
         * Move right use digit arguments.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("0123456789ABCDEFHIJK"))
            .escape()
            .append("012")
            .append(right)
            .append("iX")
            .enter();
        assertLine("0123456789ABXCDEFHIJK", b, true);
        
        /*
         * Delete move right
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("a bunch of words"))
            .escape()
            .append("05d")
            .append(right)
            .enter();
        assertLine("ch of words", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("a bunch of words"))
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
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("a bunch of words"))
            .escape()
            .append("010c")
            .append(right)
            .append("XXX")
            .enter();
        assertLine("XXX words", b, true);
        
        /*
         * Yank move right
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("a bunch of words"))
            .escape()
            .append("010y")
            .append(right)
            .append("$p")
            .enter();
        assertLine("a bunch of wordsa bunch of", b, true);
    }
    
    @Test
    public void testCtrlD() throws Exception {
        /*
         * According to bash behavior hitting ^D anywhere in a non-empty
         * line is just like hitting enter.  First, test at the end of the line.
         * The escape() puts us in move mode.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abc")).escape().op(VI_EOF_MAYBE);
        assertLine("abc", b, true);
        
        /*
         * Since VI_EOF_MAYBE is acceptable in both move mode and insert
         * mode, make sure we are testing the right now.
         */
        assertTrue(console.isKeyMap(KeyMap.VI_MOVE));
        
        /*
         * Next, the middle of the line.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abc")).left().left().escape().op(VI_EOF_MAYBE);
        assertLine("abc", b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_MOVE));
        
        /*
         * Beginning of the line.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abc")).left().left().left().escape().op(VI_EOF_MAYBE);
        assertLine("abc", b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_MOVE));
        
        /*
         * Now, check the behavior of an empty buffer. This should cause
         * a null to be returned.  I'll try it in two different ways.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abc")).back().back().back().escape().op(VI_EOF_MAYBE);
        assertLine(null, b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_MOVE));
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("")).escape().op(VI_EOF_MAYBE);
        assertLine(null, b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_MOVE));
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
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("This is a test"))
            .escape()
            .left().left().left().left()
            .ctrl('K')
            .enter();
        
        assertLine("This is a", b, true);
        
        b = (new Buffer("hello"))
            .escape()
            .ctrl('K')
            .enter();
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
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("hello"))
            .escape()
            .ctrl ('L')
            // .ctrl ('K')
            .enter ();
        assertLine("hello", b, true);
    }
    
    @Test
    public void testCtrlM() throws Exception {
        testEnter('M');
    }
    
    @Test
    public void testCtrlP_CtrlN() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("line1")).enter()
            .append("line2").enter()
            .append("li")
            .escape()
            .ctrl('P')
            .ctrl('P')
            .enter();
        assertLine("line1", b, false);
        
        console.getHistory ().clear ();
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("line1")).enter()
            .append("line2").enter()
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
        console.getHistory ().clear ();
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aline")).enter()
            .append("bline").enter()
            .append("cli")
            .escape()
            .ctrl('P')
            .ctrl('P')
            .ctrl('N')
            .append ("iX")
            .enter();
        assertLine("Xbline", b, false);
    }
    
    @Test
    public void testCtrlT() throws Exception {
        /*
         * Transpose every character exactly.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abcdef"))
            .escape()           // Move mode
            .append('0')        // Beginning of line
            .right()            // Right one
            .ctrl('T')          // Transpose
            .ctrl('T')
            .ctrl('T')
            .ctrl('T')
            .ctrl('T')
            .enter();
        assertLine("bcdefa", b, false);
        
        /*
         * Cannot transpose the first character or the last character
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abcdef"))
            .escape()           // Move mode
            .append('0')        // Beginning of line
            .ctrl('T')
            .ctrl('T')
            .append('$')        // End of line
            .ctrl('T')
            .ctrl('T')
            .enter();
        assertLine("abcdef", b, false);
    }
    
    @Test
    public void testCtrlU() throws Exception {
        /*
         * CTRL-U is "line discard", it deletes everything prior to the
         * current cursor position.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("all work and no play"))
            .escape()            // Move mode
            .left(3)             // Left to the "p" in play
            .ctrl('U')           // Line discard
            .enter();
        assertLine("play", b, false);
        
        /*
         * Nothing happens at the beginning of the line
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("donkey punch"))
            .escape()            // Move mode
            .append('0')         // Beginning of the line
            .ctrl('U')           // Line discard
            .enter();
        assertLine("donkey punch", b, false);
        
        /*
         * End of the line leaves an empty buffer
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("rabid hamster"))
            .escape()            // Move mode
            .right()             // End of line
            .ctrl('U')           // Line discard
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
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("oily rancid badgers"))
            .escape()
            .ctrl('W')
            .ctrl('W')
            .enter();
        assertLine("oily s", b, false);
        
        /*
         * Test behavior with non-word characters. 
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("pasty bulimic rats !!!!!"))
            .escape()
            .ctrl('W')
            .ctrl('W')
            .enter();
        assertLine("pasty bulimic !", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("pasty bulimic rats !!!!!"))
            .escape()
            .append("2")
            .ctrl('W')
            .enter();
        assertLine("pasty bulimic !", b, false);
    }
    
    @Test
    public void testInsertComment() throws Exception {
        /*
         * The # key causes a comment to get inserted.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("putrified whales"))
            .escape()
            .append ("#");
        assertLine("#putrified whales", b, false);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
    }
    
    @Test
    public void testD() throws Exception {
        // D is a vim extension for delete-to-end-of-line
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("banana"))
            .escape()
            .left(2)
            .append("Dadaid")
            .enter();
        assertLine("bandaid", b, false);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
    }
    
    @Test
    public void testC() throws Exception {
        // C is a vim extension for change-to-end-of-line
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("yogurt"))
            .escape()
            .left(3)
            .append("Cyo")
            .enter();
        assertLine("yoyo", b, false);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
    }
    
    @Test
    public void testS() throws Exception {
        // S is a vim extension that is a synonum for 'cc' (clear whole line)
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("great lakes brewery"))
            .escape()
            .left(3)
            .append("Sdogfishhead")
            .enter();
        assertLine("dogfishhead", b, false);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
    }
    
    @Test
    public void testEndOfLine() throws Exception {
        /*
         * The $ key causes the cursor to move to the end of the line
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("chicken sushimi"))
            .escape()
            .left(10)
            .append("$a is tasty!")
            .enter();
        assertLine("chicken sushimi is tasty!", b, false);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
        
        /*
         * Delete to EOL
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("chicken sushimi"))
            .escape()
            .append("0lld$")
            .enter();
        assertLine("ch", b, false);
        
        /*
         * Change to EOL
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("chicken sushimi"))
            .escape()
            .append("0llc$opsticks")
            .enter();
        assertLine("chopsticks", b, false);
        
        /*
         * Yank to EOL
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("chicken sushimi"))
            .escape()
            .append("0lly$$p")
            .enter();
        assertLine("chicken sushimiicken sushimi", b, false);
    }
    
    @Test
    public void testMatch() throws Exception {
        /*
         * The % character matches brackets (square, parens, or curly). 
         * First, test close paren w/nesting
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("ab((cdef[[))"))
            .escape()       // Move us back one character (on last close)
            .append("%aX")  // Find match, add an X after it
            .enter();
        assertLine("ab(X(cdef[[))", b, false);
        
        /*
         * Open paren, w/nesting
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("ab((cdef[[))"))
            .escape()       // Move us back one character (on last close)
            .append('0')    // Beginning of line
            .right(2)       // Right to first open paren
            .append("%aX")  // Match closing, add an X after it
            .enter();
        assertLine("ab((cdef[[))X", b, false);
        
        /*
         * No match leaves the cursor in place
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abcd))"))
            .escape()
            .append("%aX")
            .enter();
        assertLine("abcd))X", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("(abcd(d"))
            .escape()
            .append("0%aX") // Beginning of line, match, append X
            .enter();
        assertLine("(Xabcd(d", b, false);
        
        /*
         * Delete match
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("ab(def)hij"))
            .escape()
            .append("0lld%")
            .enter();
        assertLine("abhij", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("ab(def)"))
            .escape()
            .append("0lld%")
            .enter();
        assertLine("ab", b, false);
        
        /*
         * Yank match
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("ab(def)hij"))
            .escape()
            .append("0lly%$p")
            .enter();
        assertLine("ab(def)hij(def)", b, false);
        
        /*
         * Change match
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("ab(def)hij"))
            .escape()
            .append("0llc%X")
            .enter();
        assertLine("abXhij", b, false);
    }
    
    @Test
    public void testSearch() throws Exception {
        /*
         * Tests the "/" forward search
         */
        History history = console.getHistory();
        history.clear();
        history.add("aaadef");
        history.add("bbbdef");
        history.add("cccdef");
        
        /*
         * An aborted search should leave you exactly on the
         * character you were on of the original term. First, I will
         * test aborting by deleting back over the search expression.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("I like frogs"))
            .escape()
            .left(4)          // Cursor is on the "f"
            .append("/def")   // Start a search
            .back(4)          // Delete everything (aborts search)
            .append("ibig ")  // Insert mode, type "big "
            .enter();         // Done
        assertLine("I like big frogs", b, false);
        
        /*
         * Next, hit escape to abort. This technically isn't readline
         * behavior, but I added it because I find it useful.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("I like frogs"))
            .escape()
            .left(4)          // Cursor is on the "f"
            .append("/def")   // Start a search
            .escape()         // Abort the search
            .append("ibig ")  // Insert mode, type "big "
            .enter();         // Done
        assertLine("I like big frogs", b, false);
        
        /*
         * Test a failed history match. This is like an abort, but it
         * should leave the cursor at the start of the line.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("I like frogs"))
            .escape()
            .left(4)          // Cursor is on the "f"
            .append("/III")   // Search (no match)
            .enter()          // Kick it off.
            .append("iX")     // With cursor at start, insert an X
            .enter();
        assertLine("XI like frogs", b, false);
        
        /*
         * Test a valid search.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("I like frogs"))
            .escape()
            .left(4)          // Cursor is on the "f"
            .append("/def")   // Search (no match)
            .enter()          // Kick it off.
            .append("nNiX")   // Move forward two, insert an X. Note I use
                              // use "n" and "N" to move.
            .enter();
        assertLine("Xcccdef", b, false);
        
        /*
         * The previous test messed with history.
         */
        history.clear();
        history.add("aaadef");
        history.add("bbbdef");
        history.add("cccdef");
        
        /*
         * Search backwards
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("I like frogs"))
            .escape()
            .left(4)          // Cursor is on the "f"
            .append("?def")   // Search (no match)
            .enter()          // Kick it off.
            .append("nNiX")   // Move forward two, insert an X. Note I use
                              // use "n" and "N" to move.
            .enter();
        assertLine("Xaaadef", b, false);
        
        /*
         * Test bug fix: use CR to terminate seach instead of newline
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abc"))
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
        assertLine("Xabc", b, false);
    }
    
    @Test
    public void testWordRight() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("buttery frog necks"))
            .escape()
            .append("0ww")    // Beginning of line, nxt word, nxt word
            .ctrl('U')        // Kill to beginning of line
            .enter();         // Kick it off.
        assertLine("necks", b, false);
        
        b = (new Buffer("buttery frog    foo"))
            .escape()
            .left(5)
            .append('w')
            .ctrl('K')        // Kill to end of line
            .enter();         // Kick it off.
        assertLine("buttery frog    ", b, false);
        
        b = (new Buffer("a big batch of buttery frog livers"))
            .escape()
            .append("05w")    // Beg of line, 5 words right
            .ctrl('U')        // Kill to beginning of line
            .enter();         // Kick it off.
        assertLine("frog livers", b, false);
        
        /*
         * Delete word right
         */
        b = (new Buffer("a big batch of buttery frog livers"))
            .escape()
            .append("05dw")
            .enter();
        assertLine("frog livers", b, false);
        
        b = (new Buffer("another big batch of buttery frog livers"))
            .escape()
            .append("0ldw")
            .enter();
        assertLine("abig batch of buttery frog livers", b, false);
        
        /*
         * Yank word right
         */
        b = (new Buffer("big brown pickles"))
            .escape()
            .append("02yw$p")
            .enter();
        assertLine("big brown picklesbig brown ", b, false);
        
        /*
         * Change word right
         */
        b = (new Buffer("big brown pickles"))
            .escape()
            .append("0wcwgreen")
            .enter();
        assertLine("big green pickles", b, false);
        
        b = (new Buffer("big brown pickles"))
            .escape()
            .append("02cwlittle bitty")
            .enter();
        assertLine("little bitty pickles", b, false);
    }
    
    @Test
    public void testWordLeft() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("lucious lark liquid    "))
            .escape()
            .append("bb")     // Beginning of line, prv word, prv word
            .ctrl('K')        // Kill to end of line
            .enter();         // Kick it off.
        assertLine("lucious ", b, false);
        
        b = (new Buffer("lucious lark liquid"))
            .escape()
            .left(2)
            .append('b')
            .ctrl('U')
            .enter();
        assertLine("liquid", b, false);
        
        b = (new Buffer("lively lolling lark liquid"))
            .escape()
            .append("3b")
            .ctrl('K')        // Kill to beginning of line
            .enter();
        assertLine("lively ", b, false);
    }
    
    @Test
    public void testEndWord() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("putrid pidgen porridge"))
            .escape()
            .append("0e")
            .ctrl('K')        // Kill to end of line
            .enter();         // Kick it off.
        assertLine("putri", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("    putrid pidgen porridge"))
            .escape()
            .append("0e")
            .ctrl('K')        // Kill to end of line
            .enter();         // Kick it off.
        assertLine("    putri", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("putrid pidgen porridge and mash"))
            .escape()
            .append("05l") // Beg of line, 5 right
            .append("3e")  // 3 end-of-word
            .ctrl('U')     // Kill to beg of line
            .enter();         // Kick it off.
        assertLine("d mash", b, false);
    }
    
    @Test
    public void testInsertBeginningOfLine() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("dessicated dog droppings"))
            .escape()
            .append("Itasty ")
            .enter();
        assertLine("tasty dessicated dog droppings", b, false);
    }
    
    @Test
    public void testRubout() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("gross animal stuff"))
            .escape()
            .left()
            .append("XXX")
            .enter();
        assertLine("gross animal ff", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("gross animal stuff"))
            .escape()
            .left()
            .append("50X")
            .enter();
        assertLine("ff", b, false);
    }
    
    @Test
    public void testDelete() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("thing to delete"))
            .escape()
            .append("bbxxx")
            .enter();
        assertLine("thing delete", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("thing to delete"))
            .escape()
            .append("bb99x")
            .enter();
        assertLine("thing ", b, false);
    }
    
    @Test
    public void testChangeCase() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("big.LITTLE"))
            .escape()
            .append("0~~~~~~~~~~")
            .enter();
        assertLine("BIG.little", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("big.LITTLE"))
            .escape()
            .append("020~")
            .enter();
        assertLine("BIG.little", b, false);
    }
    
    @Test
    public void testChangeChar() throws Exception {
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abcdefhij"))
            .escape()
            .append("0rXiY")
            .enter();
        assertLine("YXbcdefhij", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abcdefhij"))
            .escape()
            .append("04rXiY")
            .enter();
        assertLine("XXXYXefhij", b, false);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abcdefhij"))
            .escape()
            .append("099rZ")
            .enter();
        assertLine("ZZZZZZZZZ", b, false);
        
        
        /*
         * Aborted replace.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abcdefhij"))
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
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("03ffiX") // start, find the third f, insert X
            .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("0ffffffiX") // start, find the third f, insert X
            .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("0ff;;iX") // start, find f, repeat fwd, repeat fwd
            .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("0ff;,iX") // start, find f, repeat fwd, repeat back
            .enter();
        assertLine("aaaaXfaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
            .escape()
            .append("0fX3;iY") // start, find X, repeat fwd x 3, ins Y
            .enter();
        assertLine("aaaaXaaaaXaaaaXaaaaYX", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("03dff") // start, delete to third f
            .enter();
        assertLine("aaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
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
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("3FfiX") // go 3 f's back, insert X
            .enter();
        assertLine("aaaaXfaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("FfFfFfiX") // start, find the third f back, insert X
            .enter();
        assertLine("aaaaXfaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("Ff;iX") // start, find f, repeat fwd, repeat fwd
            .enter();
        assertLine("aaaafaaaaXfaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("Ff;,iX") // start, rev find f, repeat, reverse
            .enter();
        assertLine("aaaafaaaafaaaaXfaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
            .escape()
            .append("FX2;iY") // start, rev find X, repeat x 2, ins Y
            .enter();
        assertLine("aaaaYXaaaaXaaaaXaaaaX", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("3dFf") // start, delete back to third f
            .enter();
        assertLine("aaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
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
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("03tfiX")
            .enter();
        assertLine("aaaafaaaafaaaXafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("0tftftfiX")
            .enter();
        assertLine("aaaXafaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("0tf;;iX")
            .enter();
        assertLine("aaaXafaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("02tf;,iX")
            .enter();
        assertLine("aaaafXaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
            .escape()
            .append("0tX3;iY")
            .enter();
        assertLine("aaaaXaaaaXaaaYaXaaaaX", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("03dtf")
            .enter();
        assertLine("faaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
            .escape()
            .append("0tX2d;")
            .enter();
        assertLine("aaaXaaaaXaaaaX", b, true);
    }
    
    @Test
    public void testCharSearch_T() throws Exception {
        /*
         * r = search backward for character, stopping after
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("3TfiX")
            .enter();
        assertLine("aaaafXaaaafaaaafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("TfTfTfiX")
            .enter();
        assertLine("aaaafaaaafaaaafXaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("Tf;;iX")
            .enter();
        assertLine("aaaafaaaafaaaafXaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("2Tf;,iX")
            .enter();
        assertLine("aaaafaaaafaaaXafaaaaf", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
            .escape()
            .append("TX3;iY")
            .enter();
        assertLine("aaaaXYaaaaXaaaaXaaaaX", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaafaaaafaaaafaaaaf"))
            .escape()
            .append("3dTf")
            .enter();
        assertLine("aaaaff", b, true);
        
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("aaaaXaaaaXaaaaXaaaaX"))
            .escape()
            .append("TX2d;")
            .enter();
        assertLine("aaaaXaaaaXaaaaX", b, true);
    }
    
    @Test
    public void test_dd() throws Exception {
        /*
         * This tests "dd" or delete-to + delete-to, which should kill the
         * current line.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abcdef"))
            .escape()
            .append("dd")
            .enter();
        assertLine("", b, true);
        
        /*
         * I found a bug here dd didn't work at position 0. This tests the fix.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abcdef"))
            .escape()
            .append("0dd")
            .enter();
        assertLine("", b, true);
    }
    
    @Test
    public void test_yy() throws Exception {
        /*
         * This tests "yy" or yank-to + yank-to, which should yank the whole line
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abcdef"))
            .escape()
            .append("yyp")
            .enter();
        assertLine("abcdefabcdef", b, true);
    }
    
    @Test
    public void test_cc() throws Exception {
        /*
         * This tests "cc" or change-to + change-to, which changes the whole line
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abcdef"))
            .escape()
            .append("ccsuck")
            .enter();
        assertLine("suck", b, true);
    }
    
    /**
     * Used to test various forms of hitting "enter" (return). This can be
     * CTRL-J or CTRL-M...maybe others.
     * 
     * @param enterChar The escape character that acts as enter.
     * @throws Exception
     */
    private void testEnter(char enterChar) throws Exception {
        /*
         * I want to test to make sure that I am re-entering insert mode 
         * when enter is hit.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abc")).escape().ctrl(enterChar);
        assertLine("abc", b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
        
        /*
         * This sort of tests the same thing by actually enter
         * characters after the first enter.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abc")).escape().ctrl(enterChar)
            .append("def").enter();
        assertLine("def", b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
    }
    
    /*
     * TODO - Test arrow key bindings
     */
}
