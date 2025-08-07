/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl.completer;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * Null completer.
 *
 * @since 2.3
 */
public final class NullCompleter implements Completer {
    public static final NullCompleter INSTANCE = new NullCompleter();

    /**
     * Creates a new NullCompleter.
     */
    public NullCompleter() {
        // Default constructor
    }

    public void complete(LineReader reader, final ParsedLine line, final List<Candidate> candidates) {}
}
