/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.io.EOFException;
import java.io.IOError;
import java.io.IOException;
import java.util.EnumSet;
import java.util.function.IntSupplier;

public class MouseSupport {

    public static boolean hasMouseSupport(Terminal terminal) {
        return terminal.getStringCapability(InfoCmp.Capability.key_mouse) != null;
    }

    public static boolean trackMouse(Terminal terminal, Terminal.MouseTracking tracking) {
        if (hasMouseSupport(terminal)) {
            switch (tracking) {
                case Off:
                    terminal.writer().write("\033[?1000l");
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

    public static MouseEvent readMouse(Terminal terminal, MouseEvent last) {
        return readMouse(() -> readExt(terminal), last);
    }

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
                    type = last.getButton() == button ? MouseEvent.Type.Dragged : MouseEvent.Type.Pressed;
                    break;
                case 1:
                    button = MouseEvent.Button.Button2;
                    type = last.getButton() == button ? MouseEvent.Type.Dragged : MouseEvent.Type.Pressed;
                    break;
                case 2:
                    button = MouseEvent.Button.Button3;
                    type = last.getButton() == button ? MouseEvent.Type.Dragged : MouseEvent.Type.Pressed;
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

    private static int readExt(Terminal terminal) {
        try {
            int c = terminal.reader().read();
            if (c < 0) {
                throw new EOFException();
            }
            return c;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
