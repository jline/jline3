/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.ArrayList;
import java.util.List;

import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.Test;

public class OptionCompleterTest extends ReaderTestSupport {

    @Test
    public void testOptions() throws Exception {
        List<Completer> argsCompleters = new ArrayList<>();
        List<OptDesc> options = new ArrayList<>();
        argsCompleters.add(new StringsCompleter("bar", "rab"));
        argsCompleters.add(new StringsCompleter("foo", "oof"));
        argsCompleters.add(NullCompleter.INSTANCE);
        options.add(new OptDesc("-s", "--sopt", new StringsCompleter("val", "lav")));
        options.add(new OptDesc(null, "--option", NullCompleter.INSTANCE));

        reader.setCompleter(new ArgumentCompleter(
                new StringsCompleter("command"), new OptionCompleter(argsCompleters, options, 1)));

        assertBuffer("command ", new TestBuffer("c").tab());
        assertBuffer("command -s", new TestBuffer("command -").tab());
        assertBuffer("command -s val ", new TestBuffer("command -s v").tab());
        assertBuffer("command -sval ", new TestBuffer("command -sv").tab());
        assertBuffer("command --sopt val ", new TestBuffer("command --sopt v").tab());
        assertBuffer("command --sopt=", new TestBuffer("command --sop").tab());
        assertBuffer("command --sopt=val ", new TestBuffer("command --sopt=v").tab());
        assertBuffer("command -sval ", new TestBuffer("command -sv").tab());
        assertBuffer("command -s val bar ", new TestBuffer("command -s val b").tab());
        assertBuffer("command -s val bar --option ", new TestBuffer("command -s val bar --o").tab());
        assertBuffer("command -s val bar --option foo ", new TestBuffer("command -s val bar --option f").tab());
    }
}
