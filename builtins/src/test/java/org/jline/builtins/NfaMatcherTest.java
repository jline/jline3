/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NfaMatcherTest {

    @Test
    public void testMultiplicity() {
        assertFalse(match("C5"));
        assertTrue(match("C5", "arg"));
        assertFalse(match("C5", "arg", "foo"));
        assertTrue(match("C5?"));
        assertTrue(match("C5?", "arg"));
        assertFalse(match("C5?", "arg", "foo"));
        assertFalse(match("C5+"));
        assertTrue(match("C5+", "arg"));
        assertTrue(match("C5+", "arg", "foo"));
        assertTrue(match("C5*"));
        assertTrue(match("C5*", "arg"));
        assertTrue(match("C5*", "arg", "foo"));
    }

    @Test
    public void testWeird() {
        assertTrue(match("a? a? a? a a a", "a", "a", "a", "a"));
        assertTrue(match("a ? * +", "a", "a", "a", "a"));
    }

    @Test
    public void testConcat() {
        assertTrue(match("C4? C5+", "arg", "foo"));
        assertTrue(match("(C1 | C2 | C3)* C4? C5+", "arg", "foo"));
        assertTrue(match("(C1 | C2 | C3)* C4? C5+", "--opt1=a", "--opt2=b", "--myopt", "arg", "foo"));
    }

    @Test
    public void testPartial() {
        assertEquals(asSet("C1", "C2", "C3", "C4", "C5"), matchPartial("(C1 | C2 | C3)* C4? C5+", "--opt1=a"));
        assertEquals(asSet("C5"), matchPartial("(C1 | C2 | C3)* C4? C5+", "--opt1=a", "--myopt"));
    }

    @Test
    public void testPartial2() {
        assertEquals(asSet("C3"), matchPartial(" ( C1 ( C2 ( C3 )  | C4 | C5 )  ) ", "--opt1", "--opt2"));
    }

    boolean match(String regexp, String... args) {
        return new NfaMatcher<>(regexp, this::matchArg).match(Arrays.asList(args));
    }

    Set<String> matchPartial(String regexp, String... args) {
        return new NfaMatcher<>(regexp, this::matchArg).matchPartial(Arrays.asList(args));
    }

    boolean matchArg(String arg, String name) {
        switch (name) {
            case "C1":
                return arg.startsWith("--opt1");
            case "C2":
                return arg.startsWith("--opt2");
            case "C3":
                return arg.startsWith("--opt3");
            case "C4":
                return arg.startsWith("--myopt");
            case "C5":
                return true;
            case "a":
                return arg.equals("a");
            default:
                throw new IllegalStateException("Unsupported: " + name);
        }
    }

    static Set<String> asSet(String... ts) {
        Set<String> s = new HashSet<>();
        for (String t : ts) {
            s.add(t);
        }
        return s;
    }
}
