/*
 * Copyright (c) 2002-2018, the original author or authors.
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
import org.jline.reader.Completer;
import org.jline.reader.CompletingParsedLine;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

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
