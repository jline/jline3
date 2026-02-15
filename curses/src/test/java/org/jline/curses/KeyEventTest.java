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
 * Test class to verify that KeyEvent parsing and handling works correctly.
 */
public class KeyEventTest {

    @Test
    public void testCharacterKey() {
        KeyEvent event = KeyParser.parse("a");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
    }

    @Test
    public void testSpaceCharacter() {
        KeyEvent event = KeyParser.parse(" ");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals(' ', event.getCharacter());
    }

    @Test
    public void testArrowUp() {
        KeyEvent up = KeyParser.parse("\u001b[A");
        assertEquals(KeyEvent.Type.Arrow, up.getType());
        assertEquals(KeyEvent.Arrow.Up, up.getArrow());
    }

    @Test
    public void testArrowDown() {
        KeyEvent down = KeyParser.parse("\u001b[B");
        assertEquals(KeyEvent.Type.Arrow, down.getType());
        assertEquals(KeyEvent.Arrow.Down, down.getArrow());
    }

    @Test
    public void testArrowRight() {
        KeyEvent right = KeyParser.parse("\u001b[C");
        assertEquals(KeyEvent.Type.Arrow, right.getType());
        assertEquals(KeyEvent.Arrow.Right, right.getArrow());
    }

    @Test
    public void testArrowLeft() {
        KeyEvent left = KeyParser.parse("\u001b[D");
        assertEquals(KeyEvent.Type.Arrow, left.getType());
        assertEquals(KeyEvent.Arrow.Left, left.getArrow());
    }

    @Test
    public void testFunctionKeyF1() {
        KeyEvent f1 = KeyParser.parse("\u001bOP");
        assertEquals(KeyEvent.Type.Function, f1.getType());
        assertEquals(1, f1.getFunctionKey());
    }

    @Test
    public void testFunctionKeyF12() {
        KeyEvent f12 = KeyParser.parse("\u001b[24~");
        assertEquals(KeyEvent.Type.Function, f12.getType());
        assertEquals(12, f12.getFunctionKey());
    }

    @Test
    public void testSpecialEnter() {
        KeyEvent enter = KeyParser.parse("\r");
        assertEquals(KeyEvent.Type.Special, enter.getType());
        assertEquals(KeyEvent.Special.Enter, enter.getSpecial());
    }

    @Test
    public void testSpecialTab() {
        KeyEvent tab = KeyParser.parse("\t");
        assertEquals(KeyEvent.Type.Special, tab.getType());
        assertEquals(KeyEvent.Special.Tab, tab.getSpecial());
    }

    @Test
    public void testSpecialEscape() {
        KeyEvent escape = KeyParser.parse("\u001b");
        assertEquals(KeyEvent.Type.Special, escape.getType());
        assertEquals(KeyEvent.Special.Escape, escape.getSpecial());
    }

    @Test
    public void testSpecialHome() {
        KeyEvent home = KeyParser.parse("\u001b[H");
        assertEquals(KeyEvent.Type.Special, home.getType());
        assertEquals(KeyEvent.Special.Home, home.getSpecial());
    }

    @Test
    public void testModifierAlt() {
        KeyEvent altA = KeyParser.parse("\u001ba");
        assertEquals(KeyEvent.Type.Character, altA.getType());
        assertEquals('a', altA.getCharacter());
        assertTrue(altA.hasModifier(KeyEvent.Modifier.Alt));
    }

    @Test
    public void testModifierControl() {
        KeyEvent ctrlA = KeyParser.parse("\u0001");
        assertEquals(KeyEvent.Type.Character, ctrlA.getType());
        assertEquals('a', ctrlA.getCharacter());
        assertTrue(ctrlA.hasModifier(KeyEvent.Modifier.Control));
    }

    @Test
    public void testUnknownSequence() {
        KeyEvent unknown = KeyParser.parse("\u001b[999~");
        assertEquals(KeyEvent.Type.Unknown, unknown.getType());
    }
}
