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

        panel.setSize(new Size(10, 10));
        assertComponentSize(panel, "t", 0, 0, 10, 2);
        assertComponentSize(panel, "c", 2, 2, 6, 6);
        assertComponentSize(panel, "b", 0, 8, 10, 2);
        assertComponentSize(panel, "l", 0, 2, 2, 6);
        assertComponentSize(panel, "r", 8, 2, 2, 6);

        panel.setSize(new Size(8, 8));
        assertComponentSize(panel, "t", 0, 0, 8, 2);
        assertComponentSize(panel, "c", 2, 2, 6, 6);
        assertComponentSize(panel, "b", 0, 8, 8, 0);
        assertComponentSize(panel, "l", 0, 2, 2, 6);
        assertComponentSize(panel, "r", 8, 2, 0, 6);

        panel.setSize(new Size(7, 7));
        assertComponentSize(panel, "t", 0, 0, 7, 1);
        assertComponentSize(panel, "c", 1, 1, 6, 6);
        assertComponentSize(panel, "b", 0, 7, 7, 0);
        assertComponentSize(panel, "l", 0, 1, 1, 6);
        assertComponentSize(panel, "r", 7, 1, 0, 6);
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
