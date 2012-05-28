/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.readline;

import jline.console.KeyMap;
import jline.console.Operation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class KeyMapTest {

    @Test
    public void testBound() throws Exception {

        KeyMap map = KeyMap.emacs();

        assertEquals( Operation.COMPLETE, map.getBound("\u001B" + KeyMap.CTRL_OB) );
        assertEquals( Operation.BACKWARD_WORD, map.getBound(KeyMap.ESCAPE + "b") );

        map.bindIfNotBound("\033[0A", Operation.PREVIOUS_HISTORY);
        assertEquals( Operation.PREVIOUS_HISTORY, map.getBound("\033[0A") );


        map.bind( "\033[0AB", Operation.NEXT_HISTORY );
        assertTrue( map.getBound("\033[0A") instanceof KeyMap );
        assertEquals( Operation.NEXT_HISTORY , map.getBound("\033[0AB") );
    }

}
