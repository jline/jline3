/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex utilities that guard against catastrophic backtracking (ReDoS).
 *
 * <p>Java's {@code java.util.regex} engine uses backtracking, so patterns with
 * nested quantifiers (e.g. {@code (a+)+b}) can take exponential time on
 * non-matching input. This class wraps input in a {@link CharSequence} that
 * enforces a wall-clock deadline, throwing {@link RegexTimeoutException} if
 * matching takes too long.</p>
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>For simple boolean checks, use {@link #matches} or {@link #find} —
 *       they catch timeouts and return {@code false}.</li>
 *   <li>When you need the {@link Matcher} (e.g. for a find loop or to read
 *       match groups), use {@link #matcher} and catch
 *       {@link RegexTimeoutException} yourself.</li>
 *   <li>For glob-style patterns (only {@code *} is special), use
 *       {@link #compileGlob} to compile a {@link Pattern} that properly
 *       escapes literal characters. Note: {@code compileGlob} only builds
 *       the pattern; to get timeout protection, pass the result through
 *       {@link #matcher}, {@link #matches}, or {@link #find}.</li>
 * </ul>
 */
public final class SafeRegex {

    /** Default timeout for regex matching operations. */
    private static final long DEFAULT_TIMEOUT_MS = 1500;

    /**
     * How often (in {@code charAt} calls) the deadline is checked.
     * Checking every call would add measurable overhead; checking every
     * 1024 calls keeps the cost negligible while still catching runaway
     * backtracking within milliseconds on typical input.
     */
    private static final int CHECK_INTERVAL = 1024;

    private SafeRegex() {}

    // ---- matcher factory ---------------------------------------------------

    /**
     * Create a {@link Matcher} that will throw {@link RegexTimeoutException}
     * if matching exceeds the default timeout.
     */
    public static Matcher matcher(Pattern pattern, CharSequence input) {
        return matcher(pattern, input, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Create a {@link Matcher} that will throw {@link RegexTimeoutException}
     * if matching exceeds the given timeout.  The timeout starts lazily on
     * the first deadline check during matching (not when the {@link Matcher}
     * is created), so it measures actual matching time.
     */
    public static Matcher matcher(Pattern pattern, CharSequence input, long timeoutMs) {
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        return pattern.matcher(new TimeoutCharSequence(input, timeoutNanos));
    }

    // ---- convenience boolean methods ---------------------------------------

    /**
     * Test whether the pattern matches the entire input, with timeout
     * protection. Returns {@code false} on timeout.
     */
    public static boolean matches(Pattern pattern, CharSequence input) {
        try {
            return matcher(pattern, input).matches();
        } catch (RegexTimeoutException e) {
            return false;
        }
    }

    /**
     * Test whether the pattern is found anywhere in the input, with timeout
     * protection. Returns {@code false} on timeout.
     */
    public static boolean find(Pattern pattern, CharSequence input) {
        try {
            return matcher(pattern, input).find();
        } catch (RegexTimeoutException e) {
            return false;
        }
    }

    // ---- glob compilation --------------------------------------------------

    /**
     * Compile a glob-style pattern into a {@link Pattern}.
     *
     * <p>Only {@code *} (match any string) and {@code \} (escape) are
     * special; every other character is regex-quoted so it matches
     * literally. This is suitable for user-facing wildcard syntax where
     * full regex power is not intended.</p>
     *
     * @param globPattern the glob pattern (e.g. {@code "foo*bar"})
     * @return a compiled regex pattern
     */
    public static Pattern compileGlob(String globPattern) {
        return compileGlob(globPattern, 0);
    }

    /**
     * Compile a glob-style pattern into a {@link Pattern} with the given
     * regex flags.
     *
     * @param globPattern the glob pattern
     * @param flags       regex flags (e.g. {@link Pattern#DOTALL})
     * @return a compiled regex pattern
     */
    public static Pattern compileGlob(String globPattern, int flags) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < globPattern.length()) {
            char ch = globPattern.charAt(i);
            if (ch == '\\' && i + 1 < globPattern.length()) {
                appendQuoted(sb, globPattern.charAt(i + 1));
                i += 2;
            } else if (ch == '*') {
                sb.append(".*");
                i++;
            } else {
                appendQuoted(sb, ch);
                i++;
            }
        }
        return Pattern.compile(sb.toString(), flags);
    }

    // ---- internal ----------------------------------------------------------

    private static void appendQuoted(StringBuilder sb, char ch) {
        // Quote individual regex metacharacters inline rather than using
        // Pattern.quote() (\Q...\E) to keep the generated regex readable.
        if ("\\^$.|?*+()[]{}".indexOf(ch) >= 0) {
            sb.append('\\');
        }
        sb.append(ch);
    }

    /**
     * A {@link CharSequence} wrapper that enforces a wall-clock deadline.
     *
     * <p>The Java regex engine calls {@link #charAt(int)} for every position
     * it considers during matching.  During catastrophic backtracking the
     * call count explodes; we check {@link System#nanoTime()} every
     * {@value #CHECK_INTERVAL} calls and throw if the deadline has passed.</p>
     *
     * <p>The deadline is lazily initialised on the first {@code charAt}
     * check so the timeout measures actual matching time, not the gap
     * between {@link Matcher} creation and first use.  Instances created
     * via {@link #subSequence} share the same deadline array, so the
     * timeout covers the entire matching operation.</p>
     */
    private static final class TimeoutCharSequence implements CharSequence {

        private final CharSequence inner;
        private final long timeoutNanos;
        /** Shared across {@link #subSequence} calls; element 0 holds the deadline (0 = not yet started). */
        private final long[] sharedDeadline;

        private int calls;

        TimeoutCharSequence(CharSequence inner, long timeoutNanos) {
            this.inner = inner;
            this.timeoutNanos = timeoutNanos;
            this.sharedDeadline = new long[1];
        }

        private TimeoutCharSequence(CharSequence inner, long timeoutNanos, long[] sharedDeadline) {
            this.inner = inner;
            this.timeoutNanos = timeoutNanos;
            this.sharedDeadline = sharedDeadline;
        }

        @Override
        public char charAt(int index) {
            if (++calls % CHECK_INTERVAL == 0) {
                long now = System.nanoTime();
                if (sharedDeadline[0] == 0) {
                    sharedDeadline[0] = now + timeoutNanos;
                } else if (now > sharedDeadline[0]) {
                    throw new RegexTimeoutException();
                }
            }
            return inner.charAt(index);
        }

        @Override
        public int length() {
            return inner.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new TimeoutCharSequence(inner.subSequence(start, end), timeoutNanos, sharedDeadline);
        }

        @Override
        public String toString() {
            return inner.toString();
        }
    }
}
