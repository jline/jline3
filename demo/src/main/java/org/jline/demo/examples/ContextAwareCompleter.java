/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Example demonstrating a context-aware completer in JLine.
 */
// SNIPPET_START: ContextAwareCompleter
public class ContextAwareCompleter implements Completer {
    private final Map<String, Completer> contextCompleters = new HashMap<>();

    public ContextAwareCompleter() {
        contextCompleters.put("default", new StringsCompleter("help", "context", "exit"));
        contextCompleters.put("file", new Completers.FilesCompleter(Paths.get("")));
        contextCompleters.put("user", new StringsCompleter("admin", "guest", "user1", "user2"));
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // Get current context from reader variables
        String context = (String) reader.getVariable("CONTEXT");
        if (context == null) {
            context = "default";
        }

        // Use the appropriate completer for this context
        Completer contextCompleter = contextCompleters.getOrDefault(context, contextCompleters.get("default"));
        contextCompleter.complete(reader, line, candidates);
    }
}
// SNIPPET_END: ContextAwareCompleter
