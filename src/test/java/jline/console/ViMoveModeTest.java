package jline.console;

import static jline.console.Operation.*;

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
         * Ctrl-J is enter. I want to test to make sure that I am 
         * re-entering insert mode when enter is hit.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        Buffer b = (new Buffer("abc")).escape().enter();
        assertLine("abc", b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
        
        /*
         * This sort of tests the same thing by actually enter
         * characters after the first enter.
         */
        console.setKeyMap(KeyMap.VI_INSERT);
        b = (new Buffer("abc")).escape().enter()
            .append("def").enter();
        assertLine("def", b, true);
        assertTrue(console.isKeyMap(KeyMap.VI_INSERT));
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
    }
    
    
    /*
     * TODO - Test arrow key bindings
     */
}
