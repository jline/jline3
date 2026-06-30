/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

/**
 * Thrown when a regular expression match exceeds its time budget.
 *
 * <p>This is an unchecked exception because it is thrown from within
 * {@link CharSequence#charAt(int)}, which the {@code java.util.regex}
 * engine calls during matching. Callers that use
 * {@link SafeRegex#matcher(java.util.regex.Pattern, CharSequence)}
 * directly should catch this exception; the convenience methods
 * {@link SafeRegex#matches} and {@link SafeRegex#find} catch it
 * internally and return {@code false}.</p>
 */
public class RegexTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegexTimeoutException() {
        super("Regular expression matching timed out");
    }

    public RegexTimeoutException(String message) {
        super(message);
    }
}
