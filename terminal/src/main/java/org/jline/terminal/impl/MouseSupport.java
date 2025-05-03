/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.EOFException;
import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.function.IntSupplier;

import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;
import org.jline.utils.InputStreamReader;

/**
 * Utility class for mouse support in terminals.
 *
 * <p>
 * The MouseSupport class provides functionality for enabling, disabling, and
 * processing mouse events in terminals that support mouse tracking. It handles
 * the details of sending the appropriate escape sequences to the terminal to
 * enable different mouse tracking modes and parsing the responses to create
 * MouseEvent objects.
 * </p>
 *
 * <p>
 * This class is used internally by terminal implementations to implement the
 * mouse-related methods defined in the Terminal interface, such as
 * {@link Terminal#hasMouseSupport()}, {@link Terminal#trackMouse(Terminal.MouseTracking)},
 * and {@link Terminal#readMouseEvent()}.
 * </p>
 *
 * <p>
 * Mouse tracking in terminals typically works by:
 * </p>
 * <ol>
 *   <li>Sending special escape sequences to enable a specific mouse tracking mode</li>
 *   <li>Receiving escape sequences from the terminal when mouse events occur</li>
 *   <li>Parsing these sequences to extract information about the event type, button, modifiers, and coordinates</li>
 *   <li>Creating MouseEvent objects that represent these events</li>
 * </ol>
 *
 * <p>
 * Note that mouse support is not available in all terminals, and the methods in
 * this class may not work correctly if the terminal does not support mouse tracking.
 * </p>
 *
 * @see Terminal#hasMouseSupport()
 * @see Terminal#trackMouse(Terminal.MouseTracking)
 * @see Terminal#readMouseEvent()
 * @see MouseEvent
 */
public class MouseSupport {

    /**
     * Checks if the terminal supports mouse tracking.
     *
     * <p>
     * This method determines whether the terminal supports mouse tracking by
     * checking if it has the key_mouse capability. This capability is required
     * for mouse tracking to work correctly.
     * </p>
     *
     * @param terminal the terminal to check
     * @return {@code true} if the terminal supports mouse tracking, {@code false} otherwise
     */
    public static boolean hasMouseSupport(Terminal terminal) {
        return terminal.getStringCapability(InfoCmp.Capability.key_mouse) != null;
    }

    /**
     * Enables or disables mouse tracking in the terminal.
     *
     * <p>
     * This method sends the appropriate escape sequences to the terminal to
     * enable or disable mouse tracking according to the specified tracking mode.
     * The available tracking modes are:
     * </p>
     * <ul>
     *   <li>{@link Terminal.MouseTracking#Off} - Disables mouse tracking</li>
     *   <li>{@link Terminal.MouseTracking#Normal} - Reports button press and release events</li>
     *   <li>{@link Terminal.MouseTracking#Button} - Reports button press, release, and motion events while buttons are pressed</li>
     *   <li>{@link Terminal.MouseTracking#Any} - Reports all mouse events, including movement without buttons pressed</li>
     * </ul>
     *
     * @param terminal the terminal to configure
     * @param tracking the mouse tracking mode to enable
     * @return {@code true} if mouse tracking is supported and was configured, {@code false} otherwise
     */
    public static boolean trackMouse(Terminal terminal, Terminal.MouseTracking tracking) {
        if (hasMouseSupport(terminal)) {
            switch (tracking) {
                case Off:
                    terminal.writer().write("\033[?1000l\033[?1002l\033[?1003l\033[?1005l");
                    break;
                case Normal:
                    terminal.writer().write("\033[?1005h\033[?1000h");
                    break;
                case Button:
                    terminal.writer().write("\033[?1005h\033[?1002h");
                    break;
                case Any:
                    terminal.writer().write("\033[?1005h\033[?1003h");
                    break;
            }
            terminal.flush();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reads a mouse event from the terminal.
     *
     * <p>
     * This method reads a mouse event from the terminal's input stream and
     * converts it into a MouseEvent object. It uses the previous mouse event
     * to determine the type of the new event (e.g., to distinguish between
     * press, drag, and release events).
     * </p>
     *
     * @param terminal the terminal to read from
     * @param last the previous mouse event, used to determine the type of the new event
     * @return the mouse event that was read
     */
    public static MouseEvent readMouse(Terminal terminal, MouseEvent last) {
        return readMouse(() -> readExt(terminal), last);
    }

    /**
     * Reads a mouse event using the provided input supplier.
     *
     * <p>
     * This method reads a mouse event using the provided input supplier and
     * converts it into a MouseEvent object. It uses the previous mouse event
     * to determine the type of the new event (e.g., to distinguish between
     * press, drag, and release events).
     * </p>
     *
     * <p>
     * The input supplier should provide the raw bytes of the mouse event data.
     * This method expects the data to be in the format used by xterm-compatible
     * terminals for mouse reporting.
     * </p>
     *
     * @param reader the input supplier to read from
     * @param last the previous mouse event, used to determine the type of the new event
     * @return the mouse event that was read
     */
    public static MouseEvent readMouse(IntSupplier reader, MouseEvent last) {
        int cb = reader.getAsInt() - ' ';
        int cx = reader.getAsInt() - ' ' - 1;
        int cy = reader.getAsInt() - ' ' - 1;
        MouseEvent.Type type;
        MouseEvent.Button button;
        EnumSet<MouseEvent.Modifier> modifiers = EnumSet.noneOf(MouseEvent.Modifier.class);
        if ((cb & 4) == 4) {
            modifiers.add(MouseEvent.Modifier.Shift);
        }
        if ((cb & 8) == 8) {
            modifiers.add(MouseEvent.Modifier.Alt);
        }
        if ((cb & 16) == 16) {
            modifiers.add(MouseEvent.Modifier.Control);
        }
        if ((cb & 64) == 64) {
            type = MouseEvent.Type.Wheel;
            button = (cb & 1) == 1 ? MouseEvent.Button.WheelDown : MouseEvent.Button.WheelUp;
        } else {
            int b = (cb & 3);
            switch (b) {
                case 0:
                    button = MouseEvent.Button.Button1;
                    if (last.getButton() == button
                            && (last.getType() == MouseEvent.Type.Pressed
                                    || last.getType() == MouseEvent.Type.Dragged)) {
                        type = MouseEvent.Type.Dragged;
                    } else {
                        type = MouseEvent.Type.Pressed;
                    }
                    break;
                case 1:
                    button = MouseEvent.Button.Button2;
                    if (last.getButton() == button
                            && (last.getType() == MouseEvent.Type.Pressed
                                    || last.getType() == MouseEvent.Type.Dragged)) {
                        type = MouseEvent.Type.Dragged;
                    } else {
                        type = MouseEvent.Type.Pressed;
                    }
                    break;
                case 2:
                    button = MouseEvent.Button.Button3;
                    if (last.getButton() == button
                            && (last.getType() == MouseEvent.Type.Pressed
                                    || last.getType() == MouseEvent.Type.Dragged)) {
                        type = MouseEvent.Type.Dragged;
                    } else {
                        type = MouseEvent.Type.Pressed;
                    }
                    break;
                default:
                    if (last.getType() == MouseEvent.Type.Pressed || last.getType() == MouseEvent.Type.Dragged) {
                        button = last.getButton();
                        type = MouseEvent.Type.Released;
                    } else {
                        button = MouseEvent.Button.NoButton;
                        type = MouseEvent.Type.Moved;
                    }
                    break;
            }
        }
        return new MouseEvent(type, button, modifiers, cx, cy);
    }

    /**
     * Reads a single character from the terminal's input stream.
     *
     * <p>
     * This method reads a single character from the terminal's input stream,
     * handling the case where the terminal's encoding is not UTF-8. Mouse events
     * are encoded in UTF-8, so if the terminal is using a different encoding,
     * this method creates a temporary UTF-8 reader to read the character.
     * </p>
     *
     * @param terminal the terminal to read from
     * @return the character that was read
     * @throws IOError if an I/O error occurs while reading
     */
    private static int readExt(Terminal terminal) {
        try {
            // The coordinates are encoded in UTF-8, so if that's not the input encoding,
            // we need to get around
            int c;
            if (terminal.encoding() != StandardCharsets.UTF_8) {
                c = new InputStreamReader(terminal.input(), StandardCharsets.UTF_8).read();
            } else {
                c = terminal.reader().read();
            }
            if (c < 0) {
                throw new EOFException();
            }
            return c;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
