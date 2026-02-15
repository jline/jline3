/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import org.jline.curses.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BorderPanelTest {

    @Test
    public void testBorderExactSize() {
        Container panel = Curses.border()
                .add(new TestComponent("t", new Size(16, 2)), Curses.Location.Top)
                .add(new TestComponent("c", new Size(6, 6)), Curses.Location.Center)
                .add(new TestComponent("b", new Size(8, 3)), Curses.Location.Bottom)
                .add(new TestComponent("l", new Size(2, 4)), Curses.Location.Left)
                .add(new TestComponent("r", new Size(3, 6)), Curses.Location.Right)
                .build();

        panel.setSize(new Size(32, 32));
        assertComponentSize(panel, "t", 0, 0, 32, 2);
        assertComponentSize(panel, "c", 2, 2, 27, 27);
        assertComponentSize(panel, "b", 0, 29, 32, 3);
        assertComponentSize(panel, "l", 0, 2, 2, 27);
        assertComponentSize(panel, "r", 29, 2, 3, 27);

        panel.setSize(new Size(16, 16));
        assertComponentSize(panel, "t", 0, 0, 16, 2);
        assertComponentSize(panel, "c", 2, 2, 11, 11);
        assertComponentSize(panel, "b", 0, 13, 16, 3);
        assertComponentSize(panel, "l", 0, 2, 2, 11);
        assertComponentSize(panel, "r", 13, 2, 3, 11);

        // With deficit, Center shrinks first to preserve edges (Top/Bottom, Left/Right)
        panel.setSize(new Size(10, 10));
        assertComponentSize(panel, "t", 0, 0, 10, 2);
        assertComponentSize(panel, "c", 2, 2, 5, 5);
        assertComponentSize(panel, "b", 0, 7, 10, 3);
        assertComponentSize(panel, "l", 0, 2, 2, 5);
        assertComponentSize(panel, "r", 7, 2, 3, 5);

        panel.setSize(new Size(8, 8));
        assertComponentSize(panel, "t", 0, 0, 8, 2);
        assertComponentSize(panel, "c", 2, 2, 3, 3);
        assertComponentSize(panel, "b", 0, 5, 8, 3);
        assertComponentSize(panel, "l", 0, 2, 2, 3);
        assertComponentSize(panel, "r", 5, 2, 3, 3);

        panel.setSize(new Size(7, 7));
        assertComponentSize(panel, "t", 0, 0, 7, 2);
        assertComponentSize(panel, "c", 2, 2, 2, 2);
        assertComponentSize(panel, "b", 0, 4, 7, 3);
        assertComponentSize(panel, "l", 0, 2, 2, 2);
        assertComponentSize(panel, "r", 4, 2, 3, 2);
    }

    private void assertComponentSize(Container panel, String name, int x, int y, int w, int h) {

        Component component = panel.getComponents().stream()
                .filter(c -> ((TestComponent) c).name.equals(name))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertEquals(x, component.getPosition().x(), "bad position: x");
        assertEquals(y, component.getPosition().y(), "bad position: y");
        assertEquals(w, component.getSize().w(), "bad size: w");
        assertEquals(h, component.getSize().h(), "bad size: h");
    }

    private static class TestComponent extends AbstractComponent {
        private final String name;
        private final Size preferred;

        public TestComponent(String name, Size preferred) {
            this.name = name;
            this.preferred = preferred;
        }

        @Override
        protected void doDraw(Screen screen) {}

        @Override
        protected Size doGetPreferredSize() {
            return preferred;
        }
    }
}
