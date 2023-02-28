/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TmuxTest {

    @Test
    public void testLayoutParse() {
        Tmux.Layout l = Tmux.Layout.parse(
                "b7c7,148x44,0,0[148x26,0,0{69x26,0,0,0,78x26,70,0,3},148x17,0,27{74x17,0,27,1,36x17,75,27,4,36x17,112,27,5}]");
        assertNotNull(l);
    }

    @Test
    public void testLayoutResize() {
        Tmux.Layout l = Tmux.Layout.parse(
                "b7c7,148x44,0,0[148x26,0,0{69x26,0,0,0,78x26,70,0,3},148x17,0,27{74x17,0,27,1,36x17,75,27,4,36x17,112,27,5}]");
        l.resize(140, 44);
        assertEquals(
                "ebac,140x44,0,0[140x26,0,0{65x26,0,0,0,74x26,66,0,0},140x17,0,27{71x17,0,27,0,33x17,72,27,0,34x17,106,27,0}]",
                l.dump());
        System.out.println(l.dump());
    }
}
