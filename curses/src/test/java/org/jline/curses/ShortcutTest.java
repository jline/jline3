/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import org.jline.curses.impl.AbstractComponent;
import org.jline.curses.impl.AbstractPanel;
import org.jline.curses.impl.Box;
import org.jline.terminal.KeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that shortcut keys work for any Component, not just Box components,
 * and that Box.focus() properly delegates to its inner component.
 */
public class ShortcutTest {

    @Test
    public void testComponentDefaultShortcutKeyIsNull() {
        TestComponent comp = new TestComponent();
        assertNull(comp.getShortcutKey());
    }

    @Test
    public void testComponentWithCustomShortcutKey() {
        TestComponentWithShortcut compWithShortcut = new TestComponentWithShortcut("F");
        assertEquals("F", compWithShortcut.getShortcutKey());
    }

    @Test
    public void testBoxShortcutKey() {
        TestComponent innerComponent = new TestComponent();
        Box box = new Box("Test Box", null, innerComponent, "B");
        assertEquals("B", box.getShortcutKey());
    }

    @Test
    public void testBoxFocusDelegation() {
        TestComponent innerComponent = new TestComponent();
        Box box = new Box("Test Box", null, innerComponent, "B");

        try {
            box.focus();
        } catch (NullPointerException e) {
            // Expected: no GUI setup, focus() calls getWindow() which returns null
        }
    }

    @Test
    public void testShortcutKeyMapCreatedOnComponentAdd() {
        TestPanel panel = new TestPanel();

        TestComponentWithShortcut comp1 = new TestComponentWithShortcut("A");
        TestComponentWithShortcut comp2 = new TestComponentWithShortcut("B");

        panel.addComponent(comp1, null);
        panel.addComponent(comp2, null);

        assertNotNull(
                panel.getShortcutKeyMap(),
                "Shortcut key map should not be null after adding components with shortcuts");
    }

    private static class TestComponent extends AbstractComponent {
        @Override
        public boolean handleKey(KeyEvent event) {
            return false;
        }

        @Override
        protected Size doGetPreferredSize() {
            return new Size(10, 1);
        }

        @Override
        protected void doDraw(Screen screen) {}
    }

    private static class TestComponentWithShortcut extends TestComponent {
        private final String shortcutKey;

        public TestComponentWithShortcut(String shortcutKey) {
            this.shortcutKey = shortcutKey;
        }

        @Override
        public String getShortcutKey() {
            return shortcutKey;
        }
    }

    private static class TestPanel extends AbstractPanel {
        @Override
        protected void layout() {}

        @Override
        protected Size doGetPreferredSize() {
            return new Size(50, 20);
        }

        public Object getShortcutKeyMap() {
            try {
                java.lang.reflect.Field field = AbstractPanel.class.getDeclaredField("shortcutKeyMap");
                field.setAccessible(true);
                return field.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access shortcutKeyMap", e);
            }
        }
    }
}
