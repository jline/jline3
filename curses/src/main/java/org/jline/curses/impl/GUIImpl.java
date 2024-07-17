/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.io.IOException;
import java.util.*;

import org.jline.curses.*;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp;

public class GUIImpl implements GUI {

    private final Terminal terminal;
    private final Deque<Window> windows = new ArrayDeque<>();
    private Window activeWindow;
    private final AbstractWindow background;
    private Size size;
    private Display display;
    private final Map<Class<?>, Renderer> renderers = new HashMap<>();
    private Theme theme = new DefaultTheme();

    public GUIImpl(Terminal terminal) {
        this.terminal = terminal;
        this.background = new BasicWindow() {
            @Override
            protected void doDraw(Screen screen) {
                AttributedStyle st = getTheme().getStyle(".background");
                screen.fill(
                        getPosition().x(),
                        getPosition().y(),
                        getSize().w(),
                        getSize().h(),
                        st);
            }
        };
        this.background.setGUI(this);
        this.background.setBehaviors(EnumSet.of(Component.Behavior.NoDecoration, Component.Behavior.FullScreen));
    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }

    @Override
    public <C extends Component> Renderer getRenderer(Class<C> clazz) {
        return renderers.get(clazz);
    }

    @Override
    public <C extends Component> void setRenderer(Class<C> clazz, Renderer renderer) {
        this.renderers.put(clazz, renderer);
    }

    @Override
    public Theme getTheme() {
        return theme;
    }

    @Override
    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    @Override
    public void addWindow(Window window) {
        if (window.getGUI() != null) {
            window.getGUI().removeWindow(window);
        }
        windows.add(window);
        ((AbstractWindow) window).setGUI(this);
        if (!window.getBehaviors().contains(Window.Behavior.NoFocus)) {
            activeWindow = window;
        }
        // todo: refresh
    }

    @Override
    public void removeWindow(Window window) {
        if (windows.remove(window)) {
            ((AbstractWindow) window).setGUI(null);
            if (activeWindow == window) {
                activeWindow = null;
                for (Window w : windows) {
                    if (!w.getBehaviors().contains(Window.Behavior.NoFocus)) {
                        activeWindow = w; // no break, the last will be the one
                    }
                }
            }
            // todo: refresh
        }
    }

    @Override
    public void run() {
        BindingReader bindingReader = new BindingReader(terminal.reader());
        KeyMap<Event> map = new KeyMap<>();
        map.setNomatch(Event.Key);
        map.setUnicode(Event.Key);
        map.bind(Event.Mouse, KeyMap.key(terminal, InfoCmp.Capability.key_mouse));

        Attributes attributes = terminal.getAttributes();
        Attributes newAttr = new Attributes(attributes);
        newAttr.setLocalFlags(
                EnumSet.of(Attributes.LocalFlag.ICANON, Attributes.LocalFlag.ECHO, Attributes.LocalFlag.IEXTEN), false);
        newAttr.setInputFlags(
                EnumSet.of(Attributes.InputFlag.IXON, Attributes.InputFlag.ICRNL, Attributes.InputFlag.INLCR), false);
        newAttr.setControlChar(Attributes.ControlChar.VMIN, 0);
        newAttr.setControlChar(Attributes.ControlChar.VTIME, 1);
        newAttr.setControlChar(Attributes.ControlChar.VINTR, 0);
        terminal.setAttributes(newAttr);
        Terminal.SignalHandler prevHandler = terminal.handle(Terminal.Signal.WINCH, this::handle);
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        terminal.puts(InfoCmp.Capability.keypad_xmit);
        terminal.trackMouse(Terminal.MouseTracking.Button);
        terminal.puts(InfoCmp.Capability.cursor_invisible);
        display = new Display(terminal, true);

        try {
            onResize();
            while (!windows.isEmpty()) {
                Event event = bindingReader.readBinding(map);
                switch (event) {
                    case Key:
                        handleInput(bindingReader.getLastBinding());
                        break;
                    case Mouse:
                        handleMouse(terminal.readMouseEvent(bindingReader::readCharacter));
                        break;
                }
                redraw();
            }
            try {
                while (terminal.reader().read(1) > 0)
                    ;
            } catch (IOException e) {
                // ignore
            }
        } finally {
            terminal.puts(InfoCmp.Capability.cursor_visible);
            terminal.trackMouse(Terminal.MouseTracking.Off);
            terminal.puts(InfoCmp.Capability.exit_ca_mode);
            terminal.puts(InfoCmp.Capability.keypad_local);
            terminal.flush();
            terminal.setAttributes(attributes);
            terminal.handle(Terminal.Signal.WINCH, prevHandler);
        }
    }

    private void handle(Terminal.Signal signal) {
        if (signal == Terminal.Signal.WINCH) {
            onResize();
        }
    }

    private void onResize() {
        org.jline.terminal.Size sz = terminal.getSize();
        size = new Size(sz.getColumns(), sz.getRows());
        display.resize(sz.getRows(), sz.getColumns());
        background.setPosition(new Position(0, 0));
        background.setSize(size);
        for (Window window : windows) {
            if (!window.getBehaviors().contains(Component.Behavior.ManualLayout)) {
                window.setPosition(new Position(size.w() / 4, size.h() / 4));
                window.setSize(new Size(size.w() / 2, size.h() / 2));
            }
        }
        redraw();
    }

    enum Event {
        Key,
        Mouse
    }

    protected void handleInput(String input) {
        if (activeWindow != null) {
            activeWindow.handleInput(input);
        } else {
            background.handleInput(input);
        }
    }

    protected void handleMouse(MouseEvent event) {
        int x = event.getX();
        int y = event.getY();
        Window window = null;
        if (activeWindow != null && activeWindow.getBehaviors().contains(Component.Behavior.Popup)) {
            window = activeWindow;
        } else {
            for (Iterator<Window> it = windows.descendingIterator(); it.hasNext(); ) {
                Window w = it.next();
                if (w.isIn(x, y)) {
                    window = w;
                    break;
                }
            }
        }
        if (window == null) {
            window = background;
        }
        window.handleMouse(event);
    }

    protected void redraw() {
        VirtualScreen screen = new VirtualScreen(size.w(), size.h());
        background.draw(screen);
        windows.forEach(w -> w.draw(screen));
        display.update(screen.lines(), -1, true);
    }
}
