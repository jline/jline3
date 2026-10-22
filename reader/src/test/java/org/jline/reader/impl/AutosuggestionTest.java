/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.LineReader.SuggestionType;
import org.junit.jupiter.api.Test;

class AutosuggestionTest extends ReaderTestSupport {

    @Test
    void nullClearsTailTip() {
        reader.setAutosuggestion(SuggestionType.TAIL_TIP);
        reader.setTailTip("[argument]");
        reader.setTailTip(null);

        assertLine("command", new TestBuffer("command").enter());
    }
}
