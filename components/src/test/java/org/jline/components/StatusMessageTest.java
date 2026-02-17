/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import java.util.List;

import org.jline.components.layout.Size;
import org.jline.components.ui.StatusMessage;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusMessageTest {

    @Test
    void testSuccessMessage() {
        StatusMessage msg = StatusMessage.success("Build passed");
        Canvas canvas = Canvas.create(30, 1);
        msg.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("\u2714")); // ✔
        assertTrue(line.contains("Build passed"));
    }

    @Test
    void testWarningMessage() {
        StatusMessage msg = StatusMessage.warning("Deprecated API");
        Canvas canvas = Canvas.create(30, 1);
        msg.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("\u26A0")); // ⚠
        assertTrue(line.contains("Deprecated API"));
    }

    @Test
    void testErrorMessage() {
        StatusMessage msg = StatusMessage.error("Test failed");
        Canvas canvas = Canvas.create(30, 1);
        msg.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("\u2716")); // ✖
        assertTrue(line.contains("Test failed"));
    }

    @Test
    void testInfoMessage() {
        StatusMessage msg = StatusMessage.info("3 tasks remaining");
        Canvas canvas = Canvas.create(30, 1);
        msg.render(canvas, 30, 1);
        String line = canvas.toLines().get(0).toString();
        assertTrue(line.contains("\u2139")); // ℹ
        assertTrue(line.contains("3 tasks remaining"));
    }

    @Test
    void testPreferredSizes() {
        StatusMessage msg = StatusMessage.success("OK");
        Size pref = msg.getPreferredSize();
        // icon (1 char) + space + "OK" = at least 4
        assertTrue(pref.width() >= 4);
        assertEquals(1, pref.height());
    }

    @Test
    void testAllTypesRender() {
        StatusMessage[] messages = {
            StatusMessage.success("a"), StatusMessage.warning("b"), StatusMessage.error("c"), StatusMessage.info("d"),
        };

        for (StatusMessage msg : messages) {
            Canvas canvas = Canvas.create(20, 1);
            msg.render(canvas, 20, 1);
            List<AttributedString> lines = canvas.toLines();
            assertFalse(lines.get(0).toString().isBlank());
        }
    }
}
