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

/**
 * Utility class for parsing raw terminal input sequences into KeyEvent objects.
 */
public class KeyParser {

    private KeyParser() {}

    /**
     * Parses a raw input sequence into a KeyEvent.
     *
     * @param rawSequence the raw input sequence from the terminal
     * @return a KeyEvent representing the parsed input
     */
    public static KeyEvent parse(String rawSequence) {
        if (rawSequence == null || rawSequence.isEmpty()) {
            return new KeyEvent(rawSequence);
        }

        // Handle escape sequences
        if (rawSequence.startsWith("\u001b")) {
            return parseEscapeSequence(rawSequence);
        }

        // Handle control characters
        if (rawSequence.length() == 1) {
            char ch = rawSequence.charAt(0);

            // Control characters (0x00-0x1F)
            if (ch >= 0 && ch <= 31) {
                return parseControlCharacter(ch, rawSequence);
            }

            // Regular printable character
            if (ch >= 32 && ch <= 126) {
                return new KeyEvent(ch, EnumSet.noneOf(KeyEvent.Modifier.class), rawSequence);
            }

            // DEL (0x7F) = Backspace
            if (ch == 127) {
                return new KeyEvent(KeyEvent.Special.Backspace, EnumSet.noneOf(KeyEvent.Modifier.class), rawSequence);
            }

            // Extended ASCII or Unicode
            if (ch > 127) {
                return new KeyEvent(ch, EnumSet.noneOf(KeyEvent.Modifier.class), rawSequence);
            }
        }

        // Multi-character sequence that's not an escape sequence
        return new KeyEvent(rawSequence);
    }

    private static KeyEvent parseEscapeSequence(String sequence) {
        if (sequence.length() < 2) {
            return new KeyEvent(KeyEvent.Special.Escape, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
        }

        // Alt+character sequences (ESC followed by a character)
        if (sequence.length() == 2) {
            char ch = sequence.charAt(1);
            EnumSet<KeyEvent.Modifier> modifiers = EnumSet.of(KeyEvent.Modifier.Alt);

            // Alt + special keys (control characters)
            switch (ch) {
                case 9:
                    return new KeyEvent(KeyEvent.Special.Tab, modifiers, sequence);
                case 13:
                    return new KeyEvent(KeyEvent.Special.Enter, modifiers, sequence);
                case 127:
                    return new KeyEvent(KeyEvent.Special.Backspace, modifiers, sequence);
                default:
                    break;
            }

            // Alt + printable characters
            if (ch >= 32 && ch <= 126) {
                return new KeyEvent(ch, modifiers, sequence);
            }

            // Alt + Ctrl+letter (e.g. ESC Ctrl+A = 0x01)
            if (ch >= 1 && ch <= 26) {
                modifiers.add(KeyEvent.Modifier.Control);
                return new KeyEvent((char) (ch + 'a' - 1), modifiers, sequence);
            }
        }

        // ANSI escape sequences
        if (sequence.startsWith("\u001b[")) {
            return parseAnsiSequence(sequence);
        }

        // SS3 escape sequences (ESC O)
        if (sequence.startsWith("\u001bO")) {
            return parseSS3Sequence(sequence);
        }

        // Other escape sequences
        return new KeyEvent(sequence);
    }

    private static KeyEvent parseAnsiSequence(String sequence) {
        // Kitty keyboard protocol: CSI … u sequences
        if (sequence.endsWith("u")) {
            return parseKittySequence(sequence);
        }

        // Common ANSI sequences
        switch (sequence) {
            // Arrow keys
            case "\u001b[A":
                return new KeyEvent(KeyEvent.Arrow.Up, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[B":
                return new KeyEvent(KeyEvent.Arrow.Down, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[C":
                return new KeyEvent(KeyEvent.Arrow.Right, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[D":
                return new KeyEvent(KeyEvent.Arrow.Left, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);

            // Function keys
            case "\u001b[11~":
            case "\u001bOP":
            case "\u001b[P":
                return new KeyEvent(1, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[12~":
            case "\u001bOQ":
            case "\u001b[Q":
                return new KeyEvent(2, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[13~":
            case "\u001bOR":
            case "\u001b[R":
                return new KeyEvent(3, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[14~":
            case "\u001bOS":
            case "\u001b[S":
                return new KeyEvent(4, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[15~":
                return new KeyEvent(5, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[17~":
                return new KeyEvent(6, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[18~":
                return new KeyEvent(7, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[19~":
                return new KeyEvent(8, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[20~":
                return new KeyEvent(9, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[21~":
                return new KeyEvent(10, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[23~":
                return new KeyEvent(11, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[24~":
                return new KeyEvent(12, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);

            // Special keys
            case "\u001b[H":
                return new KeyEvent(KeyEvent.Special.Home, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[F":
                return new KeyEvent(KeyEvent.Special.End, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[2~":
                return new KeyEvent(KeyEvent.Special.Insert, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[3~":
                return new KeyEvent(KeyEvent.Special.Delete, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[5~":
                return new KeyEvent(KeyEvent.Special.PageUp, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[6~":
                return new KeyEvent(KeyEvent.Special.PageDown, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);

            // Backtab (Shift+Tab)
            case "\u001b[Z":
                return new KeyEvent(KeyEvent.Special.Tab, EnumSet.of(KeyEvent.Modifier.Shift), sequence);

            default:
                // Try to parse modified keys (with Shift, Alt, Ctrl)
                return parseModifiedAnsiSequence(sequence);
        }
    }

    private static KeyEvent parseSS3Sequence(String sequence) {
        // SS3 sequences (ESC O)
        switch (sequence) {
            // Function keys
            case "\u001bOP":
                return new KeyEvent(1, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001bOQ":
                return new KeyEvent(2, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001bOR":
                return new KeyEvent(3, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001bOS":
                return new KeyEvent(4, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            default:
                return new KeyEvent(sequence);
        }
    }

    private static KeyEvent parseModifiedAnsiSequence(String sequence) {
        // Extract CSI parameters (between ESC[ and final byte) and the final byte
        char finalChar = sequence.charAt(sequence.length() - 1);
        String params = sequence.substring(2, sequence.length() - 1);
        String[] parts = params.split(";");

        if (finalChar != '~') {
            // Modified arrow, function, or special keys with optional event type
            if (parts.length == 2) {
                try {
                    ModifierEvent me = parseModifierEvent(parts[1]);

                    KeyEvent.Arrow arrow = parseArrowChar(finalChar);
                    if (arrow != null) {
                        return new KeyEvent(
                                KeyEvent.Type.Arrow,
                                '\0',
                                arrow,
                                null,
                                0,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    }

                    int fkey = mapSS3FunctionKey(finalChar);
                    if (fkey > 0) {
                        return new KeyEvent(
                                KeyEvent.Type.Function,
                                '\0',
                                null,
                                null,
                                fkey,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    }

                    KeyEvent.Special special = mapCSISpecialChar(finalChar);
                    if (special != null) {
                        return new KeyEvent(
                                KeyEvent.Type.Special,
                                '\0',
                                null,
                                special,
                                0,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    }
                } catch (NumberFormatException e) {
                    // Fall through to unknown
                }
            }
        } else if (parts.length == 3 && "27".equals(parts[0])) {
            // xterm modifyOtherKeys format: \E[27;{mod};{code}~
            try {
                ModifierEvent me = parseModifierEvent(parts[1]);
                int keyCode = Integer.parseInt(parts[2]);

                switch (keyCode) {
                    case 9:
                        return new KeyEvent(
                                KeyEvent.Type.Special,
                                '\0',
                                null,
                                KeyEvent.Special.Tab,
                                0,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    case 13:
                        return new KeyEvent(
                                KeyEvent.Type.Special,
                                '\0',
                                null,
                                KeyEvent.Special.Enter,
                                0,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    case 27:
                        return new KeyEvent(
                                KeyEvent.Type.Special,
                                '\0',
                                null,
                                KeyEvent.Special.Escape,
                                0,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    case 127:
                        return new KeyEvent(
                                KeyEvent.Type.Special,
                                '\0',
                                null,
                                KeyEvent.Special.Backspace,
                                0,
                                me.modifiers,
                                sequence,
                                me.eventType,
                                0,
                                0,
                                0,
                                null);
                    default:
                        if (keyCode >= 32 && keyCode <= 126) {
                            return new KeyEvent(
                                    KeyEvent.Type.Character,
                                    (char) keyCode,
                                    null,
                                    null,
                                    0,
                                    me.modifiers,
                                    sequence,
                                    me.eventType,
                                    0,
                                    0,
                                    0,
                                    null);
                        }
                }
            } catch (NumberFormatException e) {
                // Fall through to unknown
            }
        } else if (parts.length == 2) {
            // Modified function/special keys: \E[{code};{mod}[:{event}]~
            try {
                int code = Integer.parseInt(parts[0]);
                ModifierEvent me = parseModifierEvent(parts[1]);

                int functionKey = mapFunctionKeyNumber(code);
                if (functionKey > 0) {
                    return new KeyEvent(
                            KeyEvent.Type.Function,
                            '\0',
                            null,
                            null,
                            functionKey,
                            me.modifiers,
                            sequence,
                            me.eventType,
                            0,
                            0,
                            0,
                            null);
                }

                KeyEvent.Special special = mapSpecialKeyCode(code);
                if (special != null) {
                    return new KeyEvent(
                            KeyEvent.Type.Special,
                            '\0',
                            null,
                            special,
                            0,
                            me.modifiers,
                            sequence,
                            me.eventType,
                            0,
                            0,
                            0,
                            null);
                }
            } catch (NumberFormatException e) {
                // Fall through to unknown
            }
        }

        return new KeyEvent(sequence);
    }

    private static class ModifierEvent {
        final EnumSet<KeyEvent.Modifier> modifiers;
        final KeyEvent.EventType eventType;

        ModifierEvent(EnumSet<KeyEvent.Modifier> modifiers, KeyEvent.EventType eventType) {
            this.modifiers = modifiers;
            this.eventType = eventType;
        }
    }

    private static ModifierEvent parseModifierEvent(String modParam) {
        int colonIdx = modParam.indexOf(':');
        int modCode;
        KeyEvent.EventType eventType = KeyEvent.EventType.Press;
        if (colonIdx >= 0) {
            modCode = Integer.parseInt(modParam.substring(0, colonIdx));
            int event = Integer.parseInt(modParam.substring(colonIdx + 1));
            switch (event) {
                case 2:
                    eventType = KeyEvent.EventType.Repeat;
                    break;
                case 3:
                    eventType = KeyEvent.EventType.Release;
                    break;
                default:
                    break;
            }
        } else {
            modCode = Integer.parseInt(modParam);
        }
        return new ModifierEvent(parseModifierCode(modCode), eventType);
    }

    private static EnumSet<KeyEvent.Modifier> parseModifierCode(int modCode) {
        return parseKittyModifiers(modCode);
    }

    private static int mapSS3FunctionKey(char ch) {
        switch (ch) {
            case 'P':
                return 1;
            case 'Q':
                return 2;
            case 'R':
                return 3;
            case 'S':
                return 4;
            default:
                return 0;
        }
    }

    private static KeyEvent.Special mapCSISpecialChar(char ch) {
        switch (ch) {
            case 'H':
                return KeyEvent.Special.Home;
            case 'F':
                return KeyEvent.Special.End;
            default:
                return null;
        }
    }

    private static KeyEvent.Arrow parseArrowChar(char arrowChar) {
        switch (arrowChar) {
            case 'A':
                return KeyEvent.Arrow.Up;
            case 'B':
                return KeyEvent.Arrow.Down;
            case 'C':
                return KeyEvent.Arrow.Right;
            case 'D':
                return KeyEvent.Arrow.Left;
            default:
                return null;
        }
    }

    private static int mapFunctionKeyNumber(int fnNum) {
        // Map ANSI function key numbers to F1-F12
        switch (fnNum) {
            case 11:
                return 1; // F1
            case 12:
                return 2; // F2
            case 13:
                return 3; // F3
            case 14:
                return 4; // F4
            case 15:
                return 5; // F5
            case 17:
                return 6; // F6
            case 18:
                return 7; // F7
            case 19:
                return 8; // F8
            case 20:
                return 9; // F9
            case 21:
                return 10; // F10
            case 23:
                return 11; // F11
            case 24:
                return 12; // F12
            default:
                return 0;
        }
    }

    private static KeyEvent.Special mapSpecialKeyCode(int specialCode) {
        switch (specialCode) {
            case 2:
                return KeyEvent.Special.Insert;
            case 3:
                return KeyEvent.Special.Delete;
            case 5:
                return KeyEvent.Special.PageUp;
            case 6:
                return KeyEvent.Special.PageDown;
            default:
                return null;
        }
    }

    private static KeyEvent parseControlCharacter(char ch, String sequence) {
        switch (ch) {
            case '\t':
                return new KeyEvent(KeyEvent.Special.Tab, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case '\r':
            case '\n':
                return new KeyEvent(KeyEvent.Special.Enter, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case '\u001b':
                return new KeyEvent(KeyEvent.Special.Escape, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case '\b':
            case '\u007f':
                return new KeyEvent(KeyEvent.Special.Backspace, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            default:
                // Other control characters - could be Ctrl+letter combinations
                if (ch >= 1 && ch <= 26) {
                    // Ctrl+A through Ctrl+Z
                    char letter = (char) ('a' + ch - 1);
                    return new KeyEvent(letter, EnumSet.of(KeyEvent.Modifier.Control), sequence);
                }
                return new KeyEvent(sequence);
        }
    }

    // ---- Kitty Keyboard Protocol parsing ----

    /**
     * Parses a Kitty Keyboard Protocol {@code CSI … u} sequence.
     *
     * <p>Format: {@code CSI keycode:shifted:base ; modifiers:eventtype ; text u}</p>
     *
     * <p>All fields except keycode are optional. Sub-fields use colon separators;
     * main fields use semicolon separators.</p>
     */
    private static KeyEvent parseKittySequence(String sequence) {
        // Strip CSI prefix and 'u' suffix
        String body = sequence.substring(2, sequence.length() - 1);

        // Split into main fields by semicolon
        String[] fields = body.split(";", -1);

        // Field 1: keycode[:shifted[:base]]
        int[] keyCodes = parseKittyKeyCodes(fields.length >= 1 ? fields[0] : "");

        // Field 2: modifiers[:eventtype]
        int[] modAndEvent = parseKittyModAndEvent(fields.length >= 2 ? fields[1] : "");

        // Field 3: text-as-codepoints (colon-separated)
        String associatedText = (fields.length >= 3 && !fields[2].isEmpty()) ? parseTextCodepoints(fields[2]) : null;

        EnumSet<KeyEvent.Modifier> modifiers = parseKittyModifiers(modAndEvent[0]);
        KeyEvent.EventType eventType = parseKittyEventType(modAndEvent[1]);

        return buildKittyKeyEvent(
                keyCodes[0], modifiers, eventType, keyCodes[1], keyCodes[2], associatedText, sequence);
    }

    /**
     * Parses the key code field {@code keycode[:shifted[:base]]} into an array
     * of three ints: [keyCode, shiftedKeyCode, baseLayoutKeyCode].
     */
    private static int[] parseKittyKeyCodes(String field) {
        int keyCode = 0;
        int shiftedKeyCode = 0;
        int baseLayoutKeyCode = 0;
        if (!field.isEmpty()) {
            String[] parts = field.split(":", -1);
            keyCode = parseIntSafe(parts[0]);
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                shiftedKeyCode = parseIntSafe(parts[1]);
            }
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                baseLayoutKeyCode = parseIntSafe(parts[2]);
            }
        }
        return new int[] {keyCode, shiftedKeyCode, baseLayoutKeyCode};
    }

    /**
     * Parses the modifier field {@code modifiers[:eventtype]} into an array
     * of two ints: [modValue, eventTypeValue].
     */
    private static int[] parseKittyModAndEvent(String field) {
        int modValue = 1; // default: no modifiers
        int eventTypeValue = 1; // default: press
        if (!field.isEmpty()) {
            String[] parts = field.split(":", -1);
            modValue = parseIntSafe(parts[0]);
            if (modValue == 0) modValue = 1;
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                eventTypeValue = parseIntSafe(parts[1]);
            }
        }
        return new int[] {modValue, eventTypeValue};
    }

    /**
     * Parses the kitty modifier value (1 + bitmask) into a set of modifiers.
     */
    static EnumSet<KeyEvent.Modifier> parseKittyModifiers(int modValue) {
        EnumSet<KeyEvent.Modifier> modifiers = EnumSet.noneOf(KeyEvent.Modifier.class);
        int bits = modValue - 1;
        if ((bits & 1) != 0) modifiers.add(KeyEvent.Modifier.Shift);
        if ((bits & 2) != 0) modifiers.add(KeyEvent.Modifier.Alt);
        if ((bits & 4) != 0) modifiers.add(KeyEvent.Modifier.Control);
        if ((bits & 8) != 0) modifiers.add(KeyEvent.Modifier.Super);
        if ((bits & 16) != 0) modifiers.add(KeyEvent.Modifier.Hyper);
        if ((bits & 32) != 0) modifiers.add(KeyEvent.Modifier.Meta);
        if ((bits & 64) != 0) modifiers.add(KeyEvent.Modifier.CapsLock);
        if ((bits & 128) != 0) modifiers.add(KeyEvent.Modifier.NumLock);
        return modifiers;
    }

    private static KeyEvent.EventType parseKittyEventType(int value) {
        switch (value) {
            case 2:
                return KeyEvent.EventType.Repeat;
            case 3:
                return KeyEvent.EventType.Release;
            default:
                return KeyEvent.EventType.Press;
        }
    }

    private static String parseTextCodepoints(String field) {
        String[] parts = field.split(":");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            int cp = parseIntSafe(part);
            if (cp > 0 && Character.isValidCodePoint(cp)) {
                sb.appendCodePoint(cp);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Builds a KeyEvent from a parsed kitty key code, mapping it to the
     * appropriate KeyEvent type (Character, Arrow, Special, Function, or Unknown).
     */
    private static KeyEvent buildKittyKeyEvent(
            int keyCode,
            EnumSet<KeyEvent.Modifier> modifiers,
            KeyEvent.EventType eventType,
            int shiftedKeyCode,
            int baseLayoutKeyCode,
            String associatedText,
            String rawSequence) {

        // Map special key codes to KeyEvent.Special
        KeyEvent.Special special = mapKittySpecialKey(keyCode);
        if (special != null) {
            return new KeyEvent(
                    KeyEvent.Type.Special,
                    '\0',
                    null,
                    special,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Map arrow keys from PUA codepoints
        KeyEvent.Arrow arrow = mapKittyArrowKey(keyCode);
        if (arrow != null) {
            return new KeyEvent(
                    KeyEvent.Type.Arrow,
                    '\0',
                    arrow,
                    null,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Map function keys F13-F35 (F1-F12 use legacy CSI ~ format)
        int fKey = mapKittyFunctionKey(keyCode);
        if (fKey > 0) {
            return new KeyEvent(
                    KeyEvent.Type.Function,
                    '\0',
                    null,
                    null,
                    fKey,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Printable characters: Unicode codepoints outside the BMP Private Use Area
        // (U+E000–U+F8FF = 57344–63743) where the kitty protocol defines functional key codes.
        // This allows non-BMP characters (emoji, CJK Extension B, etc.) to be classified correctly.
        if (keyCode >= 32 && !(keyCode >= 0xE000 && keyCode <= 0xF8FF)) {
            char ch = (keyCode <= Character.MAX_VALUE) ? (char) keyCode : '\0';
            return new KeyEvent(
                    KeyEvent.Type.Character,
                    ch,
                    null,
                    null,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Map keypad keys
        KeyEvent.Keypad keypad = mapKittyKeypadKey(keyCode);
        if (keypad != null) {
            return new KeyEvent(
                    KeyEvent.Type.Keypad,
                    '\0',
                    null,
                    null,
                    keypad,
                    null,
                    null,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Map media keys
        KeyEvent.MediaKey media = mapKittyMediaKey(keyCode);
        if (media != null) {
            return new KeyEvent(
                    KeyEvent.Type.Media,
                    '\0',
                    null,
                    null,
                    null,
                    media,
                    null,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Map modifier keys (as standalone key events)
        KeyEvent.ModKey modKey = mapKittyModifierKey(keyCode);
        if (modKey != null) {
            return new KeyEvent(
                    KeyEvent.Type.ModifierKey,
                    '\0',
                    null,
                    null,
                    null,
                    null,
                    modKey,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Unknown key code
        return new KeyEvent(
                KeyEvent.Type.Unknown,
                '\0',
                null,
                null,
                null,
                null,
                null,
                0,
                modifiers,
                rawSequence,
                eventType,
                keyCode,
                shiftedKeyCode,
                baseLayoutKeyCode,
                associatedText);
    }

    /**
     * Maps a kitty protocol key code to a KeyEvent.Special value.
     *
     * <p>Note: Insert, Delete, Home, End, PageUp, PageDown normally use legacy
     * {@code CSI number ~} encoding even with the kitty protocol, so they are
     * handled in the legacy parser.  The PUA codes below are defined by the
     * protocol spec and mapped here defensively in case a terminal emits them
     * as {@code CSI u} sequences.</p>
     */
    private static KeyEvent.Special mapKittySpecialKey(int keyCode) {
        switch (keyCode) {
            // Standard codepoints (always sent as CSI u)
            case 13:
                return KeyEvent.Special.Enter;
            case 9:
                return KeyEvent.Special.Tab;
            case 27:
                return KeyEvent.Special.Escape;
            case 127:
                return KeyEvent.Special.Backspace;
            // PUA codepoints defined by the kitty protocol for navigation keys
            case 57348:
                return KeyEvent.Special.Insert;
            case 57349:
                return KeyEvent.Special.Delete;
            case 57354:
                return KeyEvent.Special.PageUp;
            case 57355:
                return KeyEvent.Special.PageDown;
            case 57356:
                return KeyEvent.Special.Home;
            case 57357:
                return KeyEvent.Special.End;
            case 57358:
                return KeyEvent.Special.CapsLock;
            case 57359:
                return KeyEvent.Special.ScrollLock;
            case 57360:
                return KeyEvent.Special.NumLock;
            case 57361:
                return KeyEvent.Special.PrintScreen;
            case 57362:
                return KeyEvent.Special.Pause;
            case 57363:
                return KeyEvent.Special.Menu;
            default:
                return null;
        }
    }

    private static KeyEvent.Arrow mapKittyArrowKey(int keyCode) {
        switch (keyCode) {
            case 57350:
                return KeyEvent.Arrow.Left;
            case 57351:
                return KeyEvent.Arrow.Right;
            case 57352:
                return KeyEvent.Arrow.Up;
            case 57353:
                return KeyEvent.Arrow.Down;
            default:
                return null;
        }
    }

    /**
     * Maps a kitty protocol key code to a function key number (13-35).
     * F13-F35 use Unicode Private Use Area codes 57376-57398.
     */
    private static int mapKittyFunctionKey(int keyCode) {
        // F1-F12: PUA codes 57364-57375
        if (keyCode >= 57364 && keyCode <= 57375) {
            return keyCode - 57364 + 1;
        }
        // F13-F35: PUA codes 57376-57398
        if (keyCode >= 57376 && keyCode <= 57398) {
            return keyCode - 57376 + 13;
        }
        return 0;
    }

    private static KeyEvent.Keypad mapKittyKeypadKey(int keyCode) {
        switch (keyCode) {
            case 57399:
                return KeyEvent.Keypad.KP0;
            case 57400:
                return KeyEvent.Keypad.KP1;
            case 57401:
                return KeyEvent.Keypad.KP2;
            case 57402:
                return KeyEvent.Keypad.KP3;
            case 57403:
                return KeyEvent.Keypad.KP4;
            case 57404:
                return KeyEvent.Keypad.KP5;
            case 57405:
                return KeyEvent.Keypad.KP6;
            case 57406:
                return KeyEvent.Keypad.KP7;
            case 57407:
                return KeyEvent.Keypad.KP8;
            case 57408:
                return KeyEvent.Keypad.KP9;
            case 57409:
                return KeyEvent.Keypad.Decimal;
            case 57410:
                return KeyEvent.Keypad.Divide;
            case 57411:
                return KeyEvent.Keypad.Multiply;
            case 57412:
                return KeyEvent.Keypad.Subtract;
            case 57413:
                return KeyEvent.Keypad.Add;
            case 57414:
                return KeyEvent.Keypad.Enter;
            case 57415:
                return KeyEvent.Keypad.Equal;
            case 57416:
                return KeyEvent.Keypad.Separator;
            case 57417:
                return KeyEvent.Keypad.Left;
            case 57418:
                return KeyEvent.Keypad.Right;
            case 57419:
                return KeyEvent.Keypad.Up;
            case 57420:
                return KeyEvent.Keypad.Down;
            case 57421:
                return KeyEvent.Keypad.PageUp;
            case 57422:
                return KeyEvent.Keypad.PageDown;
            case 57423:
                return KeyEvent.Keypad.Home;
            case 57424:
                return KeyEvent.Keypad.End;
            case 57425:
                return KeyEvent.Keypad.Insert;
            case 57426:
                return KeyEvent.Keypad.Delete;
            case 57427:
                return KeyEvent.Keypad.Begin;
            default:
                return null;
        }
    }

    private static KeyEvent.MediaKey mapKittyMediaKey(int keyCode) {
        switch (keyCode) {
            case 57428:
                return KeyEvent.MediaKey.Play;
            case 57429:
                return KeyEvent.MediaKey.Pause;
            case 57430:
                return KeyEvent.MediaKey.PlayPause;
            case 57431:
                return KeyEvent.MediaKey.Reverse;
            case 57432:
                return KeyEvent.MediaKey.Stop;
            case 57433:
                return KeyEvent.MediaKey.FastForward;
            case 57434:
                return KeyEvent.MediaKey.Rewind;
            case 57435:
                return KeyEvent.MediaKey.TrackNext;
            case 57436:
                return KeyEvent.MediaKey.TrackPrevious;
            case 57437:
                return KeyEvent.MediaKey.Record;
            case 57438:
                return KeyEvent.MediaKey.LowerVolume;
            case 57439:
                return KeyEvent.MediaKey.RaiseVolume;
            case 57440:
                return KeyEvent.MediaKey.MuteVolume;
            default:
                return null;
        }
    }

    private static KeyEvent.ModKey mapKittyModifierKey(int keyCode) {
        switch (keyCode) {
            case 57441:
                return KeyEvent.ModKey.LeftShift;
            case 57442:
                return KeyEvent.ModKey.LeftControl;
            case 57443:
                return KeyEvent.ModKey.LeftAlt;
            case 57444:
                return KeyEvent.ModKey.LeftSuper;
            case 57445:
                return KeyEvent.ModKey.LeftHyper;
            case 57446:
                return KeyEvent.ModKey.LeftMeta;
            case 57447:
                return KeyEvent.ModKey.RightShift;
            case 57448:
                return KeyEvent.ModKey.RightControl;
            case 57449:
                return KeyEvent.ModKey.RightAlt;
            case 57450:
                return KeyEvent.ModKey.RightSuper;
            case 57451:
                return KeyEvent.ModKey.RightHyper;
            case 57452:
                return KeyEvent.ModKey.RightMeta;
            default:
                return null;
        }
    }
}
