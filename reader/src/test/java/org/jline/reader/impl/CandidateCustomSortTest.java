/*
 * Copyright (c) the original author(s).
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
import org.jline.reader.LineReader.Option;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CandidateCustomSortTest extends ReaderTestSupport {

    @Test
    void testCandidatesSortedByValue() {
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
    void testCandidatesSortedBySortProperty() {
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
    void testCandidatesSortedByValueAndSortProperty() {
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
    void testCandidatesSortedByCornerSortProps() {
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
    void testCompletionSort() throws Exception {
        reader.setCompleter((reader, line, candidates) -> {
            candidates.add(new Candidate("foo", "foo", null, null, null, null, true, 0));
            candidates.add(new Candidate("bar", "bar", null, null, null, null, true, 1));
            candidates.add(new Candidate("zoo", "zoo", null, null, null, null, true, 2));
        });
        assertBuffer("foo", new TestBuffer("").tab().tab());
    }

    @Test
    void testCustomSortIdenticalSortValueGroups() {
        List<Candidate> candidates = Arrays.asList(
                new Candidate("foo", "foo", null, null, null, null, true, 1),
                new Candidate("bar", "bar", null, null, null, null, true, 1),
                new Candidate("zoo", "zoo", null, null, null, null, true, 1),
                new Candidate("zzz", "zzz", null, null, null, null, true, 0));
        reader.setOpt(Option.GROUP);
        String postResult = reader.computePost(candidates, null, null, "").post.toString();
        assertEquals("zzz   bar   foo   zoo", postResult);
    }

    @Test
    void testCustomSortIdenticalSortValue() {
        List<Candidate> candidates = Arrays.asList(
                new Candidate("foo", "foo", null, null, null, null, true, 1),
                new Candidate("bar", "bar", null, null, null, null, true, 1),
                new Candidate("zoo", "zoo", null, null, null, null, true, 1),
                new Candidate("zzz", "zzz", null, null, null, null, true, 0));
        reader.unsetOpt(Option.GROUP);
        String postResult = reader.computePost(candidates, null, null, "").post.toString();
        assertEquals("zzz   bar   foo   zoo", postResult);
    }
}
