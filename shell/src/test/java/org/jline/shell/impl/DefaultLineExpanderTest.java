/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import org.jline.shell.CommandSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultLineExpander}.
 */
public class DefaultLineExpanderTest {

    private DefaultLineExpander expander;
    private CommandSession session;

    @BeforeEach
    void setUp() {
        expander = new DefaultLineExpander();
        session = new CommandSession();
        session.put("NAME", "world");
        session.put("DIR", "/home/user");
        session.put("EMPTY", "");
    }

    @Test
    void expandDollarVar() {
        assertEquals("hello world", expander.expand("hello $NAME", session));
    }

    @Test
    void expandBracedVar() {
        assertEquals("hello world", expander.expand("hello ${NAME}", session));
    }

    @Test
    void expandTilde() {
        String home = System.getProperty("user.home");
        assertEquals(home + "/docs", expander.expand("~/docs", session));
    }

    @Test
    void tildeAlone() {
        String home = System.getProperty("user.home");
        assertEquals(home, expander.expand("~", session));
    }

    @Test
    void tildeInMiddleOfWord() {
        // ~ not at word start should not expand
        assertEquals("foo~bar", expander.expand("foo~bar", session));
    }

    @Test
    void tildeAfterSpace() {
        String home = System.getProperty("user.home");
        assertEquals("cd " + home, expander.expand("cd ~", session));
    }

    @Test
    void singleQuoteProtection() {
        assertEquals("hello '$NAME'", expander.expand("hello '$NAME'", session));
    }

    @Test
    void doubleQuoteExpansion() {
        assertEquals("hello \"world\"", expander.expand("hello \"$NAME\"", session));
    }

    @Test
    void unknownVarLeftAsIs() {
        assertEquals("hello $UNKNOWN", expander.expand("hello $UNKNOWN", session));
    }

    @Test
    void unknownBracedVarLeftAsIs() {
        assertEquals("hello ${UNKNOWN}", expander.expand("hello ${UNKNOWN}", session));
    }

    @Test
    void dollarAtEndOfLine() {
        // $ at end of line with no following char
        assertEquals("hello$", expander.expand("hello$", session));
    }

    @Test
    void envFallback() {
        // PATH is almost certainly set in the environment
        String path = System.getenv("PATH");
        if (path != null) {
            assertEquals(path, expander.expand("$PATH", session));
        }
    }

    @Test
    void multipleExpansions() {
        session.put("A", "1");
        session.put("B", "2");
        assertEquals("1 and 2", expander.expand("$A and $B", session));
    }

    @Test
    void bracedVarInPath() {
        assertEquals("/home/user/file.txt", expander.expand("${DIR}/file.txt", session));
    }

    @Test
    void emptyVarExpands() {
        assertEquals("hello ", expander.expand("hello $EMPTY", session));
    }

    @Test
    void nullInput() {
        assertNull(expander.expand(null, session));
    }

    @Test
    void emptyInput() {
        assertEquals("", expander.expand("", session));
    }

    @Test
    void noExpansionNeeded() {
        assertEquals("hello world", expander.expand("hello world", session));
    }

    @Test
    void escapedDollar() {
        assertEquals("hello \\$NAME", expander.expand("hello \\$NAME", session));
    }

    @Test
    void varWithUnderscore() {
        session.put("MY_VAR", "value");
        assertEquals("value", expander.expand("$MY_VAR", session));
    }

    @Test
    void nullSession() {
        // Should fall back to env-only
        String path = System.getenv("PATH");
        if (path != null) {
            assertEquals(path, expander.expand("$PATH", null));
        }
    }

    // --- Advanced braced expression tests ---

    @Test
    void defaultValueWhenUnset() {
        assertEquals("fallback", expander.expand("${MISSING:-fallback}", session));
    }

    @Test
    void defaultValueWhenEmpty() {
        assertEquals("fallback", expander.expand("${EMPTY:-fallback}", session));
    }

    @Test
    void defaultValueWhenSet() {
        assertEquals("world", expander.expand("${NAME:-fallback}", session));
    }

    @Test
    void assignDefaultWhenUnset() {
        assertEquals("assigned", expander.expand("${NEWVAR:=assigned}", session));
        assertEquals("assigned", session.get("NEWVAR"));
    }

    @Test
    void assignDefaultWhenSet() {
        assertEquals("world", expander.expand("${NAME:=fallback}", session));
    }

    @Test
    void alternateValueWhenSet() {
        assertEquals("alt", expander.expand("${NAME:+alt}", session));
    }

    @Test
    void alternateValueWhenUnset() {
        assertEquals("", expander.expand("${MISSING:+alt}", session));
    }

    @Test
    void alternateValueWhenEmpty() {
        assertEquals("", expander.expand("${EMPTY:+alt}", session));
    }

    @Test
    void errorWhenUnset() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> expander.expand("${MISSING:?not found}", session));
        assertTrue(ex.getMessage().contains("MISSING"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void errorWhenSet() {
        // Should not throw
        assertEquals("world", expander.expand("${NAME:?not found}", session));
    }

    @Test
    void defaultInContext() {
        assertEquals("hello fallback end", expander.expand("hello ${MISSING:-fallback} end", session));
    }
}
