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
     * <p>
     * This implementation enables SGR mouse mode (1006) by default, which provides better
     * support for mouse events, including explicit release events and extended coordinates.
     * It also disables other mouse modes (1015, 1016) when turning off mouse tracking to ensure
     * a clean state.
     * </p>
     *
     * @param terminal the terminal to configure
     * @param tracking the mouse tracking mode to enable
     * @return {@code true} if mouse tracking is supported and was configured, {@code false} otherwise
     */
    public static boolean trackMouse(Terminal terminal, Terminal.MouseTracking tracking) {
        if (hasMouseSupport(terminal)) {
            switch (tracking) {
                case Off:
                    terminal.writer()
                            .write("\033[?1000l\033[?1002l\033[?1003l\033[?1005l\033[?1006l\033[?1015l\033[?1016l");
                    break;
                case Normal:
                    terminal.writer().write("\033[?1006h\033[?1000h");
                    break;
                case Button:
                    terminal.writer().write("\033[?1006h\033[?1002h");
                    break;
                case Any:
                    terminal.writer().write("\033[?1006h\033[?1003h");
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
     *
     * <p>
     * This implementation supports multiple mouse event formats:
     * </p>
     * <ul>
     *   <li>X10 format (default) - Basic mouse reporting</li>
     *   <li>SGR format (1006) - Extended mouse reporting with explicit release events</li>
     *   <li>URXVT format (1015) - Extended mouse reporting with decimal coordinates</li>
     *   <li>SGR-Pixels format (1016) - Like SGR but reports position in pixels</li>
     * </ul>
     */
    public static MouseEvent readMouse(IntSupplier reader, MouseEvent last) {
        int c = reader.getAsInt();

        // Detect the mouse event format based on the first character
        if (c == '<') {
            // SGR (1006) or SGR-Pixels (1016) format
            return readMouseSGR(reader, last);
        } else if (c >= '0' && c <= '9') {
            // URXVT (1015) format
            return readMouseURXVT(c, reader, last);
        } else {
            // X10 format (default)
            return readMouseX10(c - ' ', reader, last);
        }
    }

    /**
     * Reads a mouse event in X10 format.
     *
     * @param cb the button code (already read)
     * @param reader the input supplier to read from
     * @param last the previous mouse event
     * @return the mouse event that was read
     */
    private static MouseEvent readMouseX10(int cb, IntSupplier reader, MouseEvent last) {
        int cx = reader.getAsInt() - ' ' - 1;
        int cy = reader.getAsInt() - ' ' - 1;
        return parseMouseEvent(cb, cx, cy, false, last);
    }

    /**
     * Reads a mouse event in SGR format (1006 or 1016).
     * Format: CSI < Cb ; Cx ; Cy M/m
     *
     * @param reader the input supplier to read from
     * @param last the previous mouse event
     * @return the mouse event that was read
     */
    private static MouseEvent readMouseSGR(IntSupplier reader, MouseEvent last) {
        StringBuilder sb = new StringBuilder();
        int[] params = new int[3];
        int paramIndex = 0;
        boolean isPixels = false;
        boolean isRelease = false;

        // Read parameters until 'M' or 'm' is encountered
        int c;
        while ((c = reader.getAsInt()) != -1) {
            if (c == 'M' || c == 'm') {
                isRelease = (c == 'm');
                break;
            } else if (c == ';') {
                if (paramIndex < params.length) {
                    try {
                        params[paramIndex++] = Integer.parseInt(sb.toString());
                    } catch (NumberFormatException e) {
                        // Invalid parameter, use default
                        params[paramIndex++] = 0;
                    }
                    sb.setLength(0);
                }
            } else if (c >= '0' && c <= '9') {
                sb.append((char) c);
            }
        }

        // Parse the last parameter if any
        if (sb.length() > 0 && paramIndex < params.length) {
            try {
                params[paramIndex] = Integer.parseInt(sb.toString());
            } catch (NumberFormatException e) {
                // Invalid parameter, use default
                params[paramIndex] = 0;
            }
        }

        int cb = params[0];
        int cx = params[1] - 1; // Convert to 0-based
        int cy = params[2] - 1; // Convert to 0-based

        // Check if this is SGR-Pixels format (1016)
        // In practice, we don't need to handle this differently as the MouseEvent
        // doesn't distinguish between cell and pixel coordinates

        return parseMouseEvent(cb, cx, cy, isRelease, last);
    }

    /**
     * Reads a mouse event in URXVT format (1015).
     * Format: CSI Cb ; Cx ; Cy M
     *
     * @param firstDigit the first digit of the button code (already read)
     * @param reader the input supplier to read from
     * @param last the previous mouse event
     * @return the mouse event that was read
     */
    private static MouseEvent readMouseURXVT(int firstDigit, IntSupplier reader, MouseEvent last) {
        StringBuilder sb = new StringBuilder().append((char) firstDigit);
        int[] params = new int[3];
        int paramIndex = 0;

        // Read parameters until 'M' is encountered
        int c;
        while ((c = reader.getAsInt()) != -1) {
            if (c == 'M') {
                break;
            } else if (c == ';') {
                if (paramIndex < params.length) {
                    try {
                        params[paramIndex++] = Integer.parseInt(sb.toString());
                    } catch (NumberFormatException e) {
                        // Invalid parameter, use default
                        params[paramIndex++] = 0;
                    }
                    sb.setLength(0);
                }
            } else if (c >= '0' && c <= '9') {
                sb.append((char) c);
            }
        }

        // Parse the last parameter if any
        if (sb.length() > 0 && paramIndex < params.length) {
            try {
                params[paramIndex] = Integer.parseInt(sb.toString());
            } catch (NumberFormatException e) {
                // Invalid parameter, use default
                params[paramIndex] = 0;
            }
        }

        int cb = params[0];
        int cx = params[1] - 1; // Convert to 0-based
        int cy = params[2] - 1; // Convert to 0-based

        return parseMouseEvent(cb, cx, cy, false, last);
    }

    /**
     * Parses a mouse event from the given parameters.
     *
     * @param cb the button code
     * @param cx the x coordinate
     * @param cy the y coordinate
     * @param isRelease whether this is an explicit release event (SGR format)
     * @param last the previous mouse event
     * @return the parsed mouse event
     */
    private static MouseEvent parseMouseEvent(int cb, int cx, int cy, boolean isRelease, MouseEvent last) {
        MouseEvent.Type type;
        MouseEvent.Button button;
        EnumSet<MouseEvent.Modifier> modifiers = EnumSet.noneOf(MouseEvent.Modifier.class);

        // Parse modifiers
        if ((cb & 4) == 4) {
            modifiers.add(MouseEvent.Modifier.Shift);
        }
        if ((cb & 8) == 8) {
            modifiers.add(MouseEvent.Modifier.Alt);
        }
        if ((cb & 16) == 16) {
            modifiers.add(MouseEvent.Modifier.Control);
        }

        // Handle wheel events
        if ((cb & 64) == 64) {
            type = MouseEvent.Type.Wheel;
            button = (cb & 1) == 1 ? MouseEvent.Button.WheelDown : MouseEvent.Button.WheelUp;
        } else {
            // Handle button events
            if (isRelease) {
                // Explicit release event (SGR format)
                button = getButtonForCode(cb & 3);
                type = MouseEvent.Type.Released;
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
        }

        return new MouseEvent(type, button, modifiers, cx, cy);
    }

    /**
     * Gets the button for the given button code.
     *
     * @param code the button code
     * @return the corresponding button
     */
    private static MouseEvent.Button getButtonForCode(int code) {
        switch (code) {
            case 0:
                return MouseEvent.Button.Button1;
            case 1:
                return MouseEvent.Button.Button2;
            case 2:
                return MouseEvent.Button.Button3;
            default:
                return MouseEvent.Button.NoButton;
        }
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
