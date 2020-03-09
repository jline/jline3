/*
 * Copyright (c) 2002-2028, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.*;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CompletionWithCustomMatcherTest extends ReaderTestSupport {

    @Test
    public void testComplete() throws IOException {
        reader.setMatcherFactory(new MatcherFactory() {
            final Matchers.CustomMatcherType camelMatcherType = new Matchers.CustomMatcherType("camel case");

            @Override
            public void updateMatchers(boolean prefix, CompletingParsedLine line, Map<LineReader.Option, Boolean> options,
                                           int errors, String originalGroupName,
                                           LinkedHashMap<Matchers.MatcherType, Function<Map<String, List<Candidate>>, Map<String, List<Candidate>>>> matchers) {
                matchers.put(camelMatcherType, simpleMatcher(candidate ->
                    camelMatch(line.word(), 0, candidate, 0)
                ));
            }

            private boolean camelMatch(String word, int i, String candidate, int j) {
                if (word.length() <= i) {
                    return true;
                } else {
                    char c = word.charAt(i);
                    if (candidate.length() <= j) {
                        return false;
                    }
                    if (c == candidate.charAt(j)) {
                        if (camelMatch(word, i + 1, candidate, j + 1)) {
                            return true;
                        }
                    }
                    for (int j1 = j; j1 < candidate.length(); j1++) {
                        if (Character.isUpperCase(candidate.charAt(j1))) {
                            if (Character.toUpperCase(c) == candidate.charAt(j1)) {
                                if (camelMatch(word, i + 1, candidate, j1 + 1)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            }
        });
        reader.setCompleter((reader, line, candidates) -> {
            candidates.add(newCandidate("getMethod"));
            candidates.add(newCandidate("getMethods"));
            candidates.add(newCandidate("getDeclaredMethod"));
            candidates.add(newCandidate("getDeclaredMethods"));
        });

        reader.unsetOpt(LineReader.Option.AUTO_LIST);
        reader.setOpt(LineReader.Option.AUTO_MENU);
        assertBuffer("getDeclaredMethod", new TestBuffer("gdm").left().tab());
        assertBuffer("getDeclaredMethods", new TestBuffer("gdm").left().tab().tab());
        assertBuffer("getDeclaredMethods", new TestBuffer("gDMethods").left().tab());

        assertBuffer("getMethod", new TestBuffer("getMe").left().tab());

        assertBuffer("getDeclaredMethods", new TestBuffer("gmethods").left().tab());
        assertBuffer("getMethods", new TestBuffer("gmethods").left().tab().tab());
    }

    private Candidate newCandidate(String name) {
        return new Candidate(
            /* value    = */ name,
            /* displ    = */ name,
            /* group    = */ null,
            /* descr    = */ null,
            /* suffix   = */ null,
            /* key      = */ null,
            /* complete = */ false);
    }
}
