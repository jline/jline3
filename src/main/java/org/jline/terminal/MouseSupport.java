/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.LinkedList;

public class MouseSupport {

    public static final int READ_MOUSE_EVENT = -3;

    public enum Tracking {
        Normal, Button, Any
    }

    private enum Format {
        X10, EXT, SGR
    }

    public enum Type {
        Released,
        Pressed,
        Clicked,
        Wheel,
        Moved,
        Dragged
    }

    public enum Button {
        NoButton,
        Button1,
        Button2,
        Button3,
        WheelUp,
        WheelDown
    }

    public enum Modifier {
        Shift,
        Alt,
        Control
    }

    public static class Event {
        private final Type type;
        private final Button button;
        private final EnumSet<Modifier> modifiers;
        private final int x;
        private final int y;
        private final int clickCount;

        public Event(Type type, Button button, EnumSet<Modifier> modifiers, int x, int y, int clickCount) {
            this.type = type;
            this.button = button;
            this.modifiers = modifiers;
            this.x = x;
            this.y = y;
            this.clickCount = clickCount;
        }

        public Type getType() {
            return type;
        }

        public Button getButton() {
            return button;
        }

        public EnumSet<Modifier> getModifiers() {
            return modifiers;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getClickCount() {
            return clickCount;
        }

        @Override
        public String toString() {
            return "Event[" +
                    "type=" + type +
                    ", button=" + button +
                    ", modifiers=" + modifiers +
                    ", x=" + x +
                    ", y=" + y +
                    ", clickCount=" + clickCount +
                    ']';
        }
    }

    private enum Binding {
        Mouse, Key
    }

    private final Terminal terminal;
    private final Tracking tracking;
    private final Format format;
    private final BindingReader reader;
    private final KeyMap<Binding> keys;
    private final String init;
    private final String kmous;
    private final LinkedList<Event> events = new LinkedList<>();

    private Event last = new Event(Type.Moved, Button.NoButton, EnumSet.noneOf(Modifier.class), 0, 0, 0);

    public MouseSupport(Terminal terminal) {
        this(terminal, null, null);
    }

    public MouseSupport(Terminal terminal, Tracking tracking) {
        this(terminal, tracking, null);
    }

    public MouseSupport(Terminal terminal, Tracking tracking, Format format) {
        this.terminal = terminal;
        this.reader = new BindingReader(terminal.reader());

        String init = null;
        String kmous = null;
        if (terminal.getStringCapability(InfoCmp.Capability.key_mouse) != null
                || terminal.getType().contains("xterm")) {
            if (tracking == null) {
                tracking = Tracking.Normal;
            }
            if (format == null) {
                format = Format.X10;
            }
            switch (tracking) {
                case Normal:
                    init = privateMode("1000");
                    break;
                case Button:
                    init = privateMode("1002");
                    break;
                case Any:
                    init = privateMode("1003");
                    break;
            }
            switch (format) {
                case EXT:
                    init += privateMode("1005");
                    break;
                case SGR:
                    init += privateMode("1006");
                    break;
            }
            kmous = terminal.getStringCapability(InfoCmp.Capability.key_mouse);
            if (kmous == null) {
                kmous = "\\E[M";
            }
            StringWriter sw = new StringWriter();
            try {
                Curses.tputs(sw, kmous);
            } catch (IOException e) {
                throw new IOError(e);
            }
            kmous = sw.toString();
        }
        this.init = init;
        this.format = format;
        this.tracking = tracking;
        this.kmous = kmous;

        this.keys = new KeyMap<>();
        this.keys.setNomatch(Binding.Key);
        this.keys.setUnicode(Binding.Key);
        this.keys.bind(Binding.Mouse, this.kmous);
    }

    private String privateMode(String mode) {
        return "\\E[?" + mode + "%?%p1%{1}%=%th%el%;";
    }

    public boolean hasMouseSupport() {
        return init != null;
    }

    public String getKeyMouse() {
        return kmous;
    }

    public boolean enable(boolean enabled) throws IOException {
        if (hasMouseSupport()) {
            Curses.tputs(terminal.writer(), init, enabled ? 1 : 0);
            return true;
        } else {
            return false;
        }
    }

    public int readCodepoint() {
        Binding binding = reader.readBinding(keys);
        if (binding == Binding.Key) {
            return reader.getLastBinding().codePointAt(0);
        } else if (binding == Binding.Mouse) {
            events.add(readEvent());
            return READ_MOUSE_EVENT;
        } else {
            return -1;
        }
    }

    public Event readEvent() {
        int cb, cx, cy;
        switch (format) {
            case X10:
                try {
                    cb = terminal.input().read() - ' ';
                    cx = terminal.input().read() - ' ' - 1;
                    cy = terminal.input().read() - ' ' - 1;
                } catch (IOException e) {
                    throw new IOError(e);
                }
                break;
            case EXT:
                cb = reader.readCharacter() - ' ';
                cx = reader.readCharacter() - ' ' - 1;
                cy = reader.readCharacter() - ' ' - 1;
                break;
            default:
                throw new UnsupportedOperationException("TODO");
        }
        Type type;
        Button button;
        EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        if ((cb & 4) == 4) {
            modifiers.add(Modifier.Shift);
        }
        if ((cb & 8) == 8) {
            modifiers.add(Modifier.Alt);
        }
        if ((cb & 16) == 16) {
            modifiers.add(Modifier.Control);
        }
        if ((cb & 64) == 64) {
            type = Type.Wheel;
            button = (cb & 1) == 1 ? Button.WheelUp : Button.WheelDown;
        } else {
            int b = (cb & 3);
            switch (b) {
                case 0:
                    button = Button.Button1;
                    type = last.button == button ? Type.Dragged : Type.Pressed;
                    break;
                case 1:
                    button = Button.Button2;
                    type = last.button == button ? Type.Dragged : Type.Pressed;
                    break;
                case 2:
                    button = Button.Button3;
                    type = last.button == button ? Type.Dragged : Type.Pressed;
                    break;
                default:
                    if (last.type == Type.Pressed || last.type == Type.Dragged) {
                        button = last.button;
                        type = Type.Released;
                    } else {
                        button = Button.NoButton;
                        type = Type.Moved;
                    }
                    break;
            }
        }
        Event event = new Event(type, button, modifiers, cx, cy, 0);
        last = event;
        return event;
    }

    public Event getEvent() {
        if (events.isEmpty()) {
            return null;
        } else {
            return events.remove(0);
        }
    }

}
