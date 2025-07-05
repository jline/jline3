/*
 * Copyright (c) 2002-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SortComparator class functionality.
 */
public class SortComparatorTest {

    @Test
    void testBasicSorting() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("zebra", "apple", "banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("apple", "banana", "zebra"), lines);
    }

    @Test
    void testCaseInsensitiveSorting() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(true, false, false, false, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("Zebra", "apple", "Banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("apple", "Banana", "Zebra"), lines);
    }

    @Test
    void testReverseSorting() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, true, false, false, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("apple", "banana", "zebra");
        lines.sort(comparator);

        assertEquals(Arrays.asList("zebra", "banana", "apple"), lines);
    }

    @Test
    void testNumericSorting() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, true, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("10", "2", "100", "1");
        lines.sort(comparator);

        assertEquals(Arrays.asList("1", "2", "10", "100"), lines);
    }

    @Test
    void testIgnoreLeadingBlanks() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, true, false, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("  zebra", " apple", "banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList(" apple", "banana", "  zebra"), lines);
    }

    @Test
    void testFieldSeparator() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("2"));

        List<String> lines = Arrays.asList("a:zebra", "b:apple", "c:banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("b:apple", "c:banana", "a:zebra"), lines);
    }

    @Test
    void testMultipleKeys() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("1", "2"));

        List<String> lines = Arrays.asList("b:zebra", "a:apple", "b:apple", "a:zebra");
        lines.sort(comparator);

        assertEquals(Arrays.asList("a:apple", "a:zebra", "b:apple", "b:zebra"), lines);
    }

    @Test
    void testKeyWithModifiers() {
        // Test key with numeric modifier - use only numeric values
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("2n"));

        List<String> lines = Arrays.asList("x:10", "y:2", "z:100", "w:1");
        lines.sort(comparator);

        assertEquals(Arrays.asList("w:1", "y:2", "x:10", "z:100"), lines);
    }

    @Test
    void testKeyWithReverseModifier() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("2r"));

        List<String> lines = Arrays.asList("a:apple", "b:zebra", "c:banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("b:zebra", "c:banana", "a:apple"), lines);
    }

    @Test
    void testKeyWithCaseInsensitiveModifier() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("2f"));

        List<String> lines = Arrays.asList("a:Zebra", "b:apple", "c:Banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("b:apple", "c:Banana", "a:Zebra"), lines);
    }

    @Test
    void testKeyWithIgnoreBlanksModifier() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("2b"));

        List<String> lines = Arrays.asList("a:  zebra", "b: apple", "c:banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("b: apple", "c:banana", "a:  zebra"), lines);
    }

    @Test
    void testComplexKey() {
        // Test key with field range and multiple modifiers - use only numeric values
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, ':', Arrays.asList("2,3nr"));

        List<String> lines = Arrays.asList("x:10:5", "y:2:8", "z:100:3");
        lines.sort(comparator);

        assertEquals(Arrays.asList("z:100:3", "x:10:5", "y:2:8"), lines);
    }

    @Test
    void testFloatingPointNumbers() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, true, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("3.14", "2.71", "1.41", "10.5");
        lines.sort(comparator);

        assertEquals(Arrays.asList("1.41", "2.71", "3.14", "10.5"), lines);
    }

    @Test
    void testScientificNotation() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, true, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("1e3", "2e2", "3e1", "1e4");
        lines.sort(comparator);

        assertEquals(Arrays.asList("3e1", "2e2", "1e3", "1e4"), lines);
    }

    @Test
    void testNegativeNumbers() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, true, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("-10", "5", "-2", "0");
        lines.sort(comparator);

        assertEquals(Arrays.asList("-10", "-2", "0", "5"), lines);
    }

    @Test
    void testMixedContent() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("file10.txt", "file2.txt", "file1.txt", "file20.txt");
        lines.sort(comparator);

        // String sorting (not numeric)
        assertEquals(Arrays.asList("file1.txt", "file10.txt", "file2.txt", "file20.txt"), lines);
    }

    @Test
    void testEmptyLines() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, '\0', Arrays.asList("1"));

        List<String> lines = Arrays.asList("", "apple", "", "banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("", "", "apple", "banana"), lines);
    }

    @Test
    void testWhitespaceHandling() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, '\0', Arrays.asList("2"));

        List<String> lines = Arrays.asList("a zebra", "b apple", "c banana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("b apple", "c banana", "a zebra"), lines);
    }

    @Test
    void testTabSeparatedFields() {
        PosixCommands.SortComparator comparator =
                new PosixCommands.SortComparator(false, false, false, false, '\t', Arrays.asList("2"));

        List<String> lines = Arrays.asList("a\tzebra", "b\tapple", "c\tbanana");
        lines.sort(comparator);

        assertEquals(Arrays.asList("b\tapple", "c\tbanana", "a\tzebra"), lines);
    }
}
