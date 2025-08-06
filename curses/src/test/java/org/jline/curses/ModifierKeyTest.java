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
 * Test class to verify that simple keys and modifier keys are handled correctly.
 */
public class ModifierKeyTest {

    public static void main(String[] args) {
        ModifierKeyTest test = new ModifierKeyTest();
        test.runTests();
    }

    public void runTests() {
        System.out.println("Running Modifier Key Tests...");

        try {
            testSimpleCharacterKeys();
            testControlKeys();
            testAltKeys();
            testModifiedArrowKeys();
            testModifiedFunctionKeys();
            testModifiedSpecialKeys();

            System.out.println("All tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testSimpleCharacterKeys() {
        System.out.println("Testing simple character keys...");

        // Test regular letters
        KeyEvent aKey = KeyParser.parse("a");
        if (aKey.getType() != KeyEvent.Type.Character
                || aKey.getCharacter() != 'a'
                || !aKey.getModifiers().isEmpty()) {
            throw new RuntimeException("Expected simple 'a' character, got: " + aKey);
        }

        // Test numbers
        KeyEvent oneKey = KeyParser.parse("1");
        if (oneKey.getType() != KeyEvent.Type.Character
                || oneKey.getCharacter() != '1'
                || !oneKey.getModifiers().isEmpty()) {
            throw new RuntimeException("Expected simple '1' character, got: " + oneKey);
        }

        // Test space
        KeyEvent spaceKey = KeyParser.parse(" ");
        if (spaceKey.getType() != KeyEvent.Type.Character
                || spaceKey.getCharacter() != ' '
                || !spaceKey.getModifiers().isEmpty()) {
            throw new RuntimeException("Expected simple space character, got: " + spaceKey);
        }

        System.out.println("✓ Simple character keys test passed");
    }

    private void testControlKeys() {
        System.out.println("Testing control keys...");

        // Test Ctrl+A (ASCII 1)
        KeyEvent ctrlA = KeyParser.parse("\u0001");
        if (ctrlA.getType() != KeyEvent.Type.Character
                || ctrlA.getCharacter() != 'a'
                || !ctrlA.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+A, got: " + ctrlA);
        }

        // Test Ctrl+C (ASCII 3)
        KeyEvent ctrlC = KeyParser.parse("\u0003");
        if (ctrlC.getType() != KeyEvent.Type.Character
                || ctrlC.getCharacter() != 'c'
                || !ctrlC.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+C, got: " + ctrlC);
        }

        // Test Ctrl+Z (ASCII 26)
        KeyEvent ctrlZ = KeyParser.parse("\u001a");
        if (ctrlZ.getType() != KeyEvent.Type.Character
                || ctrlZ.getCharacter() != 'z'
                || !ctrlZ.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+Z, got: " + ctrlZ);
        }

        System.out.println("✓ Control keys test passed");
    }

    private void testAltKeys() {
        System.out.println("Testing Alt keys...");

        // Test Alt+A
        KeyEvent altA = KeyParser.parse("\u001ba");
        if (altA.getType() != KeyEvent.Type.Character
                || altA.getCharacter() != 'a'
                || !altA.hasModifier(KeyEvent.Modifier.Alt)) {
            throw new RuntimeException("Expected Alt+A, got: " + altA);
        }

        // Test Alt+1
        KeyEvent alt1 = KeyParser.parse("\u001b1");
        if (alt1.getType() != KeyEvent.Type.Character
                || alt1.getCharacter() != '1'
                || !alt1.hasModifier(KeyEvent.Modifier.Alt)) {
            throw new RuntimeException("Expected Alt+1, got: " + alt1);
        }

        System.out.println("✓ Alt keys test passed");
    }

    private void testModifiedArrowKeys() {
        System.out.println("Testing modified arrow keys...");

        // Test Shift+Up Arrow
        KeyEvent shiftUp = KeyParser.parse("\u001b[1;2A");
        if (shiftUp.getType() != KeyEvent.Type.Arrow
                || shiftUp.getArrow() != KeyEvent.Arrow.Up
                || !shiftUp.hasModifier(KeyEvent.Modifier.Shift)) {
            throw new RuntimeException("Expected Shift+Up Arrow, got: " + shiftUp);
        }

        // Test Ctrl+Right Arrow
        KeyEvent ctrlRight = KeyParser.parse("\u001b[1;5C");
        if (ctrlRight.getType() != KeyEvent.Type.Arrow
                || ctrlRight.getArrow() != KeyEvent.Arrow.Right
                || !ctrlRight.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+Right Arrow, got: " + ctrlRight);
        }

        // Test Alt+Left Arrow
        KeyEvent altLeft = KeyParser.parse("\u001b[1;3D");
        if (altLeft.getType() != KeyEvent.Type.Arrow
                || altLeft.getArrow() != KeyEvent.Arrow.Left
                || !altLeft.hasModifier(KeyEvent.Modifier.Alt)) {
            throw new RuntimeException("Expected Alt+Left Arrow, got: " + altLeft);
        }

        // Test Shift+Alt+Down Arrow
        KeyEvent shiftAltDown = KeyParser.parse("\u001b[1;4B");
        if (shiftAltDown.getType() != KeyEvent.Type.Arrow
                || shiftAltDown.getArrow() != KeyEvent.Arrow.Down
                || !shiftAltDown.hasModifier(KeyEvent.Modifier.Shift)
                || !shiftAltDown.hasModifier(KeyEvent.Modifier.Alt)) {
            throw new RuntimeException("Expected Shift+Alt+Down Arrow, got: " + shiftAltDown);
        }

        System.out.println("✓ Modified arrow keys test passed");
    }

    private void testModifiedFunctionKeys() {
        System.out.println("Testing modified function keys...");

        // Test Ctrl+F1
        KeyEvent ctrlF1 = KeyParser.parse("\u001b[11;5~");
        if (ctrlF1.getType() != KeyEvent.Type.Function
                || ctrlF1.getFunctionKey() != 1
                || !ctrlF1.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+F1, got: " + ctrlF1);
        }

        // Test Shift+F12
        KeyEvent shiftF12 = KeyParser.parse("\u001b[24;2~");
        if (shiftF12.getType() != KeyEvent.Type.Function
                || shiftF12.getFunctionKey() != 12
                || !shiftF12.hasModifier(KeyEvent.Modifier.Shift)) {
            throw new RuntimeException("Expected Shift+F12, got: " + shiftF12);
        }

        System.out.println("✓ Modified function keys test passed");
    }

    private void testModifiedSpecialKeys() {
        System.out.println("Testing modified special keys...");

        // Test Ctrl+Delete
        KeyEvent ctrlDel = KeyParser.parse("\u001b[3;5~");
        if (ctrlDel.getType() != KeyEvent.Type.Special
                || ctrlDel.getSpecial() != KeyEvent.Special.Delete
                || !ctrlDel.hasModifier(KeyEvent.Modifier.Control)) {
            throw new RuntimeException("Expected Ctrl+Delete, got: " + ctrlDel);
        }

        // Test Shift+Insert
        KeyEvent shiftIns = KeyParser.parse("\u001b[2;2~");
        if (shiftIns.getType() != KeyEvent.Type.Special
                || shiftIns.getSpecial() != KeyEvent.Special.Insert
                || !shiftIns.hasModifier(KeyEvent.Modifier.Shift)) {
            throw new RuntimeException("Expected Shift+Insert, got: " + shiftIns);
        }

        System.out.println("✓ Modified special keys test passed");
    }
}
