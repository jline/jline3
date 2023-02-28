/*
 * Copyright (c) 2002-2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.jline.reader.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompletionMatcherTest {

    private CompletionMatcher compileCompletionMatcher(String line) {
        CompletionMatcher completionMatcher = new CompletionMatcherImpl();
        Parser parser = new DefaultParser();
        completionMatcher.compile(
                new HashMap<>(), false, LineReaderImpl.wrap(parser.parse(line, line.length())), true, 0, "");
        return completionMatcher;
    }

    @Test
    public void uniqueCandidates() {
        Candidate c = new Candidate("foo");
        assertEquals(
                "Expected only one element",
                1,
                compileCompletionMatcher("").matches(Arrays.asList(c, c)).size());
    }

    @Test
    public void test() {
        List<Candidate> candidates = Arrays.asList(new Candidate("foo"), new Candidate("foobar"), new Candidate("bar"));
        CompletionMatcher completionMatcher = compileCompletionMatcher("foo");
        List<Candidate> matches = completionMatcher.matches(candidates);
        assertEquals("Number of matches", 2, matches.size());
        Candidate candidate = completionMatcher.exactMatch();
        assertEquals("Exact match", "foo", (candidate != null ? candidate.value() : null));
        assertEquals("Common prefix", "foo", completionMatcher.getCommonPrefix());
    }
}
