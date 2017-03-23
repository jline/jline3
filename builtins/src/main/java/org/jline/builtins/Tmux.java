/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

/**
 * Terminal multiplexer
 */
public class Tmux {

    public static final String OPT_PREFIX = "prefix";

    public static final String CMD_SEND_PREFIX = "send-prefix";
    public static final String CMD_SPLIT_WINDOW = "split-window";
    public static final String CMD_SELECT_PANE = "select-pane";



    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private final AtomicBoolean resized = new AtomicBoolean(true);
    private final Terminal terminal;
    private final Display display;
    private final PrintStream err;
    private final String term;
    private final Consumer<Terminal> runner;
    private List<VirtualConsole> panes = new ArrayList<>();
    private VirtualConsole active;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Size size = new Size();
    private final AtomicInteger paneId = new AtomicInteger();

    private final Map<String, String> serverOptions = new HashMap<>();

    private KeyMap<Object> keyMap;

    enum Binding {
        Discard, SelfInsert, Mouse
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
        KeyMap<Object> keyMap = new KeyMap<>();
        keyMap.setUnicode(Binding.SelfInsert);
        keyMap.setNomatch(Binding.SelfInsert);
        for (int i = 0; i < 255; i++) {
            keyMap.bind(Binding.Discard, prefix + (char)(i));
        }
        keyMap.bind(Binding.Mouse, KeyMap.key(terminal, Capability.key_mouse));
        keyMap.bind(CMD_SEND_PREFIX, prefix + prefix);
        keyMap.bind(CMD_SPLIT_WINDOW, prefix + "\"");
        keyMap.bind(CMD_SPLIT_WINDOW + " -h", prefix + "%");
        keyMap.bind(CMD_SELECT_PANE + " -U", prefix + KeyMap.key(terminal, Capability.key_up));
        keyMap.bind(CMD_SELECT_PANE + " -L", prefix + KeyMap.key(terminal, Capability.key_left));
        keyMap.bind(CMD_SELECT_PANE + " -R", prefix + KeyMap.key(terminal, Capability.key_right));
        keyMap.bind(CMD_SELECT_PANE + " -D", prefix + KeyMap.key(terminal, Capability.key_down));
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
        try {
            // Create first pane
            size.copy(terminal.getSize());
            active = new VirtualConsole(getNewPaneName(), term, 0, 0, size.getColumns(), size.getRows() - 1, this::setDirty, this::close);
            active.getConsole().setAttributes(terminal.getAttributes());
            panes.add(active);
            runner.accept(active.getConsole());
            // Start input loop
            new Thread(this::inputLoop, "Mux input loop").start();
            // Redraw loop
            redrawLoop();
        } finally {
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
            if (resized.compareAndSet(true, false)) {
                handleResize();
            }
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
                    active.getMasterInputOutput().write(reader.getLastBinding().getBytes());
                    first = false;
                } else {
                    if (first) {
                        first = false;
                    } else {
                        active.getMasterInputOutput().flush();
                        first = true;
                    }
                    if (b == Binding.Mouse) {
                        MouseEvent event = terminal.readMouseEvent();
                        //System.err.println(event.toString());
                    } else if (b instanceof String) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ByteArrayOutputStream err = new ByteArrayOutputStream();
                        try (PrintStream pout = new PrintStream(out);
                             PrintStream perr = new PrintStream(err)) {
                            execute(pout, perr, (String) b);
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
        int idx = panes.indexOf(terminal);
        if (idx >= 0) {
            panes.remove(idx);
            if (panes.isEmpty()) {
                running.set(false);
                setDirty();
            } else {
                if (active == terminal) {
                    active = panes.get(Math.max(0, idx - 1));
                }
                resize(Signal.WINCH);
            }
        }
    }

    private void resize(Signal signal) {
        resized.set(true);
        setDirty();
    }

    private void interrupt(Signal signal) {
        active.getConsole().raise(signal);
    }

    private void suspend(Signal signal) {
        active.getConsole().raise(signal);
    }

    private void handleResize() {
        // Re-compute the layout
        // TODO: implement this correctly
        // TODO: for now, force a tiled-layout
        size.copy(terminal.getSize());
        int nbPanes = panes.size();
        int nbRows = 1;
        while (nbRows * nbRows < nbPanes) {
            nbRows++;
        }
        int nbCols = (nbPanes + nbRows - 1) / nbRows;
        int width = size.getColumns();
        int height = size.getRows() - 1;
        int colWidth = (width - nbCols + 1) / nbCols;
        int rowHeight = (height - nbRows + 1) / nbRows;
        for (int pane = 0; pane < nbPanes; pane++) {
            VirtualConsole terminal = panes.get(pane);
            int i = pane % nbCols;
            int j = pane / nbCols;
            int l = i * colWidth + i;
            int w = (i == nbCols - 1 || pane == nbPanes - 1) ? width - l : colWidth;
            int t = j * rowHeight + j;
            int h = (j == nbRows - 1 || pane == nbPanes - 1) ? height - t : rowHeight;
            terminal.resize(l, t, w, h);
        }
        display.clear();
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
                splitWindow(out, err, args);
                break;
            case CMD_SELECT_PANE:
                selectPane(out, err, args);
                break;
        }
    }

    protected void selectPane(PrintStream out, PrintStream err, List<String> args) throws IOException {
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
            opt.usage(err);
            return;
        }
        VirtualConsole prevActive = active;
        if (opt.isSet("L")) {
            // TODO: this is a wrong implementation
            int idx = panes.indexOf(active);
            active = panes.get((idx + panes.size() - 1) % panes.size());
        }
        else if (opt.isSet("R")) {
            // TODO: this is a wrong implementation
            int idx = panes.indexOf(active);
            active = panes.get((idx + 1) % panes.size());
        }
        if (prevActive != active) {
            setDirty();
        }
    }

    protected void sendPrefix(PrintStream out, PrintStream err, List<String> args) throws IOException {
        final String[] usage = {
                "send-prefix - ",
                "Usage: send-prefix [-2] [-t target-pane]",
                "  -? --help                    Show help",
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            opt.usage(err);
            return;
        }
        active.getMasterInputOutput().write(serverOptions.get(OPT_PREFIX).getBytes());
    }

    protected void splitWindow(PrintStream out, PrintStream err, List<String> args) throws IOException {
        final String[] usage = {
                "split-window - ",
                "Usage: split-window [-bdhvP] [-c start-directory] [-F format] [-p percentage|-l size] [-t target-pane] [command]",
                "  -? --help                    Show help",
                "  -h                           Horizontal split",
                "  -v                           Vertical split"
        };
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            opt.usage(err);
            return;
        }
        VirtualConsole target = active;
        if (opt.isSet("h")) {
            int l0 = target.getLeft();
            int t0 = target.getTop();
            int w0 = target.getWidth();
            int h0 = target.getHeight();
            int l1 = l0;
            int w1 = (w0 - 1) / 2;
            int l2 = l1 + w1 + 1;
            int w2 = l0 + w0 - l2;
            target.resize(l1, t0, w1, h0);
            active = new VirtualConsole(getNewPaneName(), term, l2, t0, w2, h0, this::setDirty, this::close);
            active.getConsole().setAttributes(terminal.getAttributes());
            panes.add(panes.indexOf(target) + 1, active);
            runner.accept(active.getConsole());
        } else {
            int l0 = target.getLeft();
            int t0 = target.getTop();
            int w0 = target.getWidth();
            int h0 = target.getHeight();
            int t1 = t0;
            int h1 = (h0 - 1) / 2;
            int t2 = t1 + h1 + 1;
            int h2 = t0 + h0 - t2;
            target.resize(l0, t1, w0, h1);
            active = new VirtualConsole(getNewPaneName(), term, l0, t2, w0, h2, this::setDirty, this::close);
            active.getConsole().setAttributes(terminal.getAttributes());
            panes.add(panes.indexOf(target) + 1, active);
            runner.accept(active.getConsole());
        }
    }

    protected synchronized void redraw() {
        long[] screen = new long[size.getRows() * size.getColumns()];
        // Fill
        Arrays.fill(screen, 0x00000020L);
        int[] cursor = new int[2];
        for (VirtualConsole terminal : panes) {
            // Dump terminal
            terminal.dump(screen, terminal.getTop(), terminal.getLeft(), size.getRows(), size.getColumns(),
                    terminal == active ? cursor : null);
            // Draw border
            drawBorder(screen, size, terminal, 0x0L);
        }
        drawBorder(screen, size, active, 0x010080000L << 32);
        // Draw status
        Arrays.fill(screen, (size.getRows() - 1) * size.getColumns(), size.getRows() * size.getColumns(),
                0x20000080L << 32 | 0x0020L);

        // Attribute mask: 0xYXFFFBBB00000000L
        //	X:	Bit 0 - Underlined
        //		Bit 1 - Negative
        //		Bit 2 - Concealed
        //      Bit 3 - Bold
        //  Y:  Bit 0 - Foreground set
        //      Bit 1 - Background set
        //	F:	Foreground r-g-b
        //	B:	Background r-g-b

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
                boolean ul =      ((a & 0x01000000) != 0);
                boolean inv =     ((a & 0x02000000) != 0);
                boolean conceal = ((a & 0x04000000) != 0);
                boolean bold =    ((a & 0x08000000) != 0);
                boolean hasFg =   ((a & 0x10000000) != 0);
                boolean hasBg =   ((a & 0x20000000) != 0);

                if ((hasBg && prevHasBg && bg != prevBg) || prevHasBg != hasBg) {
                    if (!hasBg) {
                        sb.style(sb.style().backgroundDefault());
                    } else {
                        int col = bg;
                        col = AttributedCharSequence.roundRgbColor((col & 0xF00) >> 4, (col & 0x0F0), (col & 0x00F) << 4, 256);
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
                        col = AttributedCharSequence.roundRgbColor((col & 0xF00) >> 4, (col & 0x0F0), (col & 0x00F) << 4, 256);
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
        terminal.flush();
    }

    private void drawBorder(long[] screen, Size size, VirtualConsole terminal, long attr) {
        for (int i = terminal.getLeft(); i < terminal.getLeft() + terminal.getWidth(); i++) {
            int y0 = terminal.getTop() - 1;
            int y1 = terminal.getTop() + terminal.getHeight();
            drawBorderChar(screen, size, i, y0, attr, '─');
            drawBorderChar(screen, size, i, y1, attr, '─');
        }
        for (int i = terminal.getTop(); i < terminal.getTop() + terminal.getHeight(); i++) {
            int x0 = terminal.getLeft() - 1;
            int x1 = terminal.getLeft() + terminal.getWidth();
            drawBorderChar(screen, size, x0, i, attr, '│');
            drawBorderChar(screen, size, x1, i, attr, '│');
        }
        drawBorderChar(screen, size, terminal.getLeft() - 1, terminal.getTop() - 1, attr, '┌');
        drawBorderChar(screen, size, terminal.getLeft() + terminal.getWidth(), terminal.getTop() - 1, attr, '┐');
        drawBorderChar(screen, size, terminal.getLeft() - 1, terminal.getTop() + terminal.getHeight(), attr, '└');
        drawBorderChar(screen, size, terminal.getLeft() + terminal.getWidth(), terminal.getTop() + terminal.getHeight(), attr, '┘');
    }

    private void drawBorderChar(long[] screen, Size size, int x, int y, long attr, int c) {
        if (x >= 0 && x < size.getColumns() && y >= 0 && y < size.getRows() - 1) {
            int oldc = (int)(screen[y * size.getColumns() + x] & 0xFFFFFFFFL);
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
                    case '│': return '┤';
                    case '─': return '─';
                    case '┌': return '┬';
                    case '┐': return '┐';
                    case '└': return '┴';
                    case '┘': return '┘';
                    case '├': return '┼';
                    case '┤': return '┤';
                    case '┬': return '┬';
                    case '┴': return '┴';
                    default:
                        throw new IllegalArgumentException();
                }
            case '╵':
                switch (oldc) {
                    case '│': return '│';
                    case '─': return '┴';
                    case '┌': return '├';
                    case '┐': return '┤';
                    case '└': return '└';
                    case '┘': return '┘';
                    case '├': return '├';
                    case '┤': return '┤';
                    case '┬': return '┼';
                    case '┴': return '┴';
                    default:
                        throw new IllegalArgumentException();
                }
            case '╶':
                switch (oldc) {
                    case '│': return '├';
                    case '─': return '─';
                    case '┌': return '┌';
                    case '┐': return '┬';
                    case '└': return '└';
                    case '┘': return '┴';
                    case '├': return '├';
                    case '┤': return '┼';
                    case '┬': return '┬';
                    case '┴': return '┴';
                    default:
                        throw new IllegalArgumentException();
                }
            case '╷':
                switch (oldc) {
                    case '│': return '│';
                    case '─': return '┬';
                    case '┌': return '┌';
                    case '┐': return '┐';
                    case '└': return '├';
                    case '┘': return '┤';
                    case '├': return '├';
                    case '┤': return '┤';
                    case '┬': return '┬';
                    case '┴': return '┼';
                    default:
                        throw new IllegalArgumentException();
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    private String getNewPaneName() {
        return String.format("tmux%02d", paneId.incrementAndGet());
    }

    private static class VirtualConsole implements Closeable {
        private final ScreenTerminal terminal;
        private final Consumer<VirtualConsole> closer;
        private int left;
        private int top;
        private final OutputStream masterOutput;
        private final OutputStream masterInputOutput;
        private final LineDisciplineTerminal console;

        public VirtualConsole(String name, String type, int left, int top, int columns, int rows, Runnable dirty, Consumer<VirtualConsole> closer) throws IOException {
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
            this.console = new LineDisciplineTerminal(
                    name,
                    type,
                    masterOutput,
                    Charset.defaultCharset().name()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    closer.accept(VirtualConsole.this);
                }
            };
            this.console.setSize(new Size(columns, rows));
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getWidth() {
            return console.getWidth();
        }

        public int getHeight() {
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

            @Override
            public synchronized void write(int b) {
                buffer.write(b);
            }

            @Override
            public synchronized void flush() throws IOException {
                terminal.write(buffer.toString());
                masterInputOutput.write(terminal.read().getBytes());
                buffer.reset();
            }

            @Override
            public void close() throws IOException {
                flush();
            }
        }

    }

}
