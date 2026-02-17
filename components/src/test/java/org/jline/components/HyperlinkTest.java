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

import org.jline.components.animation.SpinnerFrames;
import org.jline.components.layout.Size;
import org.jline.components.ui.Hyperlink;
import org.jline.components.ui.Spinner;
import org.jline.components.ui.Text;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HyperlinkTest {

    @Test
    void testHyperlinkWithText() {
        Hyperlink link = Hyperlink.builder()
                .url("https://example.com")
                .text("Click here")
                .build();

        Size pref = link.getPreferredSize();
        assertEquals(10, pref.width());
        assertEquals(1, pref.height());

        Canvas canvas = Canvas.create(20, 1);
        link.render(canvas, 20, 1);
        List<AttributedString> lines = canvas.toLines();
        assertTrue(lines.get(0).toString().contains("Click here"));
    }

    @Test
    void testHyperlinkWithInnerComponent() {
        Text inner =
                Text.builder().text("Link Text").style(AttributedStyle.BOLD).build();
        Hyperlink link =
                Hyperlink.builder().url("https://example.com").inner(inner).build();

        Size pref = link.getPreferredSize();
        assertEquals(9, pref.width());
        assertEquals(1, pref.height());

        Canvas canvas = Canvas.create(20, 1);
        link.render(canvas, 20, 1);
        List<AttributedString> lines = canvas.toLines();
        assertTrue(lines.get(0).toString().contains("Link Text"));
    }

    @Test
    void testHyperlinkPreferredSizeNoContent() {
        Hyperlink link = Hyperlink.builder().url("https://example.com").build();
        Size pref = link.getPreferredSize();
        assertEquals(0, pref.width());
    }

    @Test
    void testGetChildrenWithText() {
        Hyperlink link =
                Hyperlink.builder().url("https://example.com").text("Link").build();
        assertTrue(link.getChildren().isEmpty());
    }

    @Test
    void testGetChildrenWithInner() {
        Text inner = Text.builder().text("Inner").build();
        Hyperlink link =
                Hyperlink.builder().url("https://example.com").inner(inner).build();
        assertEquals(1, link.getChildren().size());
        assertSame(inner, link.getChildren().get(0));
    }

    @Test
    void testIsDirtyPropagatesFromInner() {
        Text inner = Text.builder().text("A").build();
        Hyperlink link =
                Hyperlink.builder().url("https://example.com").inner(inner).build();

        // Render to clear dirty flags
        Canvas canvas = Canvas.create(20, 1);
        link.render(canvas, 20, 1);
        assertFalse(link.isDirty());

        // Invalidate inner should make hyperlink dirty
        inner.invalidate();
        assertTrue(link.isDirty());
    }

    @Test
    void testAnimatableInsideHyperlink() {
        Spinner spinner = Spinner.builder().frames(SpinnerFrames.DOTS).build();
        Hyperlink link =
                Hyperlink.builder().url("https://example.com").inner(spinner).build();

        // getChildren() should return the spinner so animation framework can discover it
        assertEquals(1, link.getChildren().size());
        assertTrue(link.getChildren().get(0) instanceof Spinner);
    }

    @Test
    void testHyperlinkZeroSize() {
        Hyperlink link =
                Hyperlink.builder().url("https://example.com").text("Click").build();

        Canvas canvas = Canvas.create(0, 0);
        link.render(canvas, 0, 0); // should not throw
    }
}
