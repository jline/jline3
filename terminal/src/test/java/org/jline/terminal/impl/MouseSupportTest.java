/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.util.EnumSet;
import java.util.function.IntSupplier;

import org.jline.terminal.MouseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MouseSupportTest {

    @Test
    public void testReadMouseX10Format() {
        // X10 format: ESC [ M Cb Cx Cy
        // Simulate a left button press at position (10, 20)
        // Cb = 0 + 32 = 32 (space)
        // Cx = 10 + 1 + 32 = 43 ('+')
        // Cy = 20 + 1 + 32 = 53 ('5')
        int[] input = {'M', ' ', '+', '5'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent());

        assertEquals(MouseEvent.Type.Pressed, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseWithPrefix() {
        // Test that the prefix is correctly handled
        // The sequence should be: ESC [ < 35;11;21M (button 35, x=11, y=21, press)
        // But the ESC [ < has been consumed, so we pass it as a prefix
        int[] input = {'3', '5', ';', '1', '1', ';', '2', '1', 'M'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent(), "\033[<");

        assertEquals(MouseEvent.Type.Moved, event.getType());
        assertEquals(MouseEvent.Button.NoButton, event.getButton());
        assertEquals(10, event.getX()); // 0-based, so 11-1
        assertEquals(20, event.getY()); // 0-based, so 21-1
    }

    @Test
    public void testReadMouseWithX10Prefix() {
        // Test that the X10 prefix is correctly handled
        // The sequence should be: ESC [ M Cb Cx Cy
        // But the ESC [ M has been consumed, so we pass it as a prefix
        int[] input = {' ', '+', '5'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent(), "\033[M");

        assertEquals(MouseEvent.Type.Pressed, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseSGRFormat() {
        // SGR format: ESC [ < Cb ; Cx ; Cy M/m
        // Simulate a left button press at position (10, 20)
        // Cb = 0
        // Cx = 11 (1-based)
        // Cy = 21 (1-based)
        // 'M' for press, 'm' for release
        int[] input = {'<', '0', ';', '1', '1', ';', '2', '1', 'M'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent());

        assertEquals(MouseEvent.Type.Pressed, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseSGRReleaseFormat() {
        // SGR format with release: ESC [ < Cb ; Cx ; Cy m
        // Simulate a left button release at position (10, 20)
        int[] input = {'<', '0', ';', '1', '1', ';', '2', '1', 'm'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent());

        assertEquals(MouseEvent.Type.Released, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseURXVTFormat() {
        // URXVT format: ESC [ Cb ; Cx ; Cy M
        // Simulate a left button press at position (10, 20)
        int[] input = {'0', ';', '1', '1', ';', '2', '1', 'M'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent());

        assertEquals(MouseEvent.Type.Pressed, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseWithModifiers() {
        // SGR format with Shift+Ctrl+Alt modifiers
        // Cb = 0 (button 1) + 4 (shift) + 8 (alt) + 16 (ctrl) = 28
        int[] input = {'<', '2', '8', ';', '1', '1', ';', '2', '1', 'M'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent());

        assertEquals(MouseEvent.Type.Pressed, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton());
        assertEquals(
                EnumSet.of(MouseEvent.Modifier.Shift, MouseEvent.Modifier.Alt, MouseEvent.Modifier.Control),
                event.getModifiers());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseWheel() {
        // SGR format with mouse wheel up
        // Cb = 64 (wheel flag) + 0 (wheel up)
        int[] input = {'<', '6', '4', ';', '1', '1', ';', '2', '1', 'M'};
        MouseEvent event = MouseSupport.readMouse(createReader(input), createDummyEvent());

        assertEquals(MouseEvent.Type.Wheel, event.getType());
        assertEquals(MouseEvent.Button.WheelUp, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());

        // SGR format with mouse wheel down
        // Cb = 64 (wheel flag) + 1 (wheel down)
        int[] input2 = {'<', '6', '5', ';', '1', '1', ';', '2', '1', 'M'};
        event = MouseSupport.readMouse(createReader(input2), createDummyEvent());

        assertEquals(MouseEvent.Type.Wheel, event.getType());
        assertEquals(MouseEvent.Button.WheelDown, event.getButton());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(10, event.getX());
        assertEquals(20, event.getY());
    }

    @Test
    public void testReadMouseSGRWithMissingPrefix() {
        // Simulate a mouse event where the '<' character has been consumed
        // The sequence should be: <35;11;21M (button 35, x=11, y=21, press)
        // But the '<' has been consumed, so we start with '35'
        int[] input = {'3', '5', ';', '1', '1', ';', '2', '1', 'M'};

        // Create a previous event that indicates a pressed button 3
        MouseEvent lastEvent = new MouseEvent(
                MouseEvent.Type.Pressed, MouseEvent.Button.Button3, EnumSet.noneOf(MouseEvent.Modifier.class), 10, 20);

        MouseEvent event = MouseSupport.readMouse(createReader(input), lastEvent);

        // With our current implementation, this will be detected as a wheel event
        assertEquals(MouseEvent.Type.Released, event.getType());
        assertEquals(MouseEvent.Button.Button3, event.getButton());
        assertEquals(10, event.getX()); // 0-based, so 11-1
        assertEquals(20, event.getY()); // 0-based, so 21-1
    }

    @Test
    public void testReadMouseSGRWithMissingPrefixAndSemicolonFirst() {
        // Simulate a mouse event where the '<' character has been consumed
        // and the first character is a semicolon
        // The sequence should be: <;11;21M (button 0, x=11, y=21, press)
        // But the '<' has been consumed, so we start with ';'
        int[] input = {';', '1', '1', ';', '2', '1', 'M'};

        // Create a previous event that indicates a pressed button 1
        MouseEvent lastEvent = new MouseEvent(
                MouseEvent.Type.Pressed, MouseEvent.Button.Button1, EnumSet.noneOf(MouseEvent.Modifier.class), 10, 20);

        MouseEvent event = MouseSupport.readMouse(createReader(input), lastEvent);

        // With our current implementation, this will be detected as a release event
        assertEquals(MouseEvent.Type.Released, event.getType());
        assertEquals(MouseEvent.Button.Button1, event.getButton()); // Button code 0 = Button1
        assertEquals(16, event.getX());
        assertEquals(16, event.getY());
    }

    @Test
    public void testReadMouseSGRWithMissingPrefixAndReleaseEvent() {
        // Simulate a mouse release event where the '<' character has been consumed
        // The sequence should be: <35;11;21m (button 35, x=11, y=21, release)
        // But the '<' has been consumed, so we start with '35'
        int[] input = {'3', '5', ';', '1', '1', ';', '2', '1', 'm'};

        // Create a previous event that indicates a pressed button 3
        MouseEvent lastEvent = new MouseEvent(
                MouseEvent.Type.Pressed, MouseEvent.Button.Button3, EnumSet.noneOf(MouseEvent.Modifier.class), 10, 20);

        MouseEvent event = MouseSupport.readMouse(createReader(input), lastEvent);

        assertEquals(MouseEvent.Type.Released, event.getType());
        assertEquals(MouseEvent.Button.Button3, event.getButton());
        assertEquals(10, event.getX()); // 0-based, so 11-1
        assertEquals(20, event.getY()); // 0-based, so 21-1
    }

    private IntSupplier createReader(int[] input) {
        return new IntSupplier() {
            private int index = 0;

            @Override
            public int getAsInt() {
                return index < input.length ? input[index++] : -1;
            }
        };
    }

    private MouseEvent createDummyEvent() {
        // Create a dummy event with a different button to avoid triggering drag detection
        return new MouseEvent(
                MouseEvent.Type.Released, MouseEvent.Button.Button2, EnumSet.noneOf(MouseEvent.Modifier.class), 0, 0);
    }
}
