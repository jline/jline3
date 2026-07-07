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
 * Represents a keyboard event in a terminal.
 *
 * <p>
 * The KeyEvent class encapsulates information about keyboard actions in a terminal,
 * including the type of key pressed, any modifier keys that were held, and the
 * raw sequence that was received from the terminal.
 * </p>
 *
 * <p>
 * Key events include:
 * </p>
 * <ul>
 *   <li><b>Character</b> - A printable character was typed</li>
 *   <li><b>Arrow</b> - An arrow key was pressed (Up, Down, Left, Right)</li>
 *   <li><b>Function</b> - A function key was pressed (F1-F12)</li>
 *   <li><b>Special</b> - A special key was pressed (Enter, Tab, Escape, etc.)</li>
 *   <li><b>Unknown</b> - An unrecognized key sequence</li>
 * </ul>
 */
@SuppressWarnings(
        "java:S115") // enum constants use CamelCase to match JLine conventions (Arrow.Up, Special.Enter, etc.)
public class KeyEvent {

    /**
     * Defines the types of key events that can occur.
     */
    public enum Type {
        /**
         * A printable character was typed.
         */
        Character,

        /**
         * An arrow key was pressed.
         */
        Arrow,

        /**
         * A function key was pressed (F1-F12).
         */
        Function,

        /**
         * A special key was pressed (Enter, Tab, Escape, etc.).
         */
        Special,

        /**
         * An unrecognized key sequence.
         */
        Unknown
    }

    /**
     * Defines arrow key directions.
     */
    public enum Arrow {
        Up,
        Down,
        Left,
        Right
    }

    /**
     * Defines special keys.
     */
    public enum Special {
        Enter,
        Tab,
        Escape,
        Backspace,
        Delete,
        Home,
        End,
        PageUp,
        PageDown,
        Insert
    }

    /**
     * Defines the type of key action for protocols that distinguish
     * press, repeat, and release events (e.g., Kitty Keyboard Protocol).
     *
     * <p>Legacy terminals only report press events, so the default is {@link #Press}.</p>
     */
    public enum EventType {
        /** A key was pressed. */
        Press,
        /** A held key is repeating. */
        Repeat,
        /** A key was released. */
        Release
    }

    /**
     * Defines modifier keys that can be held during a key event.
     */
    public enum Modifier {
        /**
         * The Shift key was held.
         */
        Shift,

        /**
         * The Alt key was held.
         */
        Alt,

        /**
         * The Control key was held.
         */
        Control,

        /**
         * The Super (Windows/Command) key was held.
         * Reported by the Kitty Keyboard Protocol.
         */
        Super,

        /**
         * The Hyper key was held.
         * Reported by the Kitty Keyboard Protocol.
         */
        Hyper,

        /**
         * The Meta key was held.
         * Reported by the Kitty Keyboard Protocol.
         */
        Meta,

        /**
         * The Caps Lock key was active.
         * Reported by the Kitty Keyboard Protocol.
         */
        CapsLock,

        /**
         * The Num Lock key was active.
         * Reported by the Kitty Keyboard Protocol.
         */
        NumLock
    }

    private final Type type;
    private final char character;
    private final Arrow arrow;
    private final Special special;
    private final int functionKey;
    private final EnumSet<Modifier> modifiers;
    private final String rawSequence;
    private final EventType eventType;
    private final int keyCode;
    private final int shiftedKeyCode;
    private final int baseLayoutKeyCode;
    private final String associatedText;

    /**
     * Creates a character key event.
     */
    public KeyEvent(char character, EnumSet<Modifier> modifiers, String rawSequence) {
        this(Type.Character, character, null, null, 0, modifiers, rawSequence, EventType.Press, 0, 0, 0, null);
    }

    /**
     * Creates an arrow key event.
     */
    public KeyEvent(Arrow arrow, EnumSet<Modifier> modifiers, String rawSequence) {
        this(Type.Arrow, '\0', arrow, null, 0, modifiers, rawSequence, EventType.Press, 0, 0, 0, null);
    }

    /**
     * Creates a special key event.
     */
    public KeyEvent(Special special, EnumSet<Modifier> modifiers, String rawSequence) {
        this(Type.Special, '\0', null, special, 0, modifiers, rawSequence, EventType.Press, 0, 0, 0, null);
    }

    /**
     * Creates a function key event.
     */
    public KeyEvent(int functionKey, EnumSet<Modifier> modifiers, String rawSequence) {
        this(Type.Function, '\0', null, null, functionKey, modifiers, rawSequence, EventType.Press, 0, 0, 0, null);
    }

    /**
     * Creates an unknown key event.
     */
    public KeyEvent(String rawSequence) {
        this(
                Type.Unknown,
                '\0',
                null,
                null,
                0,
                EnumSet.noneOf(Modifier.class),
                rawSequence,
                EventType.Press,
                0,
                0,
                0,
                null);
    }

    /**
     * Full constructor with all Kitty Keyboard Protocol fields.
     *
     * @param type              the key event type
     * @param character         the character for Character events
     * @param arrow             the arrow direction for Arrow events
     * @param special           the special key for Special events
     * @param functionKey       the function key number for Function events
     * @param modifiers         active modifier keys
     * @param rawSequence       the raw terminal escape sequence
     * @param eventType         press, repeat, or release
     * @param keyCode           the Unicode key code from the protocol
     * @param shiftedKeyCode    the shifted variant key code (0 if absent)
     * @param baseLayoutKeyCode the base layout key code (0 if absent)
     * @param associatedText    the associated text reported by the terminal
     */
    @SuppressWarnings({"java:S107", "java:S1319"
    }) // parameter count matches the protocol; EnumSet matches JLine API convention
    public KeyEvent(
            Type type,
            char character,
            Arrow arrow,
            Special special,
            int functionKey,
            EnumSet<Modifier> modifiers,
            String rawSequence,
            EventType eventType,
            int keyCode,
            int shiftedKeyCode,
            int baseLayoutKeyCode,
            String associatedText) {
        this.type = type;
        this.character = character;
        this.arrow = arrow;
        this.special = special;
        this.functionKey = functionKey;
        this.modifiers = modifiers;
        this.rawSequence = rawSequence;
        this.eventType = eventType;
        this.keyCode = keyCode;
        this.shiftedKeyCode = shiftedKeyCode;
        this.baseLayoutKeyCode = baseLayoutKeyCode;
        this.associatedText = associatedText;
    }

    public Type getType() {
        return type;
    }

    public char getCharacter() {
        return character;
    }

    public Arrow getArrow() {
        return arrow;
    }

    public Special getSpecial() {
        return special;
    }

    public int getFunctionKey() {
        return functionKey;
    }

    public EnumSet<Modifier> getModifiers() {
        return modifiers;
    }

    public String getRawSequence() {
        return rawSequence;
    }

    public boolean hasModifier(Modifier modifier) {
        return modifiers.contains(modifier);
    }

    /**
     * Returns the event type (press, repeat, or release).
     * Only meaningful when the Kitty Keyboard Protocol is active with
     * the {@code FLAG_REPORT_EVENTS} flag enabled.
     *
     * @return the event type, defaults to {@link EventType#Press}
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns the Unicode key code as reported by the Kitty Keyboard Protocol.
     * This is the primary key identifier — always the lowercase/unshifted
     * codepoint for letter keys.
     *
     * @return the key code, or 0 if not from a kitty protocol sequence
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Returns the shifted variant key code.
     * Only present when the Kitty Keyboard Protocol is active with
     * the {@code FLAG_REPORT_ALTERNATES} flag and Shift is held.
     *
     * @return the shifted key code, or 0 if absent
     */
    public int getShiftedKeyCode() {
        return shiftedKeyCode;
    }

    /**
     * Returns the base layout key code (the key on a US keyboard layout).
     * Only present when the Kitty Keyboard Protocol is active with
     * the {@code FLAG_REPORT_ALTERNATES} flag.
     *
     * @return the base layout key code, or 0 if absent
     */
    public int getBaseLayoutKeyCode() {
        return baseLayoutKeyCode;
    }

    /**
     * Returns the associated text reported by the terminal.
     * Only present when the Kitty Keyboard Protocol is active with
     * the {@code FLAG_REPORT_TEXT} flag.
     *
     * @return the associated text, or null if absent
     */
    public String getAssociatedText() {
        return associatedText;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("KeyEvent[type=").append(type);
        switch (type) {
            case Character:
                sb.append(", character='").append(character).append("'");
                break;
            case Arrow:
                sb.append(", arrow=").append(arrow);
                break;
            case Special:
                sb.append(", special=").append(special);
                break;
            case Function:
                sb.append(", function=F").append(functionKey);
                break;
            case Unknown:
                sb.append(", unknown");
                break;
        }
        if (!modifiers.isEmpty()) {
            sb.append(", modifiers=").append(modifiers);
        }
        if (eventType != EventType.Press) {
            sb.append(", eventType=").append(eventType);
        }
        if (keyCode != 0) {
            sb.append(", keyCode=").append(keyCode);
        }
        if (associatedText != null) {
            sb.append(", text='").append(associatedText).append("'");
        }
        sb.append(", raw='").append(rawSequence).append("']");
        return sb.toString();
    }
}
