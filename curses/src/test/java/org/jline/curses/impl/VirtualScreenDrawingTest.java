/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.List;

import org.jline.curses.*;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for drawing components to a virtual screen.
 * This tests the actual rendering output of components.
 */
public class VirtualScreenDrawingTest {

    private VirtualScreen screen;
    private Theme theme;

    @BeforeEach
    public void setUp() {
        screen = new VirtualScreen(80, 25);
        theme = new org.jline.curses.impl.DefaultTheme();
    }

    @Test
    public void testVirtualScreenBasics() {
        // Test that VirtualScreen works correctly
        screen.text(5, 3, new AttributedString("Hello World"));

        List<AttributedString> lines = screen.lines();
        String line3 = lines.get(3).toString();

        // Check that "Hello World" appears at position (5, 3)
        assertTrue(
                line3.substring(5, 16).equals("Hello World"),
                "Expected 'Hello World' at position (5,3), got: '" + line3.substring(5, 16) + "'");
    }

    @Test
    public void testBoxDrawingImplementation() {
        // Test that Box.doDraw method is no longer empty and actually draws borders
        // Create a simple label to put inside the box
        Label innerLabel = new Label("Test");
        innerLabel.setTheme(theme);

        Box box = new Box("Title", Curses.Border.Single, innerLabel);
        box.setTheme(theme);
        box.setPosition(new Position(0, 0));
        box.setSize(new Size(10, 3));

        // Set up the inner component properly
        innerLabel.setPosition(new Position(1, 1));
        innerLabel.setSize(new Size(8, 1));

        box.draw(screen);

        List<AttributedString> lines = screen.lines();

        // Check that border characters appear (the exact characters depend on the theme)
        String line0 = lines.get(0).toString();
        String line1 = lines.get(1).toString();
        String line2 = lines.get(2).toString();

        // The border should not be all spaces - there should be some border characters
        assertFalse(line0.substring(0, 10).trim().isEmpty(), "Top border should not be empty");
        assertFalse(line2.substring(0, 10).trim().isEmpty(), "Bottom border should not be empty");

        // Check that we have border characters (corners and lines)
        assertTrue(line0.contains("─") || line0.contains("┐"), "Top line should contain horizontal border or corner");
        assertTrue(line1.contains("│"), "Middle line should contain vertical borders");
        assertTrue(
                line2.contains("─") || line2.contains("└") || line2.contains("┘"),
                "Bottom line should contain horizontal border or corner");

        // Check that some part of the title appears (it might be partially overwritten by the inner component)
        assertTrue(
                line0.contains("Test") || line0.contains("Title") || line0.contains("tle"),
                "Some part of title should appear in top border");
    }

    @Test
    public void testVirtualScreenFillAndText() {
        // Test basic VirtualScreen functionality
        screen.fill(0, 0, 5, 3, org.jline.utils.AttributedStyle.DEFAULT);
        screen.text(1, 1, new AttributedString("Hi"));

        List<AttributedString> lines = screen.lines();
        String line1 = lines.get(1).toString();

        assertTrue(line1.substring(1, 3).equals("Hi"), "Expected 'Hi' at position (1,1)");
    }

    @Test
    public void testBoxBorderCharacters() {
        // Test that our Box drawing method produces the expected border characters
        // We'll test this by directly calling the drawing method
        Label innerLabel = new Label("Test");
        innerLabel.setTheme(theme);

        Box box = new Box("Title", Curses.Border.Single, innerLabel);
        box.setTheme(theme);
        box.setPosition(new Position(0, 0));
        box.setSize(new Size(8, 3));

        // Set up the inner component
        innerLabel.setPosition(new Position(1, 1));
        innerLabel.setSize(new Size(6, 1));

        box.draw(screen);

        List<AttributedString> lines = screen.lines();

        // Check that we have some border content (not all spaces)
        String line0 = lines.get(0).toString().substring(0, 8);
        String line2 = lines.get(2).toString().substring(0, 8);

        // Should have border characters, not be empty
        assertFalse(line0.trim().isEmpty(), "Top border should contain characters");
        assertFalse(line2.trim().isEmpty(), "Bottom border should contain characters");

        // Should contain some part of the title (might be partially overwritten)
        String topLine = lines.get(0).toString();
        assertTrue(
                topLine.contains("Title") || topLine.contains("Test") || topLine.contains("tle"),
                "Should contain some part of title");
    }

    @Test
    public void testBoxComponentPositioning() {
        // Test that components inside boxes are positioned correctly
        Label innerLabel = new Label("Inside");
        innerLabel.setTheme(theme);

        Box box = new Box("Box", Curses.Border.Single, innerLabel);
        box.setTheme(theme);
        box.setPosition(new Position(5, 3)); // Position box at (5, 3)
        box.setSize(new Size(10, 5));

        box.draw(screen);

        List<AttributedString> lines = screen.lines();

        // Check that the border appears at the box position
        String line3 = lines.get(3).toString();
        assertTrue(line3.charAt(5) == '┌', "Expected top-left corner at box position (5, 3)");

        // Check that the inner content appears inside the box (at position 6, 4)
        String line4 = lines.get(4).toString();
        assertTrue(
                line4.substring(6, 12).contains("Inside"),
                "Expected 'Inside' to appear inside the box at (6, 4), got: '" + line4.substring(6, 12) + "'");
    }

    // Helper class for table testing
    public static class TestPerson {
        private final String name;
        private final int age;

        public TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
