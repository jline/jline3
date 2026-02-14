/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import org.jline.terminal.KeyEvent;
import org.jline.terminal.KeyParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that simple keys and modifier keys are handled correctly.
 */
public class ModifierKeyTest {

    @Test
    public void testSimpleLetterKey() {
        KeyEvent aKey = KeyParser.parse("a");
        assertEquals(KeyEvent.Type.Character, aKey.getType());
        assertEquals('a', aKey.getCharacter());
        assertTrue(aKey.getModifiers().isEmpty());
    }

    @Test
    public void testSimpleNumberKey() {
        KeyEvent oneKey = KeyParser.parse("1");
        assertEquals(KeyEvent.Type.Character, oneKey.getType());
        assertEquals('1', oneKey.getCharacter());
        assertTrue(oneKey.getModifiers().isEmpty());
    }

    @Test
    public void testSimpleSpaceKey() {
        KeyEvent spaceKey = KeyParser.parse(" ");
        assertEquals(KeyEvent.Type.Character, spaceKey.getType());
        assertEquals(' ', spaceKey.getCharacter());
        assertTrue(spaceKey.getModifiers().isEmpty());
    }

    @Test
    public void testCtrlA() {
        KeyEvent ctrlA = KeyParser.parse("\u0001");
        assertEquals(KeyEvent.Type.Character, ctrlA.getType());
        assertEquals('a', ctrlA.getCharacter());
        assertTrue(ctrlA.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testCtrlC() {
        KeyEvent ctrlC = KeyParser.parse("\u0003");
        assertEquals(KeyEvent.Type.Character, ctrlC.getType());
        assertEquals('c', ctrlC.getCharacter());
        assertTrue(ctrlC.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testCtrlZ() {
        KeyEvent ctrlZ = KeyParser.parse("\u001a");
        assertEquals(KeyEvent.Type.Character, ctrlZ.getType());
        assertEquals('z', ctrlZ.getCharacter());
        assertTrue(ctrlZ.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testAltA() {
        KeyEvent altA = KeyParser.parse("\u001ba");
        assertEquals(KeyEvent.Type.Character, altA.getType());
        assertEquals('a', altA.getCharacter());
        assertTrue(altA.hasModifier(KeyEvent.Modifier.Alt));
    }

    @Test
    public void testAltNumber() {
        KeyEvent alt1 = KeyParser.parse("\u001b1");
        assertEquals(KeyEvent.Type.Character, alt1.getType());
        assertEquals('1', alt1.getCharacter());
        assertTrue(alt1.hasModifier(KeyEvent.Modifier.Alt));
    }

    @Test
    public void testShiftUpArrow() {
        KeyEvent shiftUp = KeyParser.parse("\u001b[1;2A");
        assertEquals(KeyEvent.Type.Arrow, shiftUp.getType());
        assertEquals(KeyEvent.Arrow.Up, shiftUp.getArrow());
        assertTrue(shiftUp.hasModifier(KeyEvent.Modifier.Shift));
    }

    @Test
    public void testCtrlRightArrow() {
        KeyEvent ctrlRight = KeyParser.parse("\u001b[1;5C");
        assertEquals(KeyEvent.Type.Arrow, ctrlRight.getType());
        assertEquals(KeyEvent.Arrow.Right, ctrlRight.getArrow());
        assertTrue(ctrlRight.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testAltLeftArrow() {
        KeyEvent altLeft = KeyParser.parse("\u001b[1;3D");
        assertEquals(KeyEvent.Type.Arrow, altLeft.getType());
        assertEquals(KeyEvent.Arrow.Left, altLeft.getArrow());
        assertTrue(altLeft.hasModifier(KeyEvent.Modifier.Alt));
    }

    @Test
    public void testShiftAltDownArrow() {
        KeyEvent shiftAltDown = KeyParser.parse("\u001b[1;4B");
        assertEquals(KeyEvent.Type.Arrow, shiftAltDown.getType());
        assertEquals(KeyEvent.Arrow.Down, shiftAltDown.getArrow());
        assertTrue(shiftAltDown.hasModifier(KeyEvent.Modifier.Shift));
        assertTrue(shiftAltDown.hasModifier(KeyEvent.Modifier.Alt));
    }

    @Test
    public void testCtrlF1() {
        KeyEvent ctrlF1 = KeyParser.parse("\u001b[11;5~");
        assertEquals(KeyEvent.Type.Function, ctrlF1.getType());
        assertEquals(1, ctrlF1.getFunctionKey());
        assertTrue(ctrlF1.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testShiftF12() {
        KeyEvent shiftF12 = KeyParser.parse("\u001b[24;2~");
        assertEquals(KeyEvent.Type.Function, shiftF12.getType());
        assertEquals(12, shiftF12.getFunctionKey());
        assertTrue(shiftF12.hasModifier(KeyEvent.Modifier.Shift));
    }

    @Test
    public void testCtrlDelete() {
        KeyEvent ctrlDel = KeyParser.parse("\u001b[3;5~");
        assertEquals(KeyEvent.Type.Special, ctrlDel.getType());
        assertEquals(KeyEvent.Special.Delete, ctrlDel.getSpecial());
        assertTrue(ctrlDel.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testShiftInsert() {
        KeyEvent shiftIns = KeyParser.parse("\u001b[2;2~");
        assertEquals(KeyEvent.Type.Special, shiftIns.getType());
        assertEquals(KeyEvent.Special.Insert, shiftIns.getSpecial());
        assertTrue(shiftIns.hasModifier(KeyEvent.Modifier.Shift));
    }
}
