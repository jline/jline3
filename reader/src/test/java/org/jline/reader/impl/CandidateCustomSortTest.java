/*
 * Copyright (c) 2021, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.reader.Candidate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CandidateCustomSortTest extends ReaderTestSupport {

    @Test
    public void testCandidatesSortedByValue() {
        List<Candidate> candidates = Arrays.asList(
                new Candidate("cand3", "cand3", null, null, null, null, true),
                new Candidate("cand1", "cand1", null, null, null, null, true),
                new Candidate("cand2", "cand2", null, null, null, null, true));
        Collections.sort(candidates);

        assertEquals("cand1", candidates.get(0).value());
        assertEquals("cand2", candidates.get(1).value());
        assertEquals("cand3", candidates.get(2).value());
    }

    @Test
    public void testCandidatesSortedBySortProperty() {
        List<Candidate> candidates = Arrays.asList(
                new Candidate("cand3", "cand3", null, null, null, null, true, 3),
                new Candidate("cand1", "cand1", null, null, null, null, true, 1),
                new Candidate("cand2", "cand2", null, null, null, null, true, 2));
        Collections.sort(candidates);

        assertEquals("cand1", candidates.get(0).value());
        assertEquals("cand2", candidates.get(1).value());
        assertEquals("cand3", candidates.get(2).value());
    }

    @Test
    public void testCandidatesSortedByValueAndSortProperty() {
        List<Candidate> candidates = Arrays.asList(
                new Candidate("cand3", "cand3", null, null, null, null, true, -3),
                new Candidate("aaa", "cand1", null, null, null, null, true),
                new Candidate("cand2", "cand2", null, null, null, null, true, 2));
        Collections.sort(candidates);

        assertEquals("cand3", candidates.get(0).value());
        assertEquals("aaa", candidates.get(1).value());
        assertEquals("cand2", candidates.get(2).value());
    }

    @Test
    public void testCandidatesSortedByCornerSortProps() {
        List<Candidate> candidates = Arrays.asList(
                new Candidate("cand1", "cand1", null, null, null, null, true, 1),
                new Candidate("cand2", "cand2", null, null, null, null, true, Integer.MAX_VALUE),
                new Candidate("cand3", "cand3", null, null, null, null, true, Integer.MIN_VALUE));
        Collections.sort(candidates);

        assertEquals("cand3", candidates.get(0).value());
        assertEquals("cand1", candidates.get(1).value());
        assertEquals("cand2", candidates.get(2).value());
    }

    @Test
    public void testCompletionSort() throws Exception {
        reader.setCompleter((reader, line, candidates) -> {
            candidates.add(new Candidate("foo", "foo", null, null, null, null, true, 0));
            candidates.add(new Candidate("bar", "bar", null, null, null, null, true, 1));
            candidates.add(new Candidate("zoo", "zoo", null, null, null, null, true, 2));
        });
        assertBuffer("foo", new TestBuffer("").tab().tab());
    }
}
