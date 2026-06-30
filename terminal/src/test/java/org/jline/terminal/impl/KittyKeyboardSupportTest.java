/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KittyKeyboardSupportTest {

    @Test
    void testPushFlags() {
        assertEquals("\033[>1u", KittyKeyboardSupport.pushFlags(1));
        assertEquals("\033[>3u", KittyKeyboardSupport.pushFlags(3));
        assertEquals("\033[>31u", KittyKeyboardSupport.pushFlags(31));
    }

    @Test
    void testPopFlags() {
        assertEquals("\033[<1u", KittyKeyboardSupport.popFlags());
        assertEquals("\033[<3u", KittyKeyboardSupport.popFlags(3));
    }

    @Test
    void testKeySequence() {
        // Simple key, no modifiers
        assertEquals("\033[97u", KittyKeyboardSupport.keySequence(97, 0));
        assertEquals("\033[97u", KittyKeyboardSupport.keySequence(97, 1));

        // Key with modifiers
        assertEquals("\033[97;5u", KittyKeyboardSupport.keySequence(97, 5));
        assertEquals("\033[13;2u", KittyKeyboardSupport.keySequence(13, 2));
    }

    @Test
    void testCtrlKey() {
        // Ctrl+A: keycode 97 ('a'), modifiers = 1 + ctrl(4) = 5
        assertEquals("\033[97;5u", KittyKeyboardSupport.ctrlKey('a'));
        assertEquals("\033[122;5u", KittyKeyboardSupport.ctrlKey('z'));
    }

    @Test
    void testAltKey() {
        // Alt+A: keycode 97 ('a'), modifiers = 1 + alt(2) = 3
        assertEquals("\033[97;3u", KittyKeyboardSupport.altKey('a'));
    }

    @Test
    void testCtrlAltKey() {
        // Ctrl+Alt+A: keycode 97, modifiers = 1 + ctrl(4) + alt(2) = 7
        assertEquals("\033[97;7u", KittyKeyboardSupport.ctrlAltKey('a'));
    }

    @Test
    void testModifiedKey() {
        // Shift+Enter
        assertEquals("\033[13;2u", KittyKeyboardSupport.modifiedKey(13, true, false, false));
        // Ctrl+Backspace
        assertEquals("\033[127;5u", KittyKeyboardSupport.modifiedKey(127, false, false, true));
        // No modifiers
        assertEquals("\033[97u", KittyKeyboardSupport.modifiedKey(97, false, false, false));
    }

    @Test
    void testIsFlagsResponse() {
        assertTrue(KittyKeyboardSupport.isFlagsResponse("\033[?0u"));
        assertTrue(KittyKeyboardSupport.isFlagsResponse("\033[?1u"));
        assertTrue(KittyKeyboardSupport.isFlagsResponse("\033[?31u"));
        assertFalse(KittyKeyboardSupport.isFlagsResponse("\033[97u")); // no '?'
        assertFalse(KittyKeyboardSupport.isFlagsResponse("\033[?u")); // too short
        assertFalse(KittyKeyboardSupport.isFlagsResponse(""));
    }

    @Test
    void testParseFlagsResponse() {
        assertEquals(0, KittyKeyboardSupport.parseFlagsResponse("\033[?0u"));
        assertEquals(1, KittyKeyboardSupport.parseFlagsResponse("\033[?1u"));
        assertEquals(31, KittyKeyboardSupport.parseFlagsResponse("\033[?31u"));
        assertEquals(-1, KittyKeyboardSupport.parseFlagsResponse("\033[97u"));
        assertEquals(-1, KittyKeyboardSupport.parseFlagsResponse("invalid"));
    }

    @Test
    void testFlagConstants() {
        assertEquals(1, KittyKeyboardSupport.FLAG_DISAMBIGUATE);
        assertEquals(2, KittyKeyboardSupport.FLAG_REPORT_EVENTS);
        assertEquals(4, KittyKeyboardSupport.FLAG_REPORT_ALTERNATES);
        assertEquals(8, KittyKeyboardSupport.FLAG_REPORT_ALL_KEYS);
        assertEquals(16, KittyKeyboardSupport.FLAG_REPORT_TEXT);
    }

    @Test
    void testModifierBits() {
        assertEquals(1, KittyKeyboardSupport.MOD_SHIFT);
        assertEquals(2, KittyKeyboardSupport.MOD_ALT);
        assertEquals(4, KittyKeyboardSupport.MOD_CTRL);
        assertEquals(8, KittyKeyboardSupport.MOD_SUPER);
        assertEquals(16, KittyKeyboardSupport.MOD_HYPER);
        assertEquals(32, KittyKeyboardSupport.MOD_META);
        assertEquals(64, KittyKeyboardSupport.MOD_CAPS_LOCK);
        assertEquals(128, KittyKeyboardSupport.MOD_NUM_LOCK);
    }
}
