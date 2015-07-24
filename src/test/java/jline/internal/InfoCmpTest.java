/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class InfoCmpTest {

    @Test
    public void testInfoCmp() {
        Set<String> bools = new HashSet<String>();
        Map<String, Integer> ints = new HashMap<String, Integer>();
        Map<String, String> strings = new HashMap<String, String>();

        String infocmp = InfoCmp.getAnsiCaps();
        InfoCmp.parseInfoCmp(infocmp, bools, ints, strings);
        assertEquals(12, bools.size());
        assertTrue(strings.containsKey("acsc"));
    }
}
