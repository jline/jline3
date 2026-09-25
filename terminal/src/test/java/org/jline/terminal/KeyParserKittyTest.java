/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Kitty Keyboard Protocol sequence parsing in {@link KeyParser}.
 */
class KeyParserKittyTest {

    // Helper: prefix with ESC to build a full CSI sequence
    private static final String ESC = "\u001b";

    // ---- Basic CSI u parsing ----

    @Test
    void testSimpleCharacterKey() {
        // CSI 97 u -> 'a', no modifiers
        KeyEvent event = KeyParser.parse(ESC + "[97u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertTrue(event.getModifiers().isEmpty());
        assertEquals(KeyEvent.EventType.Press, event.getEventType());
        assertEquals(97, event.getKeyCode());
    }

    @Test
    void testCharacterWithShift() {
        // CSI 97;2u -> 'a' with Shift
        KeyEvent event = KeyParser.parse(ESC + "[97;2u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testCharacterWithCtrl() {
        // CSI 97;5u -> 'a' with Ctrl (kitty's replacement for C0 code 0x01)
        KeyEvent event = KeyParser.parse(ESC + "[97;5u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
    }

    @Test
    void testCharacterWithCtrlShift() {
        // CSI 97;6u -> 'a' with Ctrl+Shift
        KeyEvent event = KeyParser.parse(ESC + "[97;6u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift, KeyEvent.Modifier.Control), event.getModifiers());
    }

    @Test
    void testCharacterWithAlt() {
        // CSI 97;3u -> 'a' with Alt
        KeyEvent event = KeyParser.parse(ESC + "[97;3u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Alt), event.getModifiers());
    }

    @Test
    void testCharacterWithCtrlAlt() {
        // CSI 97;7u -> 'a' with Ctrl+Alt
        KeyEvent event = KeyParser.parse(ESC + "[97;7u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Alt, KeyEvent.Modifier.Control), event.getModifiers());
    }

    // ---- Special keys ----

    @Test
    void testEscapeKey() {
        // CSI 27 u -> Escape (kitty-encoded)
        KeyEvent event = KeyParser.parse(ESC + "[27u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Escape, event.getSpecial());
        assertTrue(event.getModifiers().isEmpty());
    }

    @Test
    void testEnterKey() {
        // CSI 13 u -> Enter (kitty-encoded)
        KeyEvent event = KeyParser.parse(ESC + "[13u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Enter, event.getSpecial());
    }

    @Test
    void testShiftEnter() {
        // CSI 13;2u -> Shift+Enter (the motivating use case!)
        KeyEvent event = KeyParser.parse(ESC + "[13;2u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Enter, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testCtrlEnter() {
        // CSI 13;5u -> Ctrl+Enter
        KeyEvent event = KeyParser.parse(ESC + "[13;5u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Enter, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
    }

    @Test
    void testTabKey() {
        // CSI 9 u -> Tab
        KeyEvent event = KeyParser.parse(ESC + "[9u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Tab, event.getSpecial());
    }

    @Test
    void testShiftTab() {
        // CSI 9;2u -> Shift+Tab
        KeyEvent event = KeyParser.parse(ESC + "[9;2u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Tab, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testBackspaceKey() {
        // CSI 127 u -> Backspace
        KeyEvent event = KeyParser.parse(ESC + "[127u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Backspace, event.getSpecial());
    }

    @Test
    void testCtrlBackspace() {
        // CSI 127;5u -> Ctrl+Backspace
        KeyEvent event = KeyParser.parse(ESC + "[127;5u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Backspace, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
    }

    // ---- Event types ----

    @Test
    void testPressEvent() {
        // CSI 97;1:1u -> 'a', press (explicit)
        KeyEvent event = KeyParser.parse(ESC + "[97;1:1u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(KeyEvent.EventType.Press, event.getEventType());
    }

    @Test
    void testRepeatEvent() {
        // CSI 97;1:2u -> 'a', repeat
        KeyEvent event = KeyParser.parse(ESC + "[97;1:2u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(KeyEvent.EventType.Repeat, event.getEventType());
    }

    @Test
    void testReleaseEvent() {
        // CSI 97;1:3u -> 'a', release
        KeyEvent event = KeyParser.parse(ESC + "[97;1:3u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(KeyEvent.EventType.Release, event.getEventType());
    }

    @Test
    void testReleaseWithModifiers() {
        // CSI 97;5:3u -> 'a', Ctrl, release
        KeyEvent event = KeyParser.parse(ESC + "[97;5:3u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
        assertEquals(KeyEvent.EventType.Release, event.getEventType());
    }

    // ---- Alternate key codes ----

    @Test
    void testShiftedKeyCode() {
        // CSI 97:65;2u -> 'a', shifted='A'(65), Shift
        KeyEvent event = KeyParser.parse(ESC + "[97:65;2u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(65, event.getShiftedKeyCode());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testBaseLayoutKeyCode() {
        // CSI 97:65:97;2u -> 'a', shifted='A'(65), base='a'(97), Shift
        KeyEvent event = KeyParser.parse(ESC + "[97:65:97;2u");
        assertEquals(97, event.getKeyCode());
        assertEquals(65, event.getShiftedKeyCode());
        assertEquals(97, event.getBaseLayoutKeyCode());
    }

    @Test
    void testBaseLayoutKeyCodeWithoutShifted() {
        // CSI 97::122;1u -> 'a', no shifted, base='z'(122)
        KeyEvent event = KeyParser.parse(ESC + "[97::122;1u");
        assertEquals(97, event.getKeyCode());
        assertEquals(0, event.getShiftedKeyCode());
        assertEquals(122, event.getBaseLayoutKeyCode());
    }

    // ---- Associated text ----

    @Test
    void testAssociatedText() {
        // CSI 97;2;65u -> 'a', Shift, text='A'
        KeyEvent event = KeyParser.parse(ESC + "[97;2;65u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals("A", event.getAssociatedText());
    }

    @Test
    void testAssociatedTextMultipleCodepoints() {
        // CSI 97;2;65:66u -> text='AB'
        KeyEvent event = KeyParser.parse(ESC + "[97;2;65:66u");
        assertEquals("AB", event.getAssociatedText());
    }

    // ---- Extended modifiers ----

    @Test
    void testSuperModifier() {
        // modifiers = 1 + super(8) = 9
        KeyEvent event = KeyParser.parse(ESC + "[97;9u");
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.Super));
    }

    @Test
    void testHyperModifier() {
        // modifiers = 1 + hyper(16) = 17
        KeyEvent event = KeyParser.parse(ESC + "[97;17u");
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.Hyper));
    }

    @Test
    void testMetaModifier() {
        // modifiers = 1 + meta(32) = 33
        KeyEvent event = KeyParser.parse(ESC + "[97;33u");
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.Meta));
    }

    @Test
    void testCapsLockModifier() {
        // modifiers = 1 + caps_lock(64) = 65
        KeyEvent event = KeyParser.parse(ESC + "[97;65u");
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.CapsLock));
    }

    @Test
    void testNumLockModifier() {
        // modifiers = 1 + num_lock(128) = 129
        KeyEvent event = KeyParser.parse(ESC + "[97;129u");
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.NumLock));
    }

    @Test
    void testMultipleModifiers() {
        // Ctrl+Shift+Alt = 1 + 1 + 2 + 4 = 8
        KeyEvent event = KeyParser.parse(ESC + "[97;8u");
        assertEquals(
                EnumSet.of(KeyEvent.Modifier.Shift, KeyEvent.Modifier.Alt, KeyEvent.Modifier.Control),
                event.getModifiers());
    }

    // ---- Extended function keys ----

    @Test
    void testF13Key() {
        // F13 = key code 57376
        KeyEvent event = KeyParser.parse(ESC + "[57376u");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(13, event.getFunctionKey());
    }

    @Test
    void testF35Key() {
        // F35 = key code 57398
        KeyEvent event = KeyParser.parse(ESC + "[57398u");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(35, event.getFunctionKey());
    }

    // ---- Navigation keys via PUA codes ----
    // These keys normally use legacy CSI ~ encoding, but the kitty protocol
    // defines PUA codepoints for them. We handle both paths.

    @Test
    void testInsertViaPua() {
        // Insert = PUA 57348
        KeyEvent event = KeyParser.parse(ESC + "[57348u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Insert, event.getSpecial());
        assertEquals(57348, event.getKeyCode());
    }

    @Test
    void testDeleteViaPua() {
        // Delete = PUA 57349
        KeyEvent event = KeyParser.parse(ESC + "[57349u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Delete, event.getSpecial());
    }

    @Test
    void testLeftViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57350u");
        assertEquals(KeyEvent.Type.Arrow, event.getType());
        assertEquals(KeyEvent.Arrow.Left, event.getArrow());
    }

    @Test
    void testRightViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57351u");
        assertEquals(KeyEvent.Type.Arrow, event.getType());
        assertEquals(KeyEvent.Arrow.Right, event.getArrow());
    }

    @Test
    void testUpViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57352u");
        assertEquals(KeyEvent.Type.Arrow, event.getType());
        assertEquals(KeyEvent.Arrow.Up, event.getArrow());
    }

    @Test
    void testDownViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57353u");
        assertEquals(KeyEvent.Type.Arrow, event.getType());
        assertEquals(KeyEvent.Arrow.Down, event.getArrow());
    }

    @Test
    void testHomeViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57356u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Home, event.getSpecial());
    }

    @Test
    void testEndViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57357u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.End, event.getSpecial());
    }

    @Test
    void testPageUpViaPua() {
        // PageUp = PUA 57354
        KeyEvent event = KeyParser.parse(ESC + "[57354u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.PageUp, event.getSpecial());
    }

    @Test
    void testPageDownViaPua() {
        // PageDown = PUA 57355
        KeyEvent event = KeyParser.parse(ESC + "[57355u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.PageDown, event.getSpecial());
    }

    @Test
    void testShiftInsertViaPua() {
        // Shift+Insert via PUA code
        KeyEvent event = KeyParser.parse(ESC + "[57348;2u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Insert, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    // ---- Private Use Area keys (keypad, media, etc.) ----

    @Test
    void testKeypadKey() {
        // KP_ENTER = 57414
        KeyEvent event = KeyParser.parse(ESC + "[57414u");
        assertEquals(KeyEvent.Type.Keypad, event.getType());
        assertEquals(KeyEvent.Keypad.Enter, event.getKeypad());
        assertEquals(57414, event.getKeyCode());
    }

    @Test
    void testModifierKeyEvent() {
        // LEFT_SHIFT = 57441
        KeyEvent event = KeyParser.parse(ESC + "[57441u");
        assertEquals(KeyEvent.Type.ModifierKey, event.getType());
        assertEquals(KeyEvent.ModKey.LeftShift, event.getModKey());
        assertEquals(57441, event.getKeyCode());
    }

    // ---- Full format with all fields ----

    @Test
    void testFullFormat() {
        // CSI 97:65:97;6:1;65u
        // key='a', shifted='A', base='a', Ctrl+Shift, press, text='A'
        KeyEvent event = KeyParser.parse(ESC + "[97:65:97;6:1;65u");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertEquals(97, event.getKeyCode());
        assertEquals(65, event.getShiftedKeyCode());
        assertEquals(97, event.getBaseLayoutKeyCode());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift, KeyEvent.Modifier.Control), event.getModifiers());
        assertEquals(KeyEvent.EventType.Press, event.getEventType());
        assertEquals("A", event.getAssociatedText());
    }

    // ---- Edge cases ----

    @Test
    void testKeyCodeZero() {
        // Key code 0 with text (no known key, just text)
        KeyEvent event = KeyParser.parse(ESC + "[0;;229u");
        assertNotNull(event);
        assertEquals(KeyEvent.Type.Unknown, event.getType());
        assertEquals(0, event.getKeyCode());
    }

    @Test
    void testNonBmpCharacterKey() {
        // Non-BMP Unicode characters (e.g. emoji U+1F600 = 128512) should be
        // classified as Character, not Unknown, even though their codepoint > U+E000.
        KeyEvent emoji = KeyParser.parse(ESC + "[128512u");
        assertEquals(KeyEvent.Type.Character, emoji.getType());
        assertEquals(128512, emoji.getKeyCode());
        // char is '\0' because the codepoint doesn't fit in a Java char
        assertEquals('\0', emoji.getCharacter());
    }

    @Test
    void testPuaKeyIsUnknown() {
        // A codepoint in the BMP Private Use Area that isn't a recognized
        // functional key should be classified as Unknown
        KeyEvent pua = KeyParser.parse(ESC + "[57500u");
        assertEquals(KeyEvent.Type.Unknown, pua.getType());
    }

    @Test
    void testLegacySequencesStillWork() {
        // Arrow keys should still parse as before
        KeyEvent up = KeyParser.parse(ESC + "[A");
        assertEquals(KeyEvent.Type.Arrow, up.getType());
        assertEquals(KeyEvent.Arrow.Up, up.getArrow());

        // Function keys should still parse
        KeyEvent f5 = KeyParser.parse(ESC + "[15~");
        assertEquals(KeyEvent.Type.Function, f5.getType());
        assertEquals(5, f5.getFunctionKey());

        // Regular characters
        KeyEvent a = KeyParser.parse("a");
        assertEquals(KeyEvent.Type.Character, a.getType());
        assertEquals('a', a.getCharacter());
    }

    // ---- DEL as Backspace ----

    @Test
    void testDelAsBackspace() {
        KeyEvent event = KeyParser.parse("\u007f");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Backspace, event.getSpecial());
    }

    // ---- Alt + control character combinations ----

    @Test
    void testAltTab() {
        KeyEvent event = KeyParser.parse(ESC + "\t");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Tab, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Alt), event.getModifiers());
    }

    @Test
    void testAltEnter() {
        KeyEvent event = KeyParser.parse(ESC + "\r");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Enter, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Alt), event.getModifiers());
    }

    @Test
    void testAltBackspace() {
        KeyEvent event = KeyParser.parse(ESC + "\u007f");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Backspace, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Alt), event.getModifiers());
    }

    @Test
    void testAltCtrlA() {
        KeyEvent event = KeyParser.parse(ESC + "\u0001");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('a', event.getCharacter());
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.Alt));
        assertTrue(event.getModifiers().contains(KeyEvent.Modifier.Control));
    }

    // ---- CSI P/Q/R/S for F1-F4 (kitty disambiguate) ----

    @Test
    void testF1ViaCsiP() {
        KeyEvent event = KeyParser.parse(ESC + "[P");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(1, event.getFunctionKey());
    }

    @Test
    void testF2ViaCsiQ() {
        KeyEvent event = KeyParser.parse(ESC + "[Q");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(2, event.getFunctionKey());
    }

    @Test
    void testF3ViaCsiR() {
        KeyEvent event = KeyParser.parse(ESC + "[R");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(3, event.getFunctionKey());
    }

    @Test
    void testF4ViaCsiS() {
        KeyEvent event = KeyParser.parse(ESC + "[S");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(4, event.getFunctionKey());
    }

    // ---- Modified keys with mod:event_type format ----

    @Test
    void testArrowWithEventType() {
        // CSI 1;1:1A -> Up arrow, no modifiers, press
        KeyEvent event = KeyParser.parse(ESC + "[1;1:1A");
        assertEquals(KeyEvent.Type.Arrow, event.getType());
        assertEquals(KeyEvent.Arrow.Up, event.getArrow());
        assertEquals(KeyEvent.EventType.Press, event.getEventType());
    }

    @Test
    void testArrowRelease() {
        // CSI 1;2:3D -> Left arrow, Shift, release
        KeyEvent event = KeyParser.parse(ESC + "[1;2:3D");
        assertEquals(KeyEvent.Type.Arrow, event.getType());
        assertEquals(KeyEvent.Arrow.Left, event.getArrow());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
        assertEquals(KeyEvent.EventType.Release, event.getEventType());
    }

    @Test
    void testShiftF1() {
        KeyEvent event = KeyParser.parse(ESC + "[1;2P");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(1, event.getFunctionKey());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testShiftHome() {
        KeyEvent event = KeyParser.parse(ESC + "[1;2H");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Home, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testCtrlEnd() {
        KeyEvent event = KeyParser.parse(ESC + "[1;5F");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.End, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
    }

    @Test
    void testShiftF5() {
        KeyEvent event = KeyParser.parse(ESC + "[15;2~");
        assertEquals(KeyEvent.Type.Function, event.getType());
        assertEquals(5, event.getFunctionKey());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testCtrlDelete() {
        KeyEvent event = KeyParser.parse(ESC + "[3;5~");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Delete, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
    }

    // ---- xterm modifyOtherKeys format ----

    @Test
    void testXtermShiftEnter() {
        KeyEvent event = KeyParser.parse(ESC + "[27;2;13~");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Enter, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testXtermShiftEscape() {
        KeyEvent event = KeyParser.parse(ESC + "[27;2;27~");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Escape, event.getSpecial());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Shift), event.getModifiers());
    }

    @Test
    void testXtermCtrlI() {
        KeyEvent event = KeyParser.parse(ESC + "[27;5;105~");
        assertEquals(KeyEvent.Type.Character, event.getType());
        assertEquals('i', event.getCharacter());
        assertEquals(EnumSet.of(KeyEvent.Modifier.Control), event.getModifiers());
    }

    // ---- Additional PUA special keys ----

    @Test
    void testCapsLockViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57358u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.CapsLock, event.getSpecial());
    }

    @Test
    void testNumLockViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57360u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.NumLock, event.getSpecial());
    }

    @Test
    void testPrintScreenViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57361u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.PrintScreen, event.getSpecial());
    }

    @Test
    void testMenuViaPua() {
        KeyEvent event = KeyParser.parse(ESC + "[57363u");
        assertEquals(KeyEvent.Type.Special, event.getType());
        assertEquals(KeyEvent.Special.Menu, event.getSpecial());
    }

    // ---- Keypad keys ----

    @Test
    void testKeypadDigit() {
        KeyEvent event = KeyParser.parse(ESC + "[57399u");
        assertEquals(KeyEvent.Type.Keypad, event.getType());
        assertEquals(KeyEvent.Keypad.KP0, event.getKeypad());
    }

    // ---- Media keys ----

    @Test
    void testMediaKey() {
        KeyEvent event = KeyParser.parse(ESC + "[57430u");
        assertEquals(KeyEvent.Type.Media, event.getType());
        assertEquals(KeyEvent.MediaKey.PlayPause, event.getMediaKey());
    }

    // ---- Modifier keys ----

    @Test
    void testRightShiftModifierKey() {
        KeyEvent event = KeyParser.parse(ESC + "[57447u");
        assertEquals(KeyEvent.Type.ModifierKey, event.getType());
        assertEquals(KeyEvent.ModKey.RightShift, event.getModKey());
    }
}
