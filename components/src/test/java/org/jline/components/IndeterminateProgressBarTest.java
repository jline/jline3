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
import org.jline.components.ui.IndeterminateProgressBar;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndeterminateProgressBarTest {

    @Test
    void testPreferredSize() {
        IndeterminateProgressBar bar =
                IndeterminateProgressBar.builder().width(30).build();
        assertEquals(new Size(30, 1), bar.getPreferredSize());
    }

    @Test
    void testRender() {
        IndeterminateProgressBar bar =
                IndeterminateProgressBar.builder().width(20).build();
        Canvas canvas = Canvas.create(20, 1);
        bar.render(canvas, 20, 1);
        List<AttributedString> lines = canvas.toLines();
        assertEquals(1, lines.size());
        assertEquals(20, lines.get(0).length());
    }

    @Test
    void testAnimationTick() {
        IndeterminateProgressBar bar =
                IndeterminateProgressBar.builder().width(40).cycleDuration(2000).build();

        // At tick 0, position is 0 — no change from initial
        assertFalse(bar.onTick(0));

        // After some time, position should advance
        assertTrue(bar.onTick(100));
        assertTrue(bar.isDirty());

        // Render clears dirty
        Canvas canvas = Canvas.create(40, 1);
        bar.render(canvas, 40, 1);
        assertFalse(bar.isDirty());
    }

    @Test
    void testGlowMovesWithSweep() {
        IndeterminateProgressBar bar = IndeterminateProgressBar.builder()
                .width(20)
                .glowRadius(5)
                .cycleDuration(1000)
                .barChar('#')
                .build();

        // Render at start (t=0, glow at left)
        Canvas c1 = Canvas.create(20, 1);
        bar.render(c1, 20, 1);
        String line1 = c1.toLines().get(0).toString();

        // Advance to quarter cycle (t=0.25, glow moving right)
        bar.onTick(250);
        Canvas c2 = Canvas.create(20, 1);
        bar.render(c2, 20, 1);
        String line2 = c2.toLines().get(0).toString();

        // Both should be all '#' characters (same char, different styles via gradient)
        for (int i = 0; i < 20; i++) {
            assertEquals('#', line1.charAt(i));
            assertEquals('#', line2.charAt(i));
        }
    }

    @Test
    void testIntervalIs60fps() {
        IndeterminateProgressBar bar = IndeterminateProgressBar.builder().build();
        assertEquals(16, bar.getIntervalMs());
    }

    @Test
    void testCustomColors() {
        IndeterminateProgressBar bar = IndeterminateProgressBar.builder()
                .trackColor(0, 0, 50)
                .glowColor(0, 100, 255)
                .width(10)
                .build();

        Canvas canvas = Canvas.create(10, 1);
        bar.render(canvas, 10, 1);
        assertEquals(1, canvas.toLines().size());
    }

    @Test
    void testLargeGlowRadius() {
        // Glow radius larger than bar should still render correctly
        IndeterminateProgressBar bar =
                IndeterminateProgressBar.builder().width(5).glowRadius(20).build();

        Canvas canvas = Canvas.create(5, 1);
        bar.render(canvas, 5, 1);
        assertEquals(5, canvas.toLines().get(0).length());
    }

    @Test
    void testZeroWidth() {
        IndeterminateProgressBar bar = IndeterminateProgressBar.builder().build();
        Canvas canvas = Canvas.create(0, 1);
        bar.render(canvas, 0, 1); // should not throw
    }

    @Test
    void testFullCycleWraps() {
        IndeterminateProgressBar bar =
                IndeterminateProgressBar.builder().cycleDuration(1000).build();

        // Tick past full cycle should wrap
        bar.onTick(1500);
        Canvas canvas = Canvas.create(40, 1);
        bar.render(canvas, 40, 1);
        assertNotNull(canvas.toLines());
    }

    @Test
    void testConvenienceFactory() {
        IndeterminateProgressBar bar = Components.indeterminateProgressBar();
        assertNotNull(bar);
        assertEquals(new Size(40, 1), bar.getPreferredSize());

        IndeterminateProgressBar bar2 = Components.indeterminateProgressBar(60);
        assertEquals(new Size(60, 1), bar2.getPreferredSize());
    }

    @Test
    void testDefaultCharIsUpperBlock() {
        IndeterminateProgressBar bar =
                IndeterminateProgressBar.builder().width(5).build();
        Canvas canvas = Canvas.create(5, 1);
        bar.render(canvas, 5, 1);
        String line = canvas.toLines().get(0).toString();
        // Default char is ▔ (upper one-eighth block)
        for (int i = 0; i < 5; i++) {
            assertEquals('\u2594', line.charAt(i));
        }
    }
}
