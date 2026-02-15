/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.curses.Component;
import org.jline.curses.Curses;
import org.jline.curses.Position;
import org.jline.curses.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for new widget components.
 */
public class NewWidgetsTest {

    // --- Checkbox Tests ---

    @Test
    public void testCheckboxToggle() {
        Checkbox cb = new Checkbox("Test");
        assertFalse(cb.isChecked());
        cb.toggle();
        assertTrue(cb.isChecked());
        cb.toggle();
        assertFalse(cb.isChecked());
    }

    @Test
    public void testCheckboxSetChecked() {
        Checkbox cb = new Checkbox("Test");
        cb.setChecked(true);
        assertTrue(cb.isChecked());
        cb.setChecked(false);
        assertFalse(cb.isChecked());
    }

    @Test
    public void testCheckboxChangeListener() {
        Checkbox cb = new Checkbox("Test");
        AtomicInteger count = new AtomicInteger(0);
        cb.addChangeListener(count::incrementAndGet);

        cb.toggle();
        assertEquals(1, count.get());
        cb.toggle();
        assertEquals(2, count.get());

        // setChecked to same value shouldn't fire
        cb.setChecked(false);
        assertEquals(2, count.get());
    }

    @Test
    public void testCheckboxRendering() {
        Checkbox cb = new Checkbox("OK");
        cb.setPosition(new Position(0, 0));
        cb.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        cb.doDraw(screen);
        assertEquals('[', screen.getChar(0, 0));
        assertEquals(' ', screen.getChar(1, 0));
        assertEquals(']', screen.getChar(2, 0));

        cb.setChecked(true);
        screen = new VirtualScreen(10, 1);
        cb.doDraw(screen);
        assertEquals('[', screen.getChar(0, 0));
        assertEquals('x', screen.getChar(1, 0));
        assertEquals(']', screen.getChar(2, 0));
    }

    @Test
    public void testCheckboxPreferredSize() {
        Checkbox cb = new Checkbox("Hello");
        Size pref = cb.doGetPreferredSize();
        assertEquals(9, pref.w()); // "Hello" = 5 + 4
        assertEquals(1, pref.h());
    }

    // --- RadioButton / RadioGroup Tests ---

    @Test
    public void testRadioButtonSelection() {
        RadioButton rb = new RadioButton("Option");
        assertFalse(rb.isSelected());
        rb.setSelected(true);
        assertTrue(rb.isSelected());
    }

    @Test
    public void testRadioGroupMutualExclusion() {
        RadioButton rb1 = new RadioButton("A");
        RadioButton rb2 = new RadioButton("B");
        RadioButton rb3 = new RadioButton("C");

        RadioGroup group = new RadioGroup(rb1, rb2, rb3);

        rb1.setSelected(true);
        assertTrue(rb1.isSelected());
        assertFalse(rb2.isSelected());
        assertFalse(rb3.isSelected());

        rb2.setSelected(true);
        assertFalse(rb1.isSelected());
        assertTrue(rb2.isSelected());
        assertFalse(rb3.isSelected());

        assertEquals(rb2, group.getSelected());
        assertEquals(1, group.getSelectedIndex());
    }

    @Test
    public void testRadioButtonRendering() {
        RadioButton rb = new RadioButton("Yes");
        rb.setPosition(new Position(0, 0));
        rb.setSize(new Size(10, 1));

        VirtualScreen screen = new VirtualScreen(10, 1);
        rb.doDraw(screen);
        assertEquals('(', screen.getChar(0, 0));
        assertEquals(' ', screen.getChar(1, 0));
        assertEquals(')', screen.getChar(2, 0));

        rb.setSelected(true);
        screen = new VirtualScreen(10, 1);
        rb.doDraw(screen);
        assertEquals('(', screen.getChar(0, 0));
        assertEquals('o', screen.getChar(1, 0));
        assertEquals(')', screen.getChar(2, 0));
    }

    @Test
    public void testRadioGroupChangeListener() {
        RadioButton rb1 = new RadioButton("A");
        RadioButton rb2 = new RadioButton("B");
        new RadioGroup(rb1, rb2);

        AtomicBoolean changed = new AtomicBoolean(false);
        rb1.addChangeListener(() -> changed.set(true));

        rb1.setSelected(true);
        assertTrue(changed.get());
    }

    // --- ComboBox Tests ---

    @Test
    public void testComboBoxSelection() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setItems(Arrays.asList("Red", "Green", "Blue"));
        cb.setSelectedIndex(1);

        assertEquals(1, cb.getSelectedIndex());
        assertEquals("Green", cb.getSelectedItem());
    }

    @Test
    public void testComboBoxOpenClose() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setItems(Arrays.asList("A", "B", "C"));
        assertFalse(cb.isOpen());
    }

    @Test
    public void testComboBoxAddItem() {
        ComboBox<String> cb = new ComboBox<>();
        assertEquals(-1, cb.getSelectedIndex());

        cb.addItem("First");
        assertEquals(0, cb.getSelectedIndex());
        assertEquals("First", cb.getSelectedItem());
    }

    @Test
    public void testComboBoxChangeListener() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setItems(Arrays.asList("A", "B", "C"));
        cb.setSelectedIndex(0);

        AtomicBoolean changed = new AtomicBoolean(false);
        cb.addChangeListener(() -> changed.set(true));

        cb.setSelectedIndex(1);
        assertTrue(changed.get());
    }

    // --- ProgressBar Tests ---

    @Test
    public void testProgressBarValueClamping() {
        ProgressBar pb = new ProgressBar();
        pb.setValue(0.5);
        assertEquals(0.5, pb.getValue(), 0.001);

        pb.setValue(-0.5);
        assertEquals(0.0, pb.getValue(), 0.001);

        pb.setValue(1.5);
        assertEquals(1.0, pb.getValue(), 0.001);
    }

    @Test
    public void testProgressBarRendering() {
        ProgressBar pb = new ProgressBar();
        pb.setValue(0.5);
        pb.setPosition(new Position(0, 0));
        pb.setSize(new Size(20, 1));

        VirtualScreen screen = new VirtualScreen(20, 1);
        pb.doDraw(screen);
        // Just verify it doesn't throw and draws something
        // First char should be filled block
        assertEquals('\u2588', screen.getChar(0, 0));
    }

    @Test
    public void testProgressBarPreferredSize() {
        ProgressBar pb = new ProgressBar();
        Size pref = pb.doGetPreferredSize();
        assertEquals(20, pref.w());
        assertEquals(1, pref.h());
    }

    @Test
    public void testProgressBarNoFocusBehavior() {
        ProgressBar pb = new ProgressBar();
        assertTrue(pb.getBehaviors().contains(Component.Behavior.NoFocus));
    }

    // --- Separator Tests ---

    @Test
    public void testSeparatorHorizontalRendering() {
        Separator sep = new Separator(Separator.Orientation.HORIZONTAL);
        sep.setPosition(new Position(0, 0));
        sep.setSize(new Size(5, 1));

        VirtualScreen screen = new VirtualScreen(5, 1);
        sep.doDraw(screen);
        for (int i = 0; i < 5; i++) {
            assertEquals('\u2500', screen.getChar(i, 0));
        }
    }

    @Test
    public void testSeparatorVerticalRendering() {
        Separator sep = new Separator(Separator.Orientation.VERTICAL);
        sep.setPosition(new Position(0, 0));
        sep.setSize(new Size(1, 3));

        VirtualScreen screen = new VirtualScreen(1, 3);
        sep.doDraw(screen);
        for (int i = 0; i < 3; i++) {
            assertEquals('\u2502', screen.getChar(0, i));
        }
    }

    @Test
    public void testSeparatorNoFocusBehavior() {
        Separator sep = new Separator();
        assertTrue(sep.getBehaviors().contains(Component.Behavior.NoFocus));
    }

    @Test
    public void testSeparatorPreferredSize() {
        Separator sep = new Separator();
        Size pref = sep.doGetPreferredSize();
        assertEquals(1, pref.w());
        assertEquals(1, pref.h());
    }

    // --- GridPanel Tests ---

    @Test
    public void testGridPanelLayout() {
        GridPanel grid = new GridPanel();
        Label l1 = new Label("A");
        Label l2 = new Label("B");
        Label l3 = new Label("C");

        grid.addComponent(l1, Curses.cell(0, 0));
        grid.addComponent(l2, Curses.cell(0, 1));
        grid.addComponent(l3, Curses.cell(1, 0));

        grid.setPosition(new Position(0, 0));
        grid.setSize(new Size(20, 10));

        // After setSize, layout should have been called
        assertNotNull(l1.getPosition());
        assertNotNull(l1.getSize());
        assertNotNull(l2.getPosition());
        assertNotNull(l3.getPosition());

        // l1 should be at (0,0)
        assertEquals(0, l1.getPosition().x());
        assertEquals(0, l1.getPosition().y());

        // l2 should be to the right of l1
        assertTrue(l2.getPosition().x() > 0);
        assertEquals(0, l2.getPosition().y());

        // l3 should be below l1
        assertEquals(0, l3.getPosition().x());
        assertTrue(l3.getPosition().y() > 0);
    }

    @Test
    public void testGridPanelPreferredSize() {
        GridPanel grid = new GridPanel();
        Label l1 = new Label("AAAA");
        Label l2 = new Label("BB");

        grid.addComponent(l1, Curses.cell(0, 0));
        grid.addComponent(l2, Curses.cell(0, 1));

        Size pref = grid.doGetPreferredSize();
        // Should be sum of preferred widths
        assertEquals(l1.doGetPreferredSize().w() + l2.doGetPreferredSize().w(), pref.w());
        assertEquals(
                Math.max(l1.doGetPreferredSize().h(), l2.doGetPreferredSize().h()), pref.h());
    }

    @Test
    public void testGridPanelEmptyPreferredSize() {
        GridPanel grid = new GridPanel();
        Size pref = grid.doGetPreferredSize();
        assertEquals(0, pref.w());
        assertEquals(0, pref.h());
    }

    @Test
    public void testGridPanelSpanning() {
        GridPanel grid = new GridPanel();
        Label l1 = new Label("Wide");
        Label l2 = new Label("A");
        Label l3 = new Label("B");

        grid.addComponent(l1, Curses.cell(0, 0, 1, 2)); // spans 2 columns
        grid.addComponent(l2, Curses.cell(1, 0));
        grid.addComponent(l3, Curses.cell(1, 1));

        grid.setPosition(new Position(0, 0));
        grid.setSize(new Size(20, 10));

        // l1 should span both columns
        assertTrue(l1.getSize().w() > l2.getSize().w());
    }

    // --- Dialog Tests ---

    @Test
    public void testDialogCreation() {
        Label content = new Label("Hello");
        Dialog dialog = new Dialog("Test", content);

        assertEquals("Test", dialog.getTitle());
        assertTrue(dialog.getBehaviors().contains(Component.Behavior.Popup));
        assertTrue(dialog.getBehaviors().contains(Component.Behavior.Modal));
        assertTrue(dialog.getBehaviors().contains(Component.Behavior.CloseButton));
    }

    @Test
    public void testDialogDefaultBehaviors() {
        Dialog dialog = new Dialog();
        assertTrue(dialog.getBehaviors().contains(Component.Behavior.Popup));
        assertTrue(dialog.getBehaviors().contains(Component.Behavior.Modal));
    }
}
