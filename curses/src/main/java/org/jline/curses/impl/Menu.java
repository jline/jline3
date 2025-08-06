/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.curses.Curses;
import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

public class Menu extends AbstractComponent {

    enum Action {
        Left,
        Right,
        Up,
        Down,
        Execute,
        Close
    }

    private final List<SubMenu> contents;
    private SubMenu selected;
    private KeyMap<Object> keyMap;
    private KeyMap<Object> global;
    private final BindingReader bindingReader = new BindingReader(new NonBlockingReader() {
        @Override
        protected int read(long timeout, boolean isPeek) {
            return -1;
        }

        @Override
        public int readBuffered(char[] b) {
            return -1;
        }

        @Override
        public int readBuffered(char[] b, int off, int len, long timeout) {
            return -1;
        }

        @Override
        public void close() {}
    });
    private final Map<SubMenu, MenuWindow> windows = new HashMap<>();

    public Menu(List<SubMenu> contents) {
        this.contents = contents;
        for (SubMenu s : contents) {
            this.windows.put(s, new MenuWindow(s));
        }
    }

    public List<SubMenu> getContents() {
        return contents;
    }

    @Override
    protected void doDraw(Screen screen) {
        AttributedStyle tn = getTheme().getStyle(".menu.text.normal");
        AttributedStyle kn = getTheme().getStyle(".menu.key.normal");
        AttributedStyle ts = getTheme().getStyle(".menu.text.selected");
        AttributedStyle ks = getTheme().getStyle(".menu.key.selected");
        int x = getScreenPosition().x();
        int y = getScreenPosition().y();
        int w = getSize().w();
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (SubMenu c : getContents()) {
            boolean selected = c == this.selected;
            String n = c.getName();
            String k = c.getKey();
            sb.style(tn);
            sb.append(" ");
            sb.style(selected ? ts : tn);
            sb.append(" ");
            int ki = k != null ? n.indexOf(k) : -1;
            if (ki >= 0) {
                sb.style(selected ? ts : tn);
                sb.append(n, 0, ki);
                sb.style(selected ? ks : kn);
                sb.append(n, ki, ki + k.length());
                sb.style(selected ? ts : tn);
                sb.append(n, ki + k.length(), n.length());
            } else {
                sb.style(selected ? ts : tn);
                sb.append(n);
            }
            sb.style(selected ? ts : tn);
            sb.append(" ");
            sb.style(tn);
            sb.append(" ");
        }
        sb.style(tn);
        while (sb.length() < w) {
            sb.append(' ');
        }
        screen.text(x, y, sb.toAttributedString());
    }

    @Override
    protected Size doGetPreferredSize() {
        int size = -1;
        for (SubMenu mc : getContents()) {
            size += mc.getName().length() + 5;
        }
        return new Size(size, 1);
    }

    @Override
    public boolean handleMouse(MouseEvent event) {
        int dx = event.getX() - getScreenPosition().x();
        SubMenu sel = null;
        for (SubMenu mc : getContents()) {
            int l = 4 + mc.getName().length();
            if (dx < l) {
                sel = mc;
                break;
            }
            dx -= l + 1;
        }
        select(sel);
        return true; // Mouse event handled
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        if (keyMap == null) {
            initializeKeyMaps();
        }

        // Handle key events directly based on KeyEvent type
        Action action = null;

        // Check for arrow keys
        if (event.getType() == KeyEvent.Type.Arrow) {
            switch (event.getArrow()) {
                case Up:
                    action = Action.Up;
                    break;
                case Down:
                    action = Action.Down;
                    break;
                case Left:
                    action = Action.Left;
                    break;
                case Right:
                    action = Action.Right;
                    break;
            }
        }
        // Check for special keys
        else if (event.getType() == KeyEvent.Type.Special) {
            switch (event.getSpecial()) {
                case Enter:
                    action = Action.Execute;
                    break;
                case Escape:
                    action = Action.Close;
                    break;
            }
        }
        // Check for character keys
        else if (event.getType() == KeyEvent.Type.Character) {
            char ch = event.getCharacter();
            if (ch == ' ' || ch == '\n' || ch == '\r') {
                action = Action.Execute;
            } else {
                // Check for menu item shortcuts
                for (SubMenu subMenu : contents) {
                    for (MenuItem item : subMenu.getContents()) {
                        String shortcut = item.getShortcut();
                        if (shortcut != null && isShortcutMatch(event, shortcut)) {
                            closeAndExecute(item);
                            return true;
                        }
                    }
                }
            }
        }

        // Execute the action if found
        if (action != null) {
            switch (action) {
                case Left:
                    select(contents.get((contents.indexOf(selected) + contents.size() - 1) % contents.size()));
                    break;
                case Right:
                    select(contents.get((contents.indexOf(selected) + contents.size() + 1) % contents.size()));
                    break;
                case Up:
                    if (selected != null) {
                        windows.get(selected).up();
                    }
                    break;
                case Down:
                    if (selected != null) {
                        windows.get(selected).down();
                    }
                    break;
                case Close:
                    if (selected != null) {
                        windows.get(selected).close();
                        selected = null; // Reset selection when submenu is closed with escape
                    }
                    break;
                case Execute:
                    if (selected != null) {
                        closeAndExecute(windows.get(selected).selected);
                    }
                    break;
            }
            return true; // Action was handled
        }

        return false; // Key not handled
    }

    private void initializeKeyMaps() {
        Terminal terminal = getWindow().getGUI().getTerminal();
        keyMap = new KeyMap<>();
        keyMap.bind(Action.Up, KeyMap.key(terminal, InfoCmp.Capability.key_up));
        keyMap.bind(Action.Down, KeyMap.key(terminal, InfoCmp.Capability.key_down));
        keyMap.bind(Action.Left, KeyMap.key(terminal, InfoCmp.Capability.key_left));
        keyMap.bind(Action.Right, KeyMap.key(terminal, InfoCmp.Capability.key_right));
        keyMap.bind(Action.Execute, KeyMap.key(terminal, InfoCmp.Capability.key_enter), " ", "\n", "\r");
        keyMap.bind(Action.Close, KeyMap.esc());
        global = new KeyMap<>();
        for (SubMenu subMenu : contents) {
            for (MenuItem item : subMenu.getContents()) {
                String s = item.getShortcut();
                if (s != null) {
                    global.bind(item, KeyMap.translate(s));
                }
            }
        }
    }

    private boolean isShortcutMatch(KeyEvent event, String shortcut) {
        // Simple shortcut matching - could be enhanced
        if (shortcut.length() == 1) {
            char shortcutChar = shortcut.toLowerCase().charAt(0);
            char eventChar = Character.toLowerCase(event.getCharacter());
            return shortcutChar == eventChar;
        }
        return false;
    }

    private void closeAndExecute(MenuItem item) {
        MenuWindow w = windows.get(selected);
        w.close();
        selected = null; // Reset selection when submenu closes
        if (item.getAction() != null) {
            item.getAction().run();
        }
    }

    private void select(SubMenu s) {
        if (s != selected) {
            if (selected != null) {
                windows.get(selected).close();
            }
            selected = s;
            if (selected != null) {
                getWindow().getGUI().addWindow(windows.get(selected));
            }
            invalidate(); // Trigger repaint when selection changes
        } else if (s != null) {
            // If clicking on the same submenu, close it (toggle behavior)
            windows.get(selected).close();
            selected = null;
            invalidate(); // Trigger repaint when selection changes
        }
    }

    @Override
    public void setPosition(Position position) {
        super.setPosition(position);
        Position p = getScreenPosition();
        int x = p.x();
        for (SubMenu mc : getContents()) {
            MenuWindow w = windows.get(mc);
            w.setPosition(new Position(x, p.y() + 1));
            w.setSize(w.getPreferredSize());
            int l = 4 + mc.getName().length();
            x += l + 1;
        }
    }

    class MenuWindow extends AbstractWindow {

        private final SubMenu subMenu;
        private final KeyMap<Object> keyMap;
        private MenuItem selected;

        public MenuWindow(SubMenu subMenu) {
            this.subMenu = subMenu;
            this.selected = subMenu.getContents().stream()
                    .filter(c -> c != MenuItem.SEPARATOR)
                    .findFirst()
                    .orElse(null);
            setBehaviors(EnumSet.of(Behavior.NoDecoration, Behavior.Popup, Behavior.ManualLayout));
            this.keyMap = new KeyMap<>();
            for (MenuItem item : subMenu.getContents()) {
                if (item.getKey() != null) {
                    keyMap.bind(item, item.getKey().toLowerCase());
                }
            }
        }

        @Override
        protected void doDraw(Screen screen) {
            AttributedStyle tn = getTheme().getStyle(".menu.text.normal");
            AttributedStyle kn = getTheme().getStyle(".menu.key.normal");
            AttributedStyle ts = getTheme().getStyle(".menu.text.selected");
            AttributedStyle ks = getTheme().getStyle(".menu.key.selected");
            Position p = getScreenPosition();
            Size s = getSize();
            if (s.h() <= 0 || s.w() <= 0) {
                return;
            }
            getTheme().box(screen, p.x(), p.y(), s.w(), s.h(), Curses.Border.Single, ".menu.border");
            int y = p.y() + 1;
            int ws = 0;
            for (MenuItem mi : subMenu.getContents()) {
                if (mi.getShortcut() != null) {
                    ws = Math.max(ws, mi.getShortcut().length());
                }
            }
            for (MenuItem c : subMenu.getContents()) {
                if (c == MenuItem.SEPARATOR) {
                    getTheme()
                            .separatorH(
                                    screen,
                                    p.x(),
                                    y++,
                                    s.w(),
                                    Curses.Border.Single,
                                    Curses.Border.Single,
                                    getTheme().getStyle(".menu.border"));
                    continue;
                }
                boolean selected = c == this.selected;
                String n = c.getName();
                String k = c.getKey();
                String t = c.getShortcut();
                AttributedStringBuilder sb = new AttributedStringBuilder(s.w());
                sb.style(selected ? ts : tn);
                sb.append(" ");
                int ki = k != null ? n.indexOf(k) : -1;
                if (ki >= 0) {
                    sb.style(selected ? ts : tn);
                    sb.append(n, 0, ki);
                    sb.style(selected ? ks : kn);
                    sb.append(n, ki, ki + k.length());
                    sb.style(selected ? ts : tn);
                    sb.append(n, ki + k.length(), n.length());
                } else {
                    sb.style(selected ? ts : tn);
                    sb.append(n);
                }
                sb.style(selected ? ts : tn);
                if (ws > 0) {
                    while (sb.length() < s.w() - 2 - ws) {
                        sb.append(" ");
                    }
                }
                if (t != null) {
                    sb.append(t);
                }
                sb.style(selected ? ts : tn);
                while (sb.length() < s.w() - 2) {
                    sb.append(" ");
                }
                screen.text(p.x() + 1, y++, sb.toAttributedString());
            }
        }

        @Override
        public boolean handleKey(KeyEvent event) {
            return Menu.this.handleKey(event);
        }

        void up() {
            move(-1);
        }

        void down() {
            move(+1);
        }

        void move(int dir) {
            List<MenuItem> contents = subMenu.getContents();
            int idx = contents.indexOf(selected);
            for (; ; ) {
                idx = (idx + contents.size() + dir) % contents.size();
                if (contents.get(idx) != MenuItem.SEPARATOR) {
                    break;
                }
            }
            selected = contents.get(idx);
            invalidate(); // Trigger repaint when selection changes
        }

        @Override
        public boolean handleMouse(MouseEvent event) {
            if (event.getType() == MouseEvent.Type.Pressed && !isIn(event.getX(), event.getY())) {
                close();
                Menu.this.selected = null; // Reset selection when clicking outside submenu
            } else {
                Position p = getScreenPosition();
                Size s = getSize();
                int x = p.x() + 1;
                int w = s.w() - 2;
                int y = p.y() + 1;
                if (x <= event.getX() && event.getX() <= x + w) {
                    MenuItem clicked = null;
                    for (MenuItem item : subMenu.getContents()) {
                        if (event.getY() == y) {
                            clicked = item;
                            break;
                        }
                        y++;
                    }
                    if (clicked != null && clicked != MenuItem.SEPARATOR) {
                        closeAndExecute(clicked);
                    }
                }
                super.handleMouse(event);
            }
            return true; // Mouse event handled
        }

        @Override
        public Size getPreferredSize() {
            int wn = 0;
            int ws = 0;
            int h = 0;
            for (MenuItem mi : subMenu.getContents()) {
                h++;
                if (mi == MenuItem.SEPARATOR) {
                    continue;
                }
                wn = Math.max(wn, mi.getName().length());
                if (mi.getShortcut() != null) {
                    ws = Math.max(ws, mi.getShortcut().length());
                }
            }
            return new Size(ws > 0 ? (1 + 1 + wn + 2 + ws + 1) : (1 + 1 + wn + 2), h + 2);
        }
    }
}
