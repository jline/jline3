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
import java.util.Arrays;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;

public class CompletionWithParserTest extends ReaderTestSupport {

    @Test
    public void testComplete() throws IOException {
        reader.setCompleter((reader, line, candidates) -> candidates.add(new Candidate(
                /* value    = */ "range",
                /* displ    = */ "range",
                /* group    = */ null,
                /* descr    = */ null,
                /* suffix   = */ null,
                /* key      = */ null,
                /* complete = */ false)));
        reader.setParser((line, cursor, context) -> new ParsedLine() {
            @Override
            public String word() {
                return "";
            }

            @Override
            public int wordCursor() {
                return 0;
            }

            @Override
            public int wordIndex() {
                return 3;
            }

            @Override
            public List<String> words() {
                return Arrays.asList("{", "List", ".", "", "}");
            }

            @Override
            public String line() {
                return "{List.}";
            }

            @Override
            public int cursor() {
                return 6;
            }
        });

        assertBuffer("{List.range}", new TestBuffer("{List.}").left().tab());
    }
}
