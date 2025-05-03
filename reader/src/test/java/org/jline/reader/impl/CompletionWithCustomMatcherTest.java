/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.IOException;
import java.util.Map;

import org.jline.reader.*;
import org.junit.jupiter.api.Test;

public class CompletionWithCustomMatcherTest extends ReaderTestSupport {

    @Test
    public void testComplete() throws IOException {
        reader.setCompletionMatcher(new CompletionMatcherImpl() {

            @Override
            public void compile(
                    Map<LineReader.Option, Boolean> options,
                    boolean prefix,
                    CompletingParsedLine line,
                    boolean caseInsensitive,
                    int errors,
                    String originalGroupName) {
                reset(caseInsensitive);
                defaultMatchers(options, prefix, line, caseInsensitive, errors, originalGroupName);
                if (line.word().length() > 0 && !prefix) {
                    // add custom matcher before typo matcher
                    int pos = matchers.size() + (LineReader.Option.COMPLETE_MATCHER_TYPO.isSet(options) ? -1 : 0);
                    matchers.add(pos, simpleMatcher(candidate -> camelMatch(line.word(), 0, candidate, 0)));
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
