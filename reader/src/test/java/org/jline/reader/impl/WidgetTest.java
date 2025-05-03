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

import org.jline.keymap.KeyMap;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.junit.jupiter.api.Test;

public class WidgetTest extends ReaderTestSupport {

    @Test
    public void testCustomWidget() throws IOException {
        reader.getKeyMaps().get(reader.getKeyMap()).bind(new Reference("custom-widget"), "\n");
        reader.getWidgets().put("custom-widget", () -> {
            if (reader.getBuffer().cursor() == reader.getBuffer().length()) {
                reader.callWidget(LineReader.ACCEPT_LINE);
            } else {
                reader.getBuffer().write('\n');
            }
            return true;
        });

        TestBuffer b =
                new TestBuffer().append("foo bar").left(3).enter().right(3).enter();
        assertLine("foo \nbar", b, false);
    }

    @Test
    public void testCustomWidget2() throws IOException {
        reader.getKeyMaps().get(reader.getKeyMap()).bind(new Reference(LineReader.SELF_INSERT), "\n");
        reader.getKeyMaps().get(reader.getKeyMap()).bind(new Reference(LineReader.ACCEPT_LINE), KeyMap.alt('\n'));

        TestBuffer b =
                new TestBuffer().append("foo bar").left(3).enter().right(3).alt('\n');
        assertLine("foo \nbar", b, false);
    }
}
