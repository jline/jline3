/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.junit.Test;

import static org.jline.builtins.Completers.TreeCompleter.node;
import static org.junit.Assert.assertEquals;

public class CompletersTest {

    @Test
    public void testTreeCompleter() {
        List<String> words = new ArrayList<>();
        Completer test = new Completer() {
            @Override
            public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                words.add(line.word());
                candidates.add(new Candidate("Param1"));
                candidates.add(new Candidate("Param2"));
            }
        };
        Completer completer = new Completers.TreeCompleter(
                node("Command1", node("Option1", node(test)), node("Option2"), node("Option3")));
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(
                null, new DefaultParser().parse("Command1 Option1 ", "Command1 Option1 ".length()), candidates);
        assertEquals(2, candidates.size());
        assertEquals("Param1", candidates.get(0).value());
        assertEquals("Param2", candidates.get(1).value());
        assertEquals(1, words.size());
        assertEquals("", words.get(0));
    }
}
