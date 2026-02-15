/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.terminal.KeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the invalidation/repaint system.
 */
public class InvalidationTest {

    @Test
    public void testComponentStartsInvalid() {
        TestComponent comp = new TestComponent();
        assertTrue(comp.isInvalid(), "Components should start as invalid to ensure initial draw");
    }

    @Test
    public void testDrawClearsInvalid() {
        TestComponent comp = new TestComponent();
        comp.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        comp.draw(screen);

        assertFalse(comp.isInvalid(), "Component should be valid after draw");
    }

    @Test
    public void testInvalidateMarksInvalid() {
        TestComponent comp = new TestComponent();
        comp.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        comp.draw(screen);
        assertFalse(comp.isInvalid());

        comp.invalidate();
        assertTrue(comp.isInvalid());
    }

    @Test
    public void testInvalidationPropagatesToParent() {
        TestPanel panel = new TestPanel();
        TestComponent child = new TestComponent();
        panel.addComponent(child, null);

        // Draw to clear initial invalid state
        panel.setSize(new Size(20, 5));
        VirtualScreen screen = new VirtualScreen(20, 5);
        panel.draw(screen);

        assertFalse(panel.isInvalid());

        // Invalidate child should propagate to parent
        child.invalidate();
        assertTrue(child.isInvalid());
        assertTrue(panel.isInvalid(), "Parent should be invalidated when child is invalidated");
    }

    @Test
    public void testFocusChangeInvalidates() {
        TestComponent comp = new TestComponent();
        comp.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        comp.draw(screen);
        assertFalse(comp.isInvalid());

        // Simulating focus change
        comp.focused(true);
        assertTrue(comp.isInvalid(), "Component should be invalidated when focus changes");
    }

    @Test
    public void testFocusChangeCallsOnFocus() {
        TestComponent comp = new TestComponent();
        comp.focused(true);
        assertTrue(comp.isFocused());
        assertTrue(comp.focusCalled);
    }

    @Test
    public void testFocusChangeCallsOnUnfocus() {
        TestComponent comp = new TestComponent();
        comp.focused(true);
        comp.focused(false);
        assertFalse(comp.isFocused());
        assertTrue(comp.unfocusCalled);
    }

    @Test
    public void testSameFocusStateDoesNotInvalidate() {
        TestComponent comp = new TestComponent();
        comp.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        comp.draw(screen);
        comp.focused(true); // Set focused
        comp.draw(screen); // Clear invalid
        assertFalse(comp.isInvalid());

        comp.focused(true); // Same state
        assertFalse(comp.isInvalid(), "Setting the same focus state should not invalidate");
    }

    private static class TestComponent extends AbstractComponent {
        boolean focusCalled = false;
        boolean unfocusCalled = false;

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

        @Override
        public void onFocus() {
            focusCalled = true;
        }

        @Override
        public void onUnfocus() {
            unfocusCalled = true;
        }
    }

    private static class TestPanel extends AbstractPanel {
        @Override
        protected void layout() {}

        @Override
        protected Size doGetPreferredSize() {
            return new Size(20, 5);
        }
    }
}
