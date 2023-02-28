/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import org.jline.builtins.Completers.TreeCompleter;
import org.jline.reader.LineReader;
import org.junit.Test;

import static org.jline.builtins.Completers.TreeCompleter.node;

public class TreeCompleterTest extends ReaderTestSupport {

    @Test
    public void testCaseInsensitive() throws Exception {
        reader.setCompleter(new TreeCompleter(node("ORA", node("ACTIVES"), node("LONGOPS", node("-ALL")))));
        reader.setOpt(LineReader.Option.CASE_INSENSITIVE);

        assertBuffer("ORA ACTIVES ", new TestBuffer("ORA AC").tab());
        assertBuffer("ora ACTIVES ", new TestBuffer("ora aC").tab());
        assertBuffer("ora ACTIVES ", new TestBuffer("ora ac").tab());

        assertBuffer("ORA LONGOPS ", new TestBuffer("ORA l").tab());
        assertBuffer("Ora LONGOPS -ALL ", new TestBuffer("Ora l").tab().tab());
    }
}
