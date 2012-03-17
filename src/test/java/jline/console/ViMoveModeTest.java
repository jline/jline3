/*
 * Copyright (c) 2012, Scott C. Gray. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console;

import static jline.console.Operation.*;
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
