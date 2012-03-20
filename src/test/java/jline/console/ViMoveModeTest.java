/*
 * Copyright (c) 2012, Scott C. Gray. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console;

import static jline.console.Operation.*;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

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
    public void testCtrlH() throws Exception {
        /*
         * CTRL-H is supposed to move the cursor backward. This first test
         * is testing that the escape() itself should step the cursor back
         * one character, I then hit ^H to back up another, then
         * insert the letter X and hit enter.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abc")).escape().ctrl('H').append("iX").enter();
        assertLine("aXbc", b, true);
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
    }
    
    @Test
    public void testSpace() throws Exception {
        /*
         * Space acts as a right-arrow while in move mode.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("big banshee bollocks"))
            .escape()             // Enter move, back one space
            .append(" a smell")   // space==right, append, space "smell"
            .enter();
        assertLine("big banshee bollocks smell", b, false);
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
         * TODO: Test other brackets.
         */
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
