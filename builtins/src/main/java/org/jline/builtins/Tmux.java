/*
 * Copyright (c) 2002-2019, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.DateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.jline.builtins.Options.HelpException;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Attributes;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.jline.utils.*;
import org.jline.utils.InfoCmp.Capability;

import static org.jline.builtins.Tmux.Layout.Type.LeftRight;
import static org.jline.builtins.Tmux.Layout.Type.TopBottom;
import static org.jline.builtins.Tmux.Layout.Type.WindowPane;
import static org.jline.keymap.KeyMap.*;

/**
 * Terminal multiplexer
 */
public class Tmux {

    public static final String OPT_PREFIX = "prefix";

    public static final String CMD_COMMANDS = "commands";
    public static final String CMD_SEND_PREFIX = "send-prefix";
    public static final String CMD_SPLIT_WINDOW = "split-window";
    public static final String CMD_SPLITW = "splitw";
    public static final String CMD_SELECT_PANE = "select-pane";
    public static final String CMD_SELECTP = "selectp";
    public static final String CMD_RESIZE_PANE = "resize-pane";
    public static final String CMD_RESIZEP = "resizep";
    public static final String CMD_DISPLAY_PANES = "display-panes";
    public static final String CMD_DISPLAYP = "displayp";
    public static final String CMD_CLOCK_MODE = "clock-mode";
    public static final String CMD_SET_OPTION = "set-option";
    public static final String CMD_SET = "set";
    public static final String CMD_LIST_KEYS = "list-keys";
    public static final String CMD_LSK = "lsk";
    public static final String CMD_SEND_KEYS = "send-keys";
    public static final String CMD_SEND = "send";
    public static final String CMD_BIND_KEY = "bind-key";
    public static final String CMD_BIND = "bind";
    public static final String CMD_UNBIND_KEY = "unbind-key";
    public static final String CMD_UNBIND = "unbind";
    public static final String CMD_NEW_WINDOW = "new-window";
    public static final String CMD_NEWW = "neww";
    public static final String CMD_NEXT_WINDOW = "next-window";
    public static final String CMD_NEXT = "next";
    public static final String CMD_PREVIOUS_WINDOW = "previous-window";
    public static final String CMD_PREV = "prev";
    public static final String CMD_LIST_WINDOWS = "list-windows";
    public static final String CMD_LSW = "lsw";

    private static final int[][][] WINDOW_CLOCK_TABLE = {
        {
            {1, 1, 1, 1, 1}, /* 0 */
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        },
        {
            {0, 0, 0, 0, 1}, /* 1 */
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 2 */
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 3 */
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        },
        {
            {1, 0, 0, 0, 1}, /* 4 */
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 5 */
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 6 */
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 7 */
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 8 */
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* 9 */
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        },
        {
            {0, 0, 0, 0, 0}, /* : */
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0}
        },
        {
            {1, 1, 1, 1, 1}, /* A */
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1}
        },
        {
            {1, 1, 1, 1, 1}, /* P */
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0}
        },
        {
            {1, 0, 0, 0, 1}, /* M */
            {1, 1, 0, 1, 1},
            {1, 0, 1, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1}
        },
    };

    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private final AtomicBoolean resized = new AtomicBoolean(true);
    private final Terminal terminal;
    private final Display display;
    private final PrintStream err;
    private final String term;
    private final Consumer<Terminal> runner;
    private List<Window> windows = new ArrayList<>();
    private Integer windowsId = 0;
    private int activeWindow = 0;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Size size = new Size();
    private boolean identify;
    private ScheduledExecutorService executor;

    private ScheduledFuture<?> clockFuture;

    private final Map<String, String> serverOptions = new HashMap<>();

    private KeyMap<Object> keyMap;

    enum Binding {
        Discard,
        SelfInsert,
        Mouse
    }

    private class Window {
        private List<VirtualConsole> panes = new CopyOnWriteArrayList<>();
        private VirtualConsole active;
        private int lastActive;
        private final AtomicInteger paneId = new AtomicInteger();
        private Layout layout;
        private Tmux tmux;
        private String name;

        public Window(Tmux tmux) throws IOException {
            this.tmux = tmux;
            layout = new Layout();
            layout.sx = size.getColumns();
            layout.sy = size.getRows();
            layout.type = WindowPane;
            active = new VirtualConsole(
                    paneId.incrementAndGet(),
                    term,
                    0,
                    0,
                    size.getColumns(),
                    size.getRows() - 1,
                    tmux::setDirty,
                    tmux::close,
                    layout);
            active.active = lastActive++;
            active.getConsole().setAttributes(terminal.getAttributes());
            panes.add(active);
            name = "win" + (windowsId < 10 ? "0" + windowsId : windowsId);
            windowsId++;
        }

        public String getName() {
            return name;
        }

        public List<VirtualConsole> getPanes() {
            return panes;
        }

        public VirtualConsole getActive() {
            return active;
        }

        public void remove(VirtualConsole console) {
            panes.remove(console);
            if (!panes.isEmpty()) {
                console.layout.remove();
                if (active == console) {
                    active = panes.stream()
                            .sorted(Comparator.<VirtualConsole>comparingInt(p -> p.active)
                                    .reversed())
                            .findFirst()
                            .get();
                }
                layout = active.layout;
                while (layout.parent != null) {
                    layout = layout.parent;
                }
                layout.fixOffsets();
                layout.fixPanes(size.getColumns(), size.getRows());
            }
        }

        public void handleResize() {
            layout.resize(size.getColumns(), size.getRows() - 1);
            panes.forEach(vc -> {
                if (vc.width() != vc.layout.sx
                        || vc.height() != vc.layout.sy
                        || vc.left() != vc.layout.xoff
                        || vc.top() != vc.layout.yoff) {
                    vc.resize(vc.layout.xoff, vc.layout.yoff, vc.layout.sx, vc.layout.sy);
                    display.clear();
                }
            });
        }

        public VirtualConsole splitPane(Options opt) throws IOException {
            Layout.Type type = opt.isSet("horizontal") ? LeftRight : TopBottom;
            // If we're splitting the main pane, create a parent
            if (layout.type == WindowPane) {
                Layout p = new Layout();
                p.sx = layout.sx;
                p.sy = layout.sy;
                p.type = type;
                p.cells.add(layout);
                layout.parent = p;
                layout = p;
            }
            Layout cell = active.layout();
            if (opt.isSet("f")) {
                while (cell.parent != layout) {
                    cell = cell.parent;
                }
            }
            int size = -1;
            if (opt.isSet("size")) {
                size = opt.getNumber("size");
            } else if (opt.isSet("perc")) {
                int p = opt.getNumber("perc");
                if (type == TopBottom) {
                    size = (cell.sy * p) / 100;
                } else {
                    size = (cell.sx * p) / 100;
                }
            }
            // Split now
            Layout newCell = cell.split(type, size, opt.isSet("before"));
            if (newCell == null) {
                err.println("create pane failed: pane too small");
                return null;
            }

            VirtualConsole newConsole = new VirtualConsole(
                    paneId.incrementAndGet(),
                    term,
                    newCell.xoff,
                    newCell.yoff,
                    newCell.sx,
                    newCell.sy,
                    tmux::setDirty,
                    tmux::close,
                    newCell);
            panes.add(newConsole);
            newConsole.getConsole().setAttributes(terminal.getAttributes());
            if (!opt.isSet("d")) {
                active = newConsole;
                active.active = lastActive++;
            }
            return newConsole;
        }

        public boolean selectPane(Options opt) {
            VirtualConsole prevActive = active;
            if (opt.isSet("L")) {
                active = panes.stream()
                        .filter(c -> c.bottom() > active.top() && c.top() < active.bottom())
                        .filter(c -> c != active)
                        .sorted(Comparator.<VirtualConsole>comparingInt(
                                        c -> c.left() > active.left() ? c.left() : c.left() + size.getColumns())
                                .reversed()
                                .<VirtualConsole>thenComparingInt(c -> -c.active))
                        .findFirst()
                        .orElse(active);
            } else if (opt.isSet("R")) {
                active = panes.stream()
                        .filter(c -> c.bottom() > active.top() && c.top() < active.bottom())
                        .filter(c -> c != active)
                        .sorted(Comparator.<VirtualConsole>comparingInt(
                                        c -> c.left() > active.left() ? c.left() : c.left() + size.getColumns())
                                .<VirtualConsole>thenComparingInt(c -> -c.active))
                        .findFirst()
                        .orElse(active);
            } else if (opt.isSet("U")) {
                active = panes.stream()
                        .filter(c -> c.right() > active.left() && c.left() < active.right())
                        .filter(c -> c != active)
                        .sorted(Comparator.<VirtualConsole>comparingInt(
                                        c -> c.top() > active.top() ? c.top() : c.top() + size.getRows())
                                .reversed()
                                .<VirtualConsole>thenComparingInt(c -> -c.active))
                        .findFirst()
                        .orElse(active);
            } else if (opt.isSet("D")) {
                active = panes.stream()
                        .filter(c -> c.right() > active.left() && c.left() < active.right())
                        .filter(c -> c != active)
                        .sorted(Comparator.<VirtualConsole>comparingInt(
                                        c -> c.top() > active.top() ? c.top() : c.top() + size.getRows())
                                .<VirtualConsole>thenComparingInt(c -> -c.active))
                        .findFirst()
                        .orElse(active);
            }
            boolean out = false;
            if (prevActive != active) {
                active.active = lastActive++;
                out = true;
            }
            return out;
        }

        public void resizePane(Options opt, int adjust) {
            if (opt.isSet("width")) {
                int x = opt.getNumber("width");
                active.layout().resizeTo(LeftRight, x);
            }
            if (opt.isSet("height")) {
                int y = opt.getNumber("height");
                active.layout().resizeTo(TopBottom, y);
            }
            if (opt.isSet("L")) {
                active.layout().resize(LeftRight, -adjust, true);
            } else if (opt.isSet("R")) {
                active.layout().resize(LeftRight, adjust, true);
            } else if (opt.isSet("U")) {
                active.layout().resize(TopBottom, -adjust, true);
            } else if (opt.isSet("D")) {
                active.layout().resize(TopBottom, adjust, true);
            }
        }
    }

    public Tmux(Terminal terminal, PrintStream err, Consumer<Terminal> runner) throws IOException {
        this.terminal = terminal;
        this.err = err;
        this.runner = runner;
        display = new Display(terminal, true);
        // Find terminal to use
        Integer colors = terminal.getNumericCapability(Capability.max_colors);
        term = (colors != null && colors >= 256) ? "screen-256color" : "screen";
        // Setup defaults bindings
        serverOptions.put(OPT_PREFIX, "`");
        keyMap = createKeyMap(serverOptions.get(OPT_PREFIX));
    }

    protected KeyMap<Object> createKeyMap(String prefix) {
        KeyMap<Object> keyMap = createEmptyKeyMap(prefix);
        keyMap.bind(CMD_SEND_PREFIX, prefix + prefix);
        keyMap.bind(CMD_SPLIT_WINDOW + " -v", prefix + "\"");
        keyMap.bind(CMD_SPLIT_WINDOW + " -h", prefix + "%");
        keyMap.bind(CMD_SELECT_PANE + " -U", prefix + key(terminal, Capability.key_up));
        keyMap.bind(CMD_SELECT_PANE + " -D", prefix + key(terminal, Capability.key_down));
        keyMap.bind(CMD_SELECT_PANE + " -L", prefix + key(terminal, Capability.key_left));
        keyMap.bind(CMD_SELECT_PANE + " -R", prefix + key(terminal, Capability.key_right));
        keyMap.bind(CMD_RESIZE_PANE + " -U 5", prefix + esc() + key(terminal, Capability.key_up));
        keyMap.bind(CMD_RESIZE_PANE + " -D 5", prefix + esc() + key(terminal, Capability.key_down));
        keyMap.bind(CMD_RESIZE_PANE + " -L 5", prefix + esc() + key(terminal, Capability.key_left));
        keyMap.bind(CMD_RESIZE_PANE + " -R 5", prefix + esc() + key(terminal, Capability.key_right));
        keyMap.bind(CMD_RESIZE_PANE + " -U", prefix + translate("^[[1;5A"), prefix + alt(translate("^[[A"))); // ctrl-up
        keyMap.bind(
                CMD_RESIZE_PANE + " -D", prefix + translate("^[[1;5B"), prefix + alt(translate("^[[B"))); // ctrl-down
        keyMap.bind(
                CMD_RESIZE_PANE + " -L", prefix + translate("^[[1;5C"), prefix + alt(translate("^[[C"))); // ctrl-left
        keyMap.bind(
                CMD_RESIZE_PANE + " -R", prefix + translate("^[[1;5D"), prefix + alt(translate("^[[D"))); // ctrl-right
        keyMap.bind(CMD_DISPLAY_PANES, prefix + "q");
        keyMap.bind(CMD_CLOCK_MODE, prefix + "t");
        keyMap.bind(CMD_NEW_WINDOW, prefix + "c");
        keyMap.bind(CMD_NEXT_WINDOW, prefix + "n");
        keyMap.bind(CMD_PREVIOUS_WINDOW, prefix + "p");
        return keyMap;
    }

    protected KeyMap<Object> createEmptyKeyMap(String prefix) {
        KeyMap<Object> keyMap = new KeyMap<>();
        keyMap.setUnicode(Binding.SelfInsert);
        keyMap.setNomatch(Binding.SelfInsert);
        for (int i = 0; i < 255; i++) {
            keyMap.bind(Binding.Discard, prefix + (char) (i));
        }
        keyMap.bind(Binding.Mouse, key(terminal, Capability.key_mouse));
        return keyMap;
    }

    public void run() throws IOException {
        SignalHandler prevWinchHandler = terminal.handle(Signal.WINCH, this::resize);
        SignalHandler prevIntHandler = terminal.handle(Signal.INT, this::interrupt);
        SignalHandler prevSuspHandler = terminal.handle(Signal.TSTP, this::suspend);
        Attributes attributes = terminal.enterRawMode();
        terminal.puts(Capability.enter_ca_mode);
        terminal.puts(Capability.keypad_xmit);
        terminal.trackMouse(Terminal.MouseTracking.Any);
        terminal.flush();
        executor = Executors.newSingleThreadScheduledExecutor();
        try {
            // Create first pane
            size.copy(terminal.getSize());
            windows.add(new Window(this));
            activeWindow = 0;
            runner.accept(active().getConsole());
            // Start input loop
            new Thread(this::inputLoop, "Mux input loop").start();
            // Redraw loop
            redrawLoop();
        } catch (RuntimeException e) {
            throw e;
        } finally {
            executor.shutdown();
            terminal.trackMouse(Terminal.MouseTracking.Off);
            terminal.puts(Capability.keypad_local);
            terminal.puts(Capability.exit_ca_mode);
            terminal.flush();
            terminal.setAttributes(attributes);
            terminal.handle(Signal.WINCH, prevWinchHandler);
            terminal.handle(Signal.INT, prevIntHandler);
            terminal.handle(Signal.TSTP, prevSuspHandler);
        }
    }

    private VirtualConsole active() {
        return windows.get(activeWindow).getActive();
    }

    private List<VirtualConsole> panes() {
        return windows.get(activeWindow).getPanes();
    }

    private Window window() {
        return windows.get(activeWindow);
    }

    private void redrawLoop() {
        while (running.get()) {
            try {
                synchronized (dirty) {
                    while (running.get() && !dirty.compareAndSet(true, false)) {
                        dirty.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handleResize();
            redraw();
        }
    }

    private void setDirty() {
        synchronized (dirty) {
            dirty.set(true);
            dirty.notifyAll();
        }
    }

    private void inputLoop() {
        try {
            BindingReader reader = new BindingReader(terminal.reader());
            boolean first = true;
            while (running.get()) {
                Object b;
                if (first) {
                    b = reader.readBinding(keyMap);
                } else if (reader.peekCharacter(100) >= 0) {
                    b = reader.readBinding(keyMap, null, false);
                } else {
                    b = null;
                }
                if (b == Binding.SelfInsert) {
                    if (active().clock) {
                        active().clock = false;
                        if (clockFuture != null && panes().stream().noneMatch(vc -> vc.clock)) {
                            clockFuture.cancel(false);
                            clockFuture = null;
                        }
                        setDirty();
                    } else {
                        active().getMasterInputOutput()
                                .write(reader.getLastBinding().getBytes());
                        first = false;
                    }
                } else {
                    if (first) {
                        first = false;
                    } else {
                        active().getMasterInputOutput().flush();
                        first = true;
                    }
                    if (b == Binding.Mouse) {
                        MouseEvent event = terminal.readMouseEvent();
                        // System.err.println(event.toString());
                    } else if (b instanceof String || b instanceof String[]) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ByteArrayOutputStream err = new ByteArrayOutputStream();
                        try (PrintStream pout = new PrintStream(out);
                                PrintStream perr = new PrintStream(err)) {
                            if (b instanceof String) {
                                execute(pout, perr, (String) b);
                            } else {
                                execute(pout, perr, Arrays.asList((String[]) b));
                            }
                        } catch (Exception e) {
                            // TODO: log
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                Log.info("Error in tmux input loop", e);
            }
        } finally {
            running.set(false);
            setDirty();
        }
    }

    private synchronized void close(VirtualConsole terminal) {
        int idx = -1;
        Window window = null;
        for (Window w : windows) {
            idx = w.getPanes().indexOf(terminal);
            if (idx >= 0) {
                window = w;
                break;
            }
        }
        if (idx >= 0) {
            window.remove(terminal);
            if (window.getPanes().isEmpty()) {
                if (windows.size() > 1) {
                    windows.remove(window);
                    if (activeWindow >= windows.size()) {
                        activeWindow--;
                    }
                    resize(Signal.WINCH);
                } else {
                    running.set(false);
                    setDirty();
                }
            } else {
                resize(Signal.WINCH);
            }
        }
    }

    private void resize(Signal signal) {
        resized.set(true);
        setDirty();
    }

    private void interrupt(Signal signal) {
        active().getConsole().raise(signal);
    }

    private void suspend(Signal signal) {
        active().getConsole().raise(signal);
    }

    private void handleResize() {
        // Re-compute the layout
        if (resized.compareAndSet(true, false)) {
            size.copy(terminal.getSize());
        }
        window().handleResize();
    }

    public void execute(PrintStream out, PrintStream err, String command) throws Exception {
        ParsedLine line = new DefaultParser().parse(command.trim(), 0);
        execute(out, err, line.words());
    }

    public synchronized void execute(PrintStream out, PrintStream err, List<String> command) throws Exception {
        String name = command.get(0);
        List<String> args = command.subList(1, command.size());
        switch (name) {
            case CMD_SEND_PREFIX:
                sendPrefix(out, err, args);
                break;
            case CMD_SPLIT_WINDOW:
            case CMD_SPLITW:
                splitWindow(out, err, args);
                break;
            case CMD_SELECT_PANE:
            case CMD_SELECTP:
                selectPane(out, err, args);
                break;
            case CMD_RESIZE_PANE:
            case CMD_RESIZEP:
                resizePane(out, err, args);
                break;
            case CMD_DISPLAY_PANES:
            case CMD_DISPLAYP:
                displayPanes(out, err, args);
                break;
            case CMD_CLOCK_MODE:
                clockMode(out, err, args);
                break;
            case CMD_BIND_KEY:
            case CMD_BIND:
                bindKey(out, err, args);
                break;
            case CMD_UNBIND_KEY:
            case CMD_UNBIND:
                unbindKey(out, err, args);
                break;
            case CMD_LIST_KEYS:
            case CMD_LSK:
                listKeys(out, err, args);
                break;
            case CMD_SEND_KEYS:
            case CMD_SEND:
                sendKeys(out, err, args);
                break;
            case CMD_SET_OPTION:
            case CMD_SET:
                setOption(out, err, args);
                break;
            case CMD_NEW_WINDOW:
            case CMD_NEWW:
                newWindow(out, err, args);
                break;
            case CMD_NEXT_WINDOW:
            case CMD_NEXT:
                nextWindow(out, err, args);
                break;
            case CMD_PREVIOUS_WINDOW:
            case CMD_PREV:
                previousWindow(out, err, args);
                break;
            case CMD_LIST_WINDOWS:
            case CMD_LSW:
                listWindows(out, err, args);
                break;
        }
    }

    protected void listWindows(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {"list-windows - ", "Usage: list-windows", "  -? --help                    Show help"};
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        IntStream.range(0, windows.size())
                .mapToObj(i -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(i);
                    sb.append(": ");
                    sb.append(windows.get(i).getName());
                    sb.append(i == activeWindow ? "* " : " ");
                    sb.append("(");
                    sb.append(windows.get(i).getPanes().size());
                    sb.append(" panes)");
                    if (i == activeWindow) {
                        sb.append(" (active)");
                    }
                    return sb.toString();
                })
                .sorted()
                .forEach(out::println);
    }

    protected void previousWindow(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "previous-window - ", "Usage: previous-window", "  -? --help                    Show help"
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (windows.size() > 1) {
            activeWindow--;
            if (activeWindow < 0) {
                activeWindow = windows.size() - 1;
            }
            setDirty();
        }
    }

    protected void nextWindow(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {"next-window - ", "Usage: next-window", "  -? --help                    Show help"};
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (windows.size() > 1) {
            activeWindow++;
            if (activeWindow >= windows.size()) {
                activeWindow = 0;
            }
            setDirty();
        }
    }

    protected void newWindow(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {"new-window - ", "Usage: new-window", "  -? --help                    Show help"};
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        windows.add(new Window(this));
        activeWindow = windows.size() - 1;
        runner.accept(active().getConsole());
        setDirty();
    }

    protected void setOption(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "set-option - ",
            "Usage: set-option [-agosquw] option [value]",
            "  -? --help                    Show help",
            "  -u --unset                   Unset the option"
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        int nbargs = opt.args().size();
        if (nbargs < 1 || nbargs > 2) {
            throw new HelpException(opt.usage());
        }
        String name = opt.args().get(0);
        String value = nbargs > 1 ? opt.args().get(1) : null;
        if (name.startsWith("@")) {
            // set user option
        } else {
            // set server option
            switch (name) {
                case OPT_PREFIX:
                    if (value == null) {
                        throw new IllegalArgumentException("Missing argument");
                    }
                    String prefix = translate(value);
                    String oldPrefix = serverOptions.put(OPT_PREFIX, prefix);
                    KeyMap<Object> newKeys = createEmptyKeyMap(prefix);
                    for (Map.Entry<String, Object> e : keyMap.getBoundKeys().entrySet()) {
                        if (e.getValue() instanceof String) {
                            if (e.getKey().equals(oldPrefix + oldPrefix)) {
                                newKeys.bind(e.getValue(), prefix + prefix);
                            } else if (e.getKey().startsWith(oldPrefix)) {
                                newKeys.bind(e.getValue(), prefix + e.getKey().substring(oldPrefix.length()));
                            } else {
                                newKeys.bind(e.getValue(), e.getKey());
                            }
                        }
                    }
                    keyMap = newKeys;
                    break;
            }
        }
    }

    protected void bindKey(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "bind-key - ",
            "Usage: bind-key key command [arguments]", /* [-cnr] [-t mode-table] [-T key-table] */
            "  -? --help                    Show help"
        };
        Options opt = Options.compile(usage).setOptionsFirst(true).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        List<String> vargs = opt.args();
        if (vargs.size() < 2) {
            throw new HelpException(opt.usage());
        }
        String prefix = serverOptions.get(OPT_PREFIX);
        String key = prefix + KeyMap.translate(vargs.remove(0));
        keyMap.unbind(key.substring(0, 2));
        keyMap.bind(vargs.toArray(new String[vargs.size()]), key);
    }

    protected void unbindKey(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "unbind-key - ",
            "Usage: unbind-key key", /* [-an] [-t mode-table] [-T key-table] */
            "  -? --help                    Show help"
        };
        Options opt = Options.compile(usage).setOptionsFirst(true).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        List<String> vargs = opt.args();
        if (vargs.size() != 1) {
            throw new HelpException(opt.usage());
        }
        String prefix = serverOptions.get(OPT_PREFIX);
        String key = prefix + KeyMap.translate(vargs.remove(0));
        keyMap.unbind(key);
        keyMap.bind(Binding.Discard, key);
    }

    protected void listKeys(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "list-keys - ",
            "Usage: list-keys ", /* [-t mode-table] [-T key-table] */
            "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        String prefix = serverOptions.get(OPT_PREFIX);
        keyMap.getBoundKeys().entrySet().stream()
                .filter(e -> e.getValue() instanceof String)
                .map(e -> {
                    String key = e.getKey();
                    String val = (String) e.getValue();
                    StringBuilder sb = new StringBuilder();
                    sb.append("bind-key -T ");
                    if (key.startsWith(prefix)) {
                        sb.append("prefix ");
                        key = key.substring(prefix.length());
                    } else {
                        sb.append("root   ");
                    }
                    sb.append(display(key));
                    while (sb.length() < 32) {
                        sb.append(" ");
                    }
                    sb.append(val);
                    return sb.toString();
                })
                .sorted()
                .forEach(out::println);
    }

    protected void sendKeys(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "send-keys - ",
            "Usage: send-keys [-lXRM] [-N repeat-count] [-t target-pane] key...",
            "  -? --help                    Show help",
            "  -l --literal                Send key literally",
            "  -N --number=repeat-count     Specifies a repeat count"
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        for (int i = 0, n = opt.getNumber("number"); i < n; i++) {
            for (String arg : opt.args()) {
                String s = opt.isSet("literal") ? arg : KeyMap.translate(arg);
                active().getMasterInputOutput().write(s.getBytes());
            }
        }
    }

    protected void clockMode(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {"clock-mode - ", "Usage: clock-mode", "  -? --help                    Show help"};
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        active().clock = true;

        if (clockFuture == null) {
            long initial = Instant.now()
                    .until(Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60), ChronoUnit.MILLIS);
            long delay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
            clockFuture = executor.scheduleWithFixedDelay(this::setDirty, initial, delay, TimeUnit.MILLISECONDS);
        }
        setDirty();
    }

    protected void displayPanes(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {"display-panes - ", "Usage: display-panes", "  -? --help                    Show help"};
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        identify = true;
        setDirty();
        executor.schedule(
                () -> {
                    identify = false;
                    setDirty();
                },
                1,
                TimeUnit.SECONDS);
    }

    protected void resizePane(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "resize-pane - ",
            "Usage: resize-pane [-UDLR] [-x width] [-y height] [-t target-pane] [adjustment]",
            "  -? --help                    Show help",
            "  -U                           Resize pane upward",
            "  -D                           Select pane downward",
            "  -L                           Select pane to the left",
            "  -R                           Select pane to the right",
            "  -x --width=width             Set the width of the pane",
            "  -y --height=height           Set the height of the pane"
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        int adjust;
        if (opt.args().size() == 0) {
            adjust = 1;
        } else if (opt.args().size() == 1) {
            adjust = Integer.parseInt(opt.args().get(0));
        } else {
            throw new HelpException(opt.usage());
        }
        window().resizePane(opt, adjust);
        setDirty();
    }

    protected void selectPane(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "select-pane - ",
            "Usage: select-pane [-UDLR] [-t target-pane]",
            "  -? --help                    Show help",
            "  -U                           Select pane up",
            "  -D                           Select pane down",
            "  -L                           Select pane left",
            "  -R                           Select pane right",
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        if (window().selectPane(opt)) {
            setDirty();
        }
    }

    protected void sendPrefix(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "send-prefix - ", "Usage: send-prefix [-2] [-t target-pane]", "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        active().getMasterInputOutput().write(serverOptions.get(OPT_PREFIX).getBytes());
    }

    protected void splitWindow(PrintStream out, PrintStream err, List<String> args) throws Exception {
        final String[] usage = {
            "split-window - ",
            "Usage: split-window [-bdfhvP] [-c start-directory] [-F format] [-p percentage|-l size] [-t target-pane] [command]",
            "  -? --help                    Show help",
            "  -h --horizontal              Horizontal split",
            "  -v --vertical                Vertical split",
            "  -l --size=size               Size",
            "  -p --perc=percentage         Percentage",
            "  -b --before                  Insert the new pane before the active one",
            "  -f                           Split the full window instead of the active pane",
            "  -d                           Do not make the new pane the active one"
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        VirtualConsole newConsole = window().splitPane(opt);
        runner.accept(newConsole.getConsole());
        setDirty();
    }

    protected void layoutResize() {
        // See layout_resize
    }

    int ACTIVE_COLOR = 0xF44;
    int INACTIVE_COLOR = 0x44F;
    int CLOCK_COLOR = 0x44F;

    protected synchronized void redraw() {
        long[] screen = new long[size.getRows() * size.getColumns()];
        // Fill
        Arrays.fill(screen, 0x00000020L);
        int[] cursor = new int[2];
        for (VirtualConsole terminal : panes()) {
            if (terminal.clock) {
                String str = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
                print(screen, terminal, str, CLOCK_COLOR);
            } else {
                // Dump terminal
                terminal.dump(
                        screen,
                        terminal.top(),
                        terminal.left(),
                        size.getRows(),
                        size.getColumns(),
                        terminal == active() ? cursor : null);
            }

            if (identify) {
                String id = Integer.toString(terminal.id);
                print(screen, terminal, id, terminal == active() ? ACTIVE_COLOR : INACTIVE_COLOR);
            }
            // Draw border
            drawBorder(screen, size, terminal, 0x0L);
        }
        drawBorder(screen, size, active(), 0x010080000L << 32);
        // Draw status
        Arrays.fill(
                screen,
                (size.getRows() - 1) * size.getColumns(),
                size.getRows() * size.getColumns(),
                0x20000080L << 32 | 0x0020L);

        // Attribute mask: 0xYXFFFBBB00000000L
        //  X:  Bit 0 - Underlined
        //      Bit 1 - Negative
        //      Bit 2 - Concealed
        //      Bit 3 - Bold
        //  Y:  Bit 0 - Foreground set
        //      Bit 1 - Background set
        //  F:  Foreground r-g-b
        //  B:  Background r-g-b

        List<AttributedString> lines = new ArrayList<>();
        int prevBg = 0;
        int prevFg = 0;
        boolean prevInv = false;
        boolean prevUl = false;
        boolean prevBold = false;
        boolean prevConceal = false;
        boolean prevHasFg = false;
        boolean prevHasBg = false;
        for (int y = 0; y < size.getRows(); y++) {
            AttributedStringBuilder sb = new AttributedStringBuilder(size.getColumns());
            for (int x = 0; x < size.getColumns(); x++) {
                long d = screen[y * size.getColumns() + x];
                int c = (int) (d & 0xffffffffL);
                int a = (int) (d >> 32);
                int bg = a & 0x000fff;
                int fg = (a & 0xfff000) >> 12;
                boolean ul = ((a & 0x01000000) != 0);
                boolean inv = ((a & 0x02000000) != 0);
                boolean conceal = ((a & 0x04000000) != 0);
                boolean bold = ((a & 0x08000000) != 0);
                boolean hasFg = ((a & 0x10000000) != 0);
                boolean hasBg = ((a & 0x20000000) != 0);

                if ((hasBg && prevHasBg && bg != prevBg) || prevHasBg != hasBg) {
                    if (!hasBg) {
                        sb.style(sb.style().backgroundDefault());
                    } else {
                        int col = bg;
                        col = Colors.roundRgbColor((col & 0xF00) >> 4, (col & 0x0F0), (col & 0x00F) << 4, 256);
                        sb.style(sb.style().background(col));
                    }
                    prevBg = bg;
                    prevHasBg = hasBg;
                }
                if ((hasFg && prevHasFg && fg != prevFg) || prevHasFg != hasFg) {
                    if (!hasFg) {
                        sb.style(sb.style().foregroundDefault());
                    } else {
                        int col = fg;
                        col = Colors.roundRgbColor((col & 0xF00) >> 4, (col & 0x0F0), (col & 0x00F) << 4, 256);
                        sb.style(sb.style().foreground(col));
                    }
                    prevFg = fg;
                    prevHasFg = hasFg;
                }
                if (conceal != prevConceal) {
                    sb.style(conceal ? sb.style().conceal() : sb.style().concealOff());
                    prevConceal = conceal;
                }
                if (inv != prevInv) {
                    sb.style(inv ? sb.style().inverse() : sb.style().inverseOff());
                    prevInv = inv;
                }
                if (ul != prevUl) {
                    sb.style(ul ? sb.style().underline() : sb.style().underlineOff());
                    prevUl = ul;
                }
                if (bold != prevBold) {
                    sb.style(bold ? sb.style().bold() : sb.style().boldOff());
                    prevBold = bold;
                }
                sb.append((char) c);
            }
            lines.add(sb.toAttributedString());
        }
        display.resize(size.getRows(), size.getColumns());
        display.update(lines, size.cursorPos(cursor[1], cursor[0]));
    }

    private void print(long[] screen, VirtualConsole terminal, String id, int color) {
        if (terminal.height() > 5) {
            long attr = ((long) color << 32) | 0x02000000000000000L;
            int yoff = (terminal.height() - 5) / 2;
            int xoff = (terminal.width() - id.length() * 6) / 2;
            for (int i = 0; i < id.length(); i++) {
                char ch = id.charAt(i);
                int idx;
                switch (ch) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        idx = ch - '0';
                        break;
                    case ':':
                        idx = 10;
                        break;
                    case 'A':
                        idx = 11;
                        break;
                    case 'P':
                        idx = 12;
                        break;
                    case 'M':
                        idx = 13;
                        break;
                    default:
                        idx = -1;
                        break;
                }
                if (idx >= 0) {
                    int[][] data = WINDOW_CLOCK_TABLE[idx];
                    for (int y = 0; y < data.length; y++) {
                        for (int x = 0; x < data[y].length; x++) {
                            if (data[y][x] != 0) {
                                int off = (terminal.top + yoff + y) * size.getColumns()
                                        + terminal.left()
                                        + xoff
                                        + x
                                        + 6 * i;
                                screen[off] = attr | ' ';
                            }
                        }
                    }
                }
            }
        } else {
            long attr = ((long) color << 44) | 0x01000000000000000L;
            int yoff = (terminal.height() + 1) / 2;
            int xoff = (terminal.width() - id.length()) / 2;
            int off = (terminal.top + yoff) * size.getColumns() + terminal.left() + xoff;
            for (int i = 0; i < id.length(); i++) {
                screen[off + i] = attr | id.charAt(i);
            }
        }
    }

    private void drawBorder(long[] screen, Size size, VirtualConsole terminal, long attr) {
        for (int i = terminal.left(); i < terminal.right(); i++) {
            int y0 = terminal.top() - 1;
            int y1 = terminal.bottom();
            drawBorderChar(screen, size, i, y0, attr, '─');
            drawBorderChar(screen, size, i, y1, attr, '─');
        }
        for (int i = terminal.top(); i < terminal.bottom(); i++) {
            int x0 = terminal.left() - 1;
            int x1 = terminal.right();
            drawBorderChar(screen, size, x0, i, attr, '│');
            drawBorderChar(screen, size, x1, i, attr, '│');
        }
        drawBorderChar(screen, size, terminal.left() - 1, terminal.top() - 1, attr, '┌');
        drawBorderChar(screen, size, terminal.right(), terminal.top() - 1, attr, '┐');
        drawBorderChar(screen, size, terminal.left() - 1, terminal.bottom(), attr, '└');
        drawBorderChar(screen, size, terminal.right(), terminal.bottom(), attr, '┘');
    }

    private void drawBorderChar(long[] screen, Size size, int x, int y, long attr, int c) {
        if (x >= 0 && x < size.getColumns() && y >= 0 && y < size.getRows() - 1) {
            int oldc = (int) (screen[y * size.getColumns() + x] & 0xFFFFFFFFL);
            c = addBorder(c, oldc);
            screen[y * size.getColumns() + x] = attr | c;
        }
    }

    private int addBorder(int c, int oldc) {
        if (oldc == ' ') {
            return c;
        }
        if (oldc == '┼') {
            return '┼';
        }
        switch (c) {
            case '│':
                return addBorder('╷', addBorder('╵', oldc));
            case '─':
                return addBorder('╴', addBorder('╶', oldc));
            case '┌':
                return addBorder('╶', addBorder('╷', oldc));
            case '┐':
                return addBorder('╴', addBorder('╷', oldc));
            case '└':
                return addBorder('╶', addBorder('╵', oldc));
            case '┘':
                return addBorder('╴', addBorder('╵', oldc));
            case '├':
                return addBorder('╶', addBorder('│', oldc));
            case '┤':
                return addBorder('╴', addBorder('│', oldc));
            case '┬':
                return addBorder('╷', addBorder('─', oldc));
            case '┴':
                return addBorder('╵', addBorder('─', oldc));
            case '╴':
                switch (oldc) {
                    case '│':
                        return '┤';
                    case '─':
                        return '─';
                    case '┌':
                        return '┬';
                    case '┐':
                        return '┐';
                    case '└':
                        return '┴';
                    case '┘':
                        return '┘';
                    case '├':
                        return '┼';
                    case '┤':
                        return '┤';
                    case '┬':
                        return '┬';
                    case '┴':
                        return '┴';
                    default:
                        throw new IllegalArgumentException();
                }
            case '╵':
                switch (oldc) {
                    case '│':
                        return '│';
                    case '─':
                        return '┴';
                    case '┌':
                        return '├';
                    case '┐':
                        return '┤';
                    case '└':
                        return '└';
                    case '┘':
                        return '┘';
                    case '├':
                        return '├';
                    case '┤':
                        return '┤';
                    case '┬':
                        return '┼';
                    case '┴':
                        return '┴';
                    default:
                        throw new IllegalArgumentException();
                }
            case '╶':
                switch (oldc) {
                    case '│':
                        return '├';
                    case '─':
                        return '─';
                    case '┌':
                        return '┌';
                    case '┐':
                        return '┬';
                    case '└':
                        return '└';
                    case '┘':
                        return '┴';
                    case '├':
                        return '├';
                    case '┤':
                        return '┼';
                    case '┬':
                        return '┬';
                    case '┴':
                        return '┴';
                    default:
                        throw new IllegalArgumentException();
                }
            case '╷':
                switch (oldc) {
                    case '│':
                        return '│';
                    case '─':
                        return '┬';
                    case '┌':
                        return '┌';
                    case '┐':
                        return '┐';
                    case '└':
                        return '├';
                    case '┘':
                        return '┤';
                    case '├':
                        return '├';
                    case '┤':
                        return '┤';
                    case '┬':
                        return '┬';
                    case '┴':
                        return '┼';
                    default:
                        throw new IllegalArgumentException();
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    static class Layout {

        static final Pattern PATTERN = Pattern.compile("([0-9]+)x([0-9]+),([0-9]+),([0-9]+)([^0-9]\\S*)?");
        private static final int PANE_MINIMUM = 3;

        enum Type {
            LeftRight,
            TopBottom,
            WindowPane
        }

        Type type;
        Layout parent;
        int sx;
        int sy;
        int xoff;
        int yoff;
        List<Layout> cells = new CopyOnWriteArrayList<>();

        public static Layout parse(String layout) {
            if (layout.length() < 6) {
                throw new IllegalArgumentException("Bad syntax");
            }
            String chk = layout.substring(0, 4);
            if (layout.charAt(4) != ',') {
                throw new IllegalArgumentException("Bad syntax");
            }
            layout = layout.substring(5);
            if (Integer.parseInt(chk, 16) != checksum(layout)) {
                throw new IllegalArgumentException("Bad checksum");
            }
            return parseCell(null, layout);
        }

        public String dump() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("0000,");
            doDump(sb);
            int chk = checksum(sb, 5);
            sb.setCharAt(0, toHexChar((chk >> 12) & 0x000F));
            sb.setCharAt(1, toHexChar((chk >> 8) & 0x000F));
            sb.setCharAt(2, toHexChar((chk >> 4) & 0x000F));
            sb.setCharAt(3, toHexChar(chk & 0x000F));
            return sb.toString();
        }

        private static char toHexChar(int i) {
            return (i < 10) ? (char) (i + '0') : (char) (i - 10 + 'a');
        }

        private void doDump(StringBuilder sb) {
            sb.append(sx)
                    .append('x')
                    .append(sy)
                    .append(',')
                    .append(xoff)
                    .append(',')
                    .append(yoff);
            switch (type) {
                case WindowPane:
                    sb.append(',').append('0');
                    break;
                case TopBottom:
                case LeftRight:
                    sb.append(type == Type.TopBottom ? '[' : '{');
                    boolean first = true;
                    for (Layout c : cells) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(',');
                        }
                        c.doDump(sb);
                    }
                    sb.append(type == Type.TopBottom ? ']' : '}');
                    break;
            }
        }

        public void resize(Type type, int change, boolean opposite) {
            /* Find next parent of the same type. */
            Layout lc = this;
            Layout lcparent = lc.parent;
            while (lcparent != null && lcparent.type != type) {
                lc = lcparent;
                lcparent = lc.parent;
            }
            if (lcparent == null) {
                return;
            }
            /* If this is the last cell, move back one. */
            if (lc.nextSibling() == null) {
                lc = lc.prevSibling();
            }
            /* Grow or shrink the cell. */
            int size;
            int needed = change;
            while (needed != 0) {
                if (change > 0) {
                    size = lc.resizePaneGrow(type, needed, opposite);
                    needed -= size;
                } else {
                    size = lc.resizePaneShrink(type, needed);
                    needed += size;
                }
                if (size == 0) {
                    /* no more change possible */
                    break;
                }
            }
            fixOffsets();
            fixPanes();
        }

        int resizePaneGrow(Type type, int needed, boolean opposite) {
            int size = 0;
            /* Growing. Always add to the current cell. */
            Layout lcadd = this;
            /* Look towards the tail for a suitable cell for reduction. */
            Layout lcremove = this.nextSibling();
            while (lcremove != null) {
                size = lcremove.resizeCheck(type);
                if (size > 0) {
                    break;
                }
                lcremove = lcremove.nextSibling();
            }
            /* If none found, look towards the head. */
            if (opposite && lcremove == null) {
                lcremove = this.prevSibling();
                while (lcremove != null) {
                    size = lcremove.resizeCheck(type);
                    if (size > 0) {
                        break;
                    }
                    lcremove = lcremove.prevSibling();
                }
            }
            if (lcremove == null) {
                return 0;
            }
            /* Change the cells. */
            if (size > needed) {
                size = needed;
            }
            lcadd.resizeAdjust(type, size);
            lcremove.resizeAdjust(type, -size);
            return size;
        }

        int resizePaneShrink(Type type, int needed) {
            int size = 0;
            /* Shrinking. Find cell to remove from by walking towards head. */
            Layout lcremove = this;
            do {
                size = lcremove.resizeCheck(type);
                if (size > 0) {
                    break;
                }
                lcremove = lcremove.prevSibling();
            } while (lcremove != null);
            if (lcremove == null) {
                return 0;
            }
            /* And add onto the next cell (from the original cell). */
            Layout lcadd = this.nextSibling();
            if (lcadd == null) {
                return 0;
            }
            /* Change the cells. */
            if (size > -needed) {
                size = -needed;
            }
            lcadd.resizeAdjust(type, size);
            lcremove.resizeAdjust(type, -size);
            return size;
        }

        Layout prevSibling() {
            int idx = parent.cells.indexOf(this);
            if (idx > 0) {
                return parent.cells.get(idx - 1);
            } else {
                return null;
            }
        }

        Layout nextSibling() {
            int idx = parent.cells.indexOf(this);
            if (idx < parent.cells.size() - 1) {
                return parent.cells.get(idx + 1);
            } else {
                return null;
            }
        }

        public void resizeTo(Type type, int new_size) {
            /* Find next parent of the same type. */
            Layout lc = this;
            Layout lcparent = lc.parent;
            while (lcparent != null && lcparent.type != type) {
                lc = lcparent;
                lcparent = lc.parent;
            }
            if (lcparent == null) {
                return;
            }
            /* Work out the size adjustment. */
            int size = type == LeftRight ? lc.sx : lc.sy;
            int change = lc.nextSibling() == null ? size - new_size : new_size - size;
            /* Resize the pane. */
            lc.resize(type, change, true);
        }

        public void resize(int sx, int sy) {
            // Horizontal
            int xchange = sx - this.sx;
            int xlimit = resizeCheck(LeftRight);
            if (xchange < 0 && xchange < -xlimit) {
                xchange = -xlimit;
            }
            if (xlimit == 0) {
                if (sx <= this.sx) {
                    xchange = 0;
                } else {
                    xchange = sx - this.sx;
                }
            }
            if (xchange != 0) {
                resizeAdjust(LeftRight, xchange);
            }

            // Horizontal
            int ychange = sy - this.sy;
            int ylimit = resizeCheck(Type.TopBottom);
            if (ychange < 0 && ychange < -ylimit) {
                ychange = -ylimit;
            }
            if (ylimit == 0) {
                if (sy <= this.sy) {
                    ychange = 0;
                } else {
                    ychange = sy - this.sy;
                }
            }
            if (ychange != 0) {
                resizeAdjust(Type.TopBottom, ychange);
            }

            // Fix offsets
            fixOffsets();
            fixPanes(sx, sy);
        }

        public void remove() {
            if (parent == null) {
                throw new IllegalStateException();
            }
            int idx = parent.cells.indexOf(this);
            Layout other = parent.cells.get(idx == 0 ? 1 : idx - 1);
            other.resizeAdjust(parent.type, parent.type == LeftRight ? (sx + 1) : (sy + 1));
            parent.cells.remove(this);
            if (other.parent.cells.size() == 1) {
                if (other.parent.parent == null) {
                    other.parent = null;
                } else {
                    other.parent.parent.cells.set(other.parent.parent.cells.indexOf(other.parent), other);
                    other.parent = other.parent.parent;
                }
            }
        }

        private int resizeCheck(Type type) {
            if (this.type == Type.WindowPane) {
                int min = PANE_MINIMUM;
                int avail;
                if (type == LeftRight) {
                    avail = this.sx;
                } else {
                    avail = this.sy;
                    min += 1; // TODO: need status
                }
                if (avail > min) {
                    avail -= min;
                } else {
                    avail = 0;
                }
                return avail;
            } else if (this.type == type) {
                return this.cells.stream()
                        .mapToInt(c -> c != null ? c.resizeCheck(type) : 0)
                        .sum();
            } else {
                return this.cells.stream()
                        .mapToInt(c -> c != null ? c.resizeCheck(type) : Integer.MAX_VALUE)
                        .min()
                        .orElse(Integer.MAX_VALUE);
            }
        }

        private void resizeAdjust(Type type, int change) {
            if (type == LeftRight) {
                this.sx += change;
            } else {
                this.sy += change;
            }
            if (this.type == Type.WindowPane) {
                return;
            }
            if (this.type != type) {
                for (Layout c : cells) {
                    c.resizeAdjust(type, change);
                }
                return;
            }
            while (change != 0) {
                for (Layout c : cells) {
                    if (change == 0) {
                        break;
                    }
                    if (change > 0) {
                        c.resizeAdjust(type, 1);
                        change--;
                        continue;
                    }
                    if (c.resizeCheck(type) > 0) {
                        c.resizeAdjust(type, -1);
                        change++;
                    }
                }
                ;
            }
        }

        public void fixOffsets() {
            if (type == LeftRight) {
                int xoff = this.xoff;
                for (Layout cell : cells) {
                    cell.xoff = xoff;
                    cell.yoff = this.yoff;
                    cell.fixOffsets();
                    xoff += cell.sx + 1;
                }
            } else if (type == TopBottom) {
                int yoff = this.yoff;
                for (Layout cell : cells) {
                    cell.xoff = this.xoff;
                    cell.yoff = yoff;
                    cell.fixOffsets();
                    yoff += cell.sy + 1;
                }
            }
        }

        public void fixPanes() {}

        public void fixPanes(int sx, int sy) {}

        public int countCells() {
            switch (type) {
                case LeftRight:
                case TopBottom:
                    return cells.stream().mapToInt(Layout::countCells).sum();
                default:
                    return 1;
            }
        }

        public Layout split(Type type, int size, boolean insertBefore) {
            if (type == WindowPane) {
                throw new IllegalStateException();
            }
            if ((type == LeftRight ? sx : sy) < PANE_MINIMUM * 2 + 1) {
                return null;
            }
            if (parent == null) {
                throw new IllegalStateException();
            }

            int saved_size = type == LeftRight ? sx : sy;
            int size2 = size < 0 ? ((saved_size + 1) / 2) - 1 : insertBefore ? saved_size - size - 1 : size;
            if (size2 < PANE_MINIMUM) {
                size2 = PANE_MINIMUM;
            } else if (size2 > saved_size - 2) {
                size2 = saved_size - 2;
            }
            int size1 = saved_size - 1 - size2;

            if (parent.type != type) {
                Layout p = new Layout();
                p.type = type;
                p.parent = parent;
                p.sx = sx;
                p.sy = sy;
                p.xoff = xoff;
                p.yoff = yoff;
                parent.cells.set(parent.cells.indexOf(this), p);
                p.cells.add(this);
                parent = p;
            }
            Layout cell = new Layout();
            cell.type = WindowPane;
            cell.parent = parent;
            parent.cells.add(parent.cells.indexOf(this) + (insertBefore ? 0 : 1), cell);

            int sx = this.sx;
            int sy = this.sy;
            int xoff = this.xoff;
            int yoff = this.yoff;
            Layout cell1, cell2;
            if (insertBefore) {
                cell1 = cell;
                cell2 = this;
            } else {
                cell1 = this;
                cell2 = cell;
            }
            if (type == LeftRight) {
                cell1.setSize(size1, sy, xoff, yoff);
                cell2.setSize(size2, sy, xoff + size1 + 1, yoff);
            } else {
                cell1.setSize(sx, size1, xoff, yoff);
                cell2.setSize(sx, size2, xoff, yoff + size1 + 1);
            }
            return cell;
        }

        private void setSize(int sx, int sy, int xoff, int yoff) {
            this.sx = sx;
            this.sy = sy;
            this.xoff = xoff;
            this.yoff = yoff;
        }

        private static int checksum(CharSequence layout) {
            return checksum(layout, 0);
        }

        private static int checksum(CharSequence layout, int start) {
            int csum = 0;
            for (int i = start; i < layout.length(); i++) {
                csum = (csum >> 1) + ((csum & 1) << 15);
                csum += layout.charAt(i);
            }
            return csum;
        }

        private static Layout parseCell(Layout parent, String layout) {
            Matcher matcher = PATTERN.matcher(layout);
            if (matcher.matches()) {
                Layout cell = new Layout();
                cell.type = Type.WindowPane;
                cell.parent = parent;
                cell.sx = Integer.parseInt(matcher.group(1));
                cell.sy = Integer.parseInt(matcher.group(2));
                cell.xoff = Integer.parseInt(matcher.group(3));
                cell.yoff = Integer.parseInt(matcher.group(4));
                if (parent != null) {
                    parent.cells.add(cell);
                }
                layout = matcher.group(5);
                if (layout == null || layout.isEmpty()) {
                    return cell;
                }
                if (layout.charAt(0) == ',') {
                    int i = 1;
                    while (i < layout.length() && Character.isDigit(layout.charAt(i))) {
                        i++;
                    }
                    if (i == layout.length()) {
                        return cell;
                    }
                    if (layout.charAt(i) == ',') {
                        layout = layout.substring(i);
                    }
                }
                int i;
                switch (layout.charAt(0)) {
                    case '{':
                        cell.type = LeftRight;
                        i = findMatch(layout, '{', '}');
                        parseCell(cell, layout.substring(1, i));
                        layout = layout.substring(i + 1);
                        if (!layout.isEmpty() && layout.charAt(0) == ',') {
                            parseCell(parent, layout.substring(1));
                        }
                        return cell;
                    case '[':
                        cell.type = Type.TopBottom;
                        i = findMatch(layout, '[', ']');
                        parseCell(cell, layout.substring(1, i));
                        layout = layout.substring(i + 1);
                        if (!layout.isEmpty() && layout.charAt(0) == ',') {
                            parseCell(parent, layout.substring(1));
                        }
                        return cell;
                    case ',':
                        parseCell(parent, layout.substring(1));
                        return cell;
                    default:
                        throw new IllegalArgumentException("Unexpected '" + layout.charAt(0) + "'");
                }
            } else {
                throw new IllegalArgumentException("Bad syntax");
            }
        }
    }

    private static int findMatch(String layout, char c0, char c1) {
        if (layout.charAt(0) != c0) {
            throw new IllegalArgumentException();
        }
        int nb = 0;
        int i = 0;
        while (i < layout.length()) {
            char c = layout.charAt(i);
            if (c == c0) {
                nb++;
            } else if (c == c1) {
                if (--nb == 0) {
                    return i;
                }
            }
            i++;
        }
        if (nb > 0) {
            throw new IllegalArgumentException("No matching '" + c1 + "'");
        }
        return i;
    }

    private static class VirtualConsole implements Closeable {
        private final ScreenTerminal terminal;
        private final Consumer<VirtualConsole> closer;
        private final int id;
        private int left;
        private int top;
        private final Layout layout;
        private int active;
        private boolean clock;
        private final OutputStream masterOutput;
        private final OutputStream masterInputOutput;
        private final LineDisciplineTerminal console;

        public VirtualConsole(
                int id,
                String type,
                int left,
                int top,
                int columns,
                int rows,
                Runnable dirty,
                Consumer<VirtualConsole> closer,
                Layout layout)
                throws IOException {
            String name = String.format("tmux%02d", id);
            this.id = id;
            this.left = left;
            this.top = top;
            this.closer = closer;
            this.terminal = new ScreenTerminal(columns, rows) {
                @Override
                protected void setDirty() {
                    super.setDirty();
                    dirty.run();
                }
            };
            this.masterOutput = new MasterOutputStream();
            this.masterInputOutput = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    console.processInputByte(b);
                }
            };
            this.console = new LineDisciplineTerminal(name, type, masterOutput, null) {
                @Override
                protected void doClose() throws IOException {
                    super.doClose();
                    closer.accept(VirtualConsole.this);
                }
            };
            this.console.setSize(new Size(columns, rows));
            this.layout = layout;
        }

        Layout layout() {
            return layout;
        }

        public int left() {
            return left;
        }

        public int top() {
            return top;
        }

        public int right() {
            return left() + width();
        }

        public int bottom() {
            return top() + height();
        }

        public int width() {
            return console.getWidth();
        }

        public int height() {
            return console.getHeight();
        }

        public LineDisciplineTerminal getConsole() {
            return console;
        }

        public OutputStream getMasterInputOutput() {
            return masterInputOutput;
        }

        public void resize(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            console.setSize(new Size(width, height));
            terminal.setSize(width, height);
            console.raise(Signal.WINCH);
        }

        public void dump(long[] fullscreen, int ftop, int fleft, int fheight, int fwidth, int[] cursor) {
            terminal.dump(fullscreen, ftop, fleft, fheight, fwidth, cursor);
        }

        @Override
        public void close() throws IOException {
            console.close();
        }

        private class MasterOutputStream extends OutputStream {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            private final CharsetDecoder decoder = Charset.defaultCharset()
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);

            @Override
            public synchronized void write(int b) {
                buffer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                buffer.write(b, off, len);
            }

            @Override
            public synchronized void flush() throws IOException {
                int size = buffer.size();
                if (size > 0) {
                    CharBuffer out;
                    for (; ; ) {
                        out = CharBuffer.allocate(size);
                        ByteBuffer in = ByteBuffer.wrap(buffer.toByteArray());
                        CoderResult result = decoder.decode(in, out, false);
                        if (result.isOverflow()) {
                            size *= 2;
                        } else {
                            buffer.reset();
                            buffer.write(in.array(), in.arrayOffset(), in.remaining());
                            break;
                        }
                    }
                    if (out.position() > 0) {
                        out.flip();
                        terminal.write(out);
                        masterInputOutput.write(terminal.read().getBytes());
                    }
                }
            }

            @Override
            public void close() throws IOException {
                flush();
            }
        }
    }
}
