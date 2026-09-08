/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for {@link DefaultPrinter}.
 */
class DefaultPrinterTest {

    /**
     * Verifies that {@code isQuotedString} completes in constant time, even for
     * pathological inputs that would cause catastrophic backtracking with the
     * old regex-based implementation.
     *
     * @see <a href="https://github.com/jline/jline3/pull/2249">PR #2249</a>
     */
    @Test
    void isQuotedStringDoesNotBacktrack() throws Exception {
        Method m = DefaultPrinter.class.getDeclaredMethod("isQuotedString", String.class);
        m.setAccessible(true);

        // A pathological input for the old regex "\"(\\.|[^\"])*\"|'(\\.|[^'])*'"
        // With the regex, this causes exponential backtracking.  With the O(1)
        // char-check it returns instantly.
        char[] chars = new char[50_001];
        Arrays.fill(chars, '.');
        chars[0] = '"';
        String pathological = new String(chars);

        assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
            boolean result = (boolean) m.invoke(null, pathological);
            // Not a valid quoted string (no closing quote)
            assertFalse(result);
        });
    }

    @Test
    void isQuotedStringBasicCases() throws Exception {
        Method m = DefaultPrinter.class.getDeclaredMethod("isQuotedString", String.class);
        m.setAccessible(true);

        // Double-quoted
        assertTrue((boolean) m.invoke(null, "\"hello\""));
        // Single-quoted
        assertTrue((boolean) m.invoke(null, "'hello'"));
        // Minimal quoted strings
        assertTrue((boolean) m.invoke(null, "\"\""));
        assertTrue((boolean) m.invoke(null, "''"));
        // Not quoted
        assertFalse((boolean) m.invoke(null, "hello"));
        assertFalse((boolean) m.invoke(null, "\""));
        assertFalse((boolean) m.invoke(null, "'"));
        assertFalse((boolean) m.invoke(null, ""));
        // Mismatched quotes
        assertFalse((boolean) m.invoke(null, "\"hello'"));
        assertFalse((boolean) m.invoke(null, "'hello\""));
        // Embedded quotes (intentional relaxation — accepted by the new implementation)
        assertTrue((boolean) m.invoke(null, "\"a\"b\""));
    }
}
