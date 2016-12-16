/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jline.utils.InfoCmp.Capability;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class InfoCmpTest {

    @Test
    public void testInfoCmp() {
        Set<Capability> bools = new HashSet<>();
        Map<Capability, Integer> ints = new HashMap<>();
        Map<Capability, String> strings = new HashMap<>();

        String infocmp = InfoCmp.getLoadedInfoCmp("ansi");
        InfoCmp.parseInfoCmp(infocmp, bools, ints, strings);
        assertEquals(4, bools.size());
        assertTrue(strings.containsKey(Capability.byName("acsc")));
    }
}
