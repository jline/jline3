/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.util.function.Function;

/**
 * Interpolation.
 * Borrowed and adapted from Apache Felix Utils module.
 */
public final class InterpolationHelper {

    private InterpolationHelper() {}

    private static final char ESCAPE_CHAR = '\\';
    private static final String DELIM_START = "@{";
    private static final String DELIM_STOP = "}";
    private static final String MARKER = "@__";

    public static String substVars(String val, Function<String, String> callback, boolean defaultsToEmptyString)
            throws IllegalArgumentException {
        return unescape(doSubstVars(val, callback, defaultsToEmptyString));
    }

    private static String doSubstVars(String val, Function<String, String> callback, boolean defaultsToEmptyString)
            throws IllegalArgumentException {
        // Assume we have a value that is something like:
        // "leading @{foo.@{bar}} middle @{baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int startDelim;
        int stopDelim = -1;
        do {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            while (stopDelim > 0 && val.charAt(stopDelim - 1) == ESCAPE_CHAR) {
                stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            }

            // Find the matching starting "@{" variable delimiter
            // by looping until we find a start delimiter that is
            // greater than the stop delimiter we have found.
            startDelim = val.indexOf(DELIM_START);
            while (stopDelim >= 0) {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim)) {
                    break;
                } else if (idx < stopDelim) {
                    startDelim = idx;
                }
            }
        } while (startDelim >= 0 && stopDelim >= 0 && stopDelim < startDelim + DELIM_START.length());

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) || (stopDelim < 0)) {
            return val;
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable = val.substring(startDelim + DELIM_START.length(), stopDelim);

        String substValue = null;
        // Get the value of the deepest nested variable placeholder.
        if (variable.length() > 0 && callback != null) {
            substValue = callback.apply(variable);
        }

        if (substValue == null) {
            if (defaultsToEmptyString) {
                substValue = "";
            } else {
                // alters the original token to avoid infinite recursion
                // altered tokens are reverted in unescape()
                substValue = MARKER + "{" + variable + "}";
            }
        }

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim) + substValue + val.substring(stopDelim + DELIM_STOP.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = doSubstVars(val, callback, defaultsToEmptyString);

        // Return the value.
        return val;
    }

    private static String unescape(String val) {
        val = val.replaceAll(MARKER, "@");
        int escape = val.indexOf(ESCAPE_CHAR);
        while (escape >= 0 && escape < val.length() - 1) {
            char c = val.charAt(escape + 1);
            if (c == '{' || c == '}' || c == ESCAPE_CHAR) {
                val = val.substring(0, escape) + val.substring(escape + 1);
            }
            escape = val.indexOf(ESCAPE_CHAR, escape + 1);
        }
        return val;
    }
}
