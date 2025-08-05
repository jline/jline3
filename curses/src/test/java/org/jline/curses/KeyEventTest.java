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

/**
 * Test class to verify that KeyEvent parsing and handling works correctly.
 */
public class KeyEventTest {

    public static void main(String[] args) {
        KeyEventTest test = new KeyEventTest();
        test.runTests();
    }

    public void runTests() {
        System.out.println("Running KeyEvent Tests...");

        try {
            testCharacterKeys();
            testArrowKeys();
            testFunctionKeys();
            testSpecialKeys();
            testModifierKeys();
            testUnknownKeys();

            System.out.println("All tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testCharacterKeys() {
        System.out.println("Testing character keys...");

        // Test regular character
        KeyEvent event = KeyParser.parse("a");
        if (event.getType() != KeyEvent.Type.Character || event.getCharacter() != 'a') {
            throw new RuntimeException("Expected character 'a', got: " + event);
        }

        // Test space
        event = KeyParser.parse(" ");
        if (event.getType() != KeyEvent.Type.Character || event.getCharacter() != ' ') {
            throw new RuntimeException("Expected space character, got: " + event);
        }

        System.out.println("✓ Character keys test passed");
    }

    private void testArrowKeys() {
        System.out.println("Testing arrow keys...");

        // Test arrow keys
        KeyEvent up = KeyParser.parse("\u001b[A");
        if (up.getType() != KeyEvent.Type.Arrow || up.getArrow() != KeyEvent.Arrow.Up) {
            throw new RuntimeException("Expected Up arrow, got: " + up);
        }

        KeyEvent down = KeyParser.parse("\u001b[B");
        if (down.getType() != KeyEvent.Type.Arrow || down.getArrow() != KeyEvent.Arrow.Down) {
            throw new RuntimeException("Expected Down arrow, got: " + down);
        }

        KeyEvent right = KeyParser.parse("\u001b[C");
        if (right.getType() != KeyEvent.Type.Arrow || right.getArrow() != KeyEvent.Arrow.Right) {
            throw new RuntimeException("Expected Right arrow, got: " + right);
        }

        KeyEvent left = KeyParser.parse("\u001b[D");
        if (left.getType() != KeyEvent.Type.Arrow || left.getArrow() != KeyEvent.Arrow.Left) {
            throw new RuntimeException("Expected Left arrow, got: " + left);
        }

        System.out.println("✓ Arrow keys test passed");
    }

    private void testFunctionKeys() {
        System.out.println("Testing function keys...");

        // Test F1 key
        KeyEvent f1 = KeyParser.parse("\u001bOP");
        if (f1.getType() != KeyEvent.Type.Function || f1.getFunctionKey() != 1) {
            throw new RuntimeException("Expected F1, got: " + f1);
        }

        // Test F12 key
        KeyEvent f12 = KeyParser.parse("\u001b[24~");
        if (f12.getType() != KeyEvent.Type.Function || f12.getFunctionKey() != 12) {
            throw new RuntimeException("Expected F12, got: " + f12);
        }

        System.out.println("✓ Function keys test passed");
    }

    private void testSpecialKeys() {
        System.out.println("Testing special keys...");

        // Test Enter
        KeyEvent enter = KeyParser.parse("\r");
        if (enter.getType() != KeyEvent.Type.Special || enter.getSpecial() != KeyEvent.Special.Enter) {
            throw new RuntimeException("Expected Enter, got: " + enter);
        }

        // Test Tab
        KeyEvent tab = KeyParser.parse("\t");
        if (tab.getType() != KeyEvent.Type.Special || tab.getSpecial() != KeyEvent.Special.Tab) {
            throw new RuntimeException("Expected Tab, got: " + tab);
        }

        // Test Escape
        KeyEvent escape = KeyParser.parse("\u001b");
        if (escape.getType() != KeyEvent.Type.Special || escape.getSpecial() != KeyEvent.Special.Escape) {
            throw new RuntimeException("Expected Escape, got: " + escape);
        }

        // Test Home
        KeyEvent home = KeyParser.parse("\u001b[H");
        if (home.getType() != KeyEvent.Type.Special || home.getSpecial() != KeyEvent.Special.Home) {
            throw new RuntimeException("Expected Home, got: " + home);
        }

        System.out.println("✓ Special keys test passed");
    }

    private void testModifierKeys() {
        System.out.println("Testing modifier keys...");

        // Test Alt+a
        KeyEvent altA = KeyParser.parse("\u001ba");
        if (altA.getType() != KeyEvent.Type.Character
                || altA.getCharacter() != 'a'
                || !altA.hasModifier(KeyEvent.Modifier.Alt)) {
            throw new RuntimeException("Expected Alt+a, got: " + altA);
        }

        // Test Ctrl+a
        KeyEvent ctrlA = KeyParser.parse("\u0001");
        if (ctrlA.getType() != KeyEvent.Type.Character
                || ctrlA.getCharacter() != 'a'
                || !ctrlA.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+a, got: " + ctrlA);
        }

        System.out.println("✓ Modifier keys test passed");
    }

    private void testUnknownKeys() {
        System.out.println("Testing unknown keys...");

        // Test unknown sequence
        KeyEvent unknown = KeyParser.parse("\u001b[999~");
        if (unknown.getType() != KeyEvent.Type.Unknown) {
            throw new RuntimeException("Expected Unknown type, got: " + unknown);
        }

        System.out.println("✓ Unknown keys test passed");
    }
}
