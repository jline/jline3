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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that KeyMapBuilder creates proper KeyMaps with pre-parsed KeyEvents.
 */
public class KeyMapBuilderTest {

    @Test
    public void testMouseInputEventIsMouse() {
        InputEvent mouseEvent = InputEvent.MOUSE;
        assertTrue(mouseEvent.isMouse());
        assertFalse(mouseEvent.isKey());
        assertNull(mouseEvent.getKeyEvent());
    }

    @Test
    public void testKeyInputEventIsKey() {
        KeyEvent keyEvent = new KeyEvent('a', java.util.EnumSet.noneOf(KeyEvent.Modifier.class), "a");
        InputEvent keyInputEvent = new InputEvent(keyEvent);
        assertFalse(keyInputEvent.isMouse());
        assertTrue(keyInputEvent.isKey());
        assertSame(keyEvent, keyInputEvent.getKeyEvent());
    }

    @Test
    public void testParseUnmatchedCharacter() {
        InputEvent charEvent = KeyMapBuilder.parseUnmatchedInput("a");
        assertTrue(charEvent.isKey());
        KeyEvent keyEvent = charEvent.getKeyEvent();
        assertEquals(KeyEvent.Type.Character, keyEvent.getType());
        assertEquals('a', keyEvent.getCharacter());
    }

    @Test
    public void testParseUnmatchedArrowKey() {
        InputEvent arrowEvent = KeyMapBuilder.parseUnmatchedInput("\u001b[A");
        assertTrue(arrowEvent.isKey());
        KeyEvent keyEvent = arrowEvent.getKeyEvent();
        assertEquals(KeyEvent.Type.Arrow, keyEvent.getType());
        assertEquals(KeyEvent.Arrow.Up, keyEvent.getArrow());
    }
}
