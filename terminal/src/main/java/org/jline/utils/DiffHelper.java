/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for computing differences between strings with ANSI attribute awareness.
 *
 * <p>
 * The DiffHelper class provides methods for computing the differences between two strings
 * while being aware of ANSI escape sequences and text attributes. This allows for proper
 * diffing of styled text without breaking the ANSI escape sequences.
 * </p>
 *
 * <p>
 * Unlike standard diff algorithms, this implementation ensures that any text in a Diff
 * object is a valid ANSI string with properly balanced escape sequences. This is particularly
 * important when diffing AttributedStrings or other text with embedded styling information.
 * </p>
 *
 * <p>
 * The diff algorithm identifies three types of operations:
 * </p>
 * <ul>
 *   <li>DELETE - Text that exists in the first string but not in the second</li>
 *   <li>INSERT - Text that exists in the second string but not in the first</li>
 *   <li>EQUAL - Text that is common to both strings</li>
 * </ul>
 *
 * <p>
 * This class is particularly useful for implementing features like change highlighting
 * in terminal applications, where differences between versions of text need to be
 * displayed with proper styling.
 * </p>
 */
public class DiffHelper {

    /**
     * Private constructor to prevent instantiation.
     */
    private DiffHelper() {
        // Utility class
    }

    /**
     * The data structure representing a diff is a Linked list of Diff objects:
     * {Diff(Operation.DELETE, "Hello"), Diff(Operation.INSERT, "Goodbye"),
     *  Diff(Operation.EQUAL, " world.")}
     * which means: delete "Hello", add "Goodbye" and keep " world."
     */
    public enum Operation {
        DELETE,
        INSERT,
        EQUAL
    }

    /**
     * Class representing one diff operation.
     */
    public static class Diff {
        /**
         * One of: INSERT, DELETE or EQUAL.
         */
        public final Operation operation;
        /**
         * The text associated with this diff operation.
         */
        public final AttributedString text;

        /**
         * Constructor.  Initializes the diff with the provided values.
         * @param operation One of INSERT, DELETE or EQUAL.
         * @param text The text being applied.
         */
        public Diff(Operation operation, AttributedString text) {
            // Construct a diff with the specified operation and text.
            this.operation = operation;
            this.text = text;
        }

        /**
         * Display a human-readable version of this Diff.
         * @return text version.
         */
        public String toString() {
            return "Diff(" + this.operation + ",\"" + this.text + "\")";
        }
    }

    /**
     * Compute the common prefix and suffix lengths between two attributed strings
     * without any object allocation.
     *
     * <p>This is an allocation-free alternative to {@link #diff(AttributedString, AttributedString)}
     * that stores the common prefix length in {@code result[0]} and the common suffix length
     * in {@code result[1]}. The caller can derive the diff segments from these two values:
     * <ul>
     *   <li>EQUAL prefix: {@code [s1, s1 + result[0])}</li>
     *   <li>INSERT: {@code [s2 + result[0], e2 - result[1])}</li>
     *   <li>DELETE: {@code [s1 + result[0], e1 - result[1])}</li>
     *   <li>EQUAL suffix: {@code [e1 - result[1], e1)}</li>
     * </ul>
     *
     * @param text1  the old line
     * @param s1     start index in text1 (inclusive)
     * @param e1     end index in text1 (exclusive)
     * @param text2  the new line
     * @param s2     start index in text2 (inclusive)
     * @param e2     end index in text2 (exclusive)
     * @param result a reusable two-element array; on return result[0] = commonStart, result[1] = commonEnd
     */
    static void diff(AttributedString text1, int s1, int e1, AttributedString text2, int s2, int e2, int[] result) {
        int n = Math.min(e1 - s1, e2 - s2);
        int commonStart = commonPrefixLength(text1, s1, e1, text2, s2, e2, n);
        result[0] = commonStart;
        result[1] = commonSuffixLength(text1, e1, text2, e2, n - commonStart);
    }

    /**
     * Scan forward for the common prefix length, respecting hidden-range boundaries.
     */
    private static int commonPrefixLength(
            AttributedString text1, int s1, int e1, AttributedString text2, int s2, int e2, int n) {
        int commonStart = 0;
        int startHiddenRange = -1;
        while (commonStart < n
                && text1.charAt(s1 + commonStart) == text2.charAt(s2 + commonStart)
                && text1.styleCodeAt(s1 + commonStart) == text2.styleCodeAt(s2 + commonStart)) {
            if (text1.isHidden(s1 + commonStart)) {
                if (startHiddenRange < 0) startHiddenRange = commonStart;
            } else {
                startHiddenRange = -1;
            }
            commonStart++;
        }
        if (startHiddenRange >= 0
                && (((e1 - s1) > commonStart && text1.isHidden(s1 + commonStart))
                        || ((e2 - s2) > commonStart && text2.isHidden(s2 + commonStart)))) {
            commonStart = startHiddenRange;
        }
        return commonStart;
    }

    /**
     * Scan backward for the common suffix length, respecting hidden-range boundaries.
     */
    private static int commonSuffixLength(AttributedString text1, int e1, AttributedString text2, int e2, int n) {
        int commonEnd = 0;
        int startHiddenRange = -1;
        while (commonEnd < n
                && text1.charAt(e1 - commonEnd - 1) == text2.charAt(e2 - commonEnd - 1)
                && text1.styleCodeAt(e1 - commonEnd - 1) == text2.styleCodeAt(e2 - commonEnd - 1)) {
            if (text1.isHidden(e1 - commonEnd - 1)) {
                if (startHiddenRange < 0) startHiddenRange = commonEnd;
            } else {
                startHiddenRange = -1;
            }
            commonEnd++;
        }
        if (startHiddenRange >= 0
                && commonEnd < n
                && (text1.isHidden(e1 - commonEnd - 1) || text2.isHidden(e2 - commonEnd - 1))) {
            commonEnd = startHiddenRange;
        }
        return commonEnd;
    }

    /**
     * Compute the differences between two attributed strings.
     *
     * <p>Returns a list of {@link Diff} operations (EQUAL, INSERT, DELETE) that
     * transform {@code text1} into {@code text2}. Hidden character ranges are
     * kept intact — they are never split across diff segments.</p>
     *
     * @param text1 the original text
     * @param text2 the modified text
     * @return a list of diff operations
     */
    public static List<Diff> diff(AttributedString text1, AttributedString text2) {
        int l1 = text1.length();
        int l2 = text2.length();
        int[] result = new int[2];
        diff(text1, 0, l1, text2, 0, l2, result);
        int commonStart = result[0];
        int commonEnd = result[1];
        LinkedList<Diff> diffs = new LinkedList<>();
        if (commonStart > 0) {
            diffs.add(new Diff(DiffHelper.Operation.EQUAL, text1.subSequence(0, commonStart)));
        }
        if (l2 > commonStart + commonEnd) {
            diffs.add(new Diff(DiffHelper.Operation.INSERT, text2.subSequence(commonStart, l2 - commonEnd)));
        }
        if (l1 > commonStart + commonEnd) {
            diffs.add(new Diff(DiffHelper.Operation.DELETE, text1.subSequence(commonStart, l1 - commonEnd)));
        }
        if (commonEnd > 0) {
            diffs.add(new Diff(DiffHelper.Operation.EQUAL, text1.subSequence(l1 - commonEnd, l1)));
        }
        return diffs;
    }
}
