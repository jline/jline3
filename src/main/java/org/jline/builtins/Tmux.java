/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jline.Console;
import org.jline.Console.Signal;
import org.jline.Console.SignalHandler;
import org.jline.JLine;
import org.jline.console.AbstractDisciplinedConsole;
import org.jline.console.Attributes;
import org.jline.console.Size;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.DefaultParser;
import org.jline.reader.Display;
import org.jline.reader.ParsedLine;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;
import org.jline.utils.InfoCmp;
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
    private final Console console;
    private final Display display;
    private final PrintStream err;
    private final String term;
    private final Consumer<Console> runner;
    private List<VirtualTerminal> panes = new ArrayList<>();
    private VirtualTerminal active;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Size size = new Size();

    private final Map<String, String> serverOptions = new HashMap<>();
    private final KeyMap keyMap = new KeyMap();

    private static final Object UNMAPPED = new Object();


    public Tmux(Console console, PrintStream err, Consumer<Console> runner) throws IOException {
        InfoCmp.setDefaultInfoCmp("screen", SCREEN_CAPS);
        InfoCmp.setDefaultInfoCmp("screen-256color", SCREEN_256COLOR_CAPS);
        this.console = console;
        this.err = err;
        this.runner = runner;
        display = new Display(console, true);
        // Find terminal to use
        Integer colors = console.getNumericCapability(Capability.max_colors);
        term = (colors != null && colors >= 256) ? "screen-256color" : "screen";
        // Setup defaults bindings
        serverOptions.put(OPT_PREFIX, "`");
        keyMap.bind(UNMAPPED, KeyMap.range("^@-^?"));
        keyMap.bind(CMD_SEND_PREFIX, serverOptions.get(OPT_PREFIX));
        keyMap.bind(CMD_SEND_PREFIX, serverOptions.get(OPT_PREFIX));
        keyMap.bind(CMD_SPLIT_WINDOW, "\"");
        keyMap.bind(CMD_SPLIT_WINDOW + " -h", "%");
        keyMap.bind(CMD_SELECT_PANE + " -U", KeyMap.key(console, Capability.key_up));
        keyMap.bind(CMD_SELECT_PANE + " -L", KeyMap.key(console, Capability.key_left));
        keyMap.bind(CMD_SELECT_PANE + " -R", KeyMap.key(console, Capability.key_right));
        keyMap.bind(CMD_SELECT_PANE + " -D", KeyMap.key(console, Capability.key_down));
    }

    public void run() throws IOException {
        SignalHandler prevWinchHandler = console.handle(Signal.WINCH, this::resize);
        SignalHandler prevIntHandler = console.handle(Signal.INT, this::interrupt);
        Attributes attributes = console.enterRawMode();
        console.puts(Capability.enter_ca_mode);
        console.puts(Capability.keypad_xmit);
        console.flush();
        try {
            // Create first pane
            size.copy(console.getSize());
            active = new VirtualTerminal(term, 0, 0, size.getColumns(), size.getRows() - 1, this::setDirty, this::close);
            active.setAttributes(console.getAttributes());
            panes.add(active);
            runner.accept(active);
            // Start input loop
            new Thread(this::inputLoop, "Mux input loop").start();
            // Redraw loop
            redrawLoop();
        } finally {
            console.puts(Capability.keypad_local);
            console.puts(Capability.exit_ca_mode);
            console.flush();
            console.setAttributes(attributes);
            console.handle(Signal.WINCH, prevWinchHandler);
            console.handle(Signal.INT, prevIntHandler);
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
            int c;
            while ((c = console.reader().read()) >= 0) {
                String pfx = serverOptions.get(OPT_PREFIX);
                if (pfx != null && c == pfx.charAt(0)) {
                    // escape sequences
                    Object b = new BindingReader(console, UNMAPPED).readBinding(keyMap);
                    if (b instanceof String) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ByteArrayOutputStream err = new ByteArrayOutputStream();
                        try (PrintStream pout = new PrintStream(out);
                             PrintStream perr = new PrintStream(err)) {
                            execute(pout, perr, (String) b);
                        } catch (Exception e) {
                            // TODO: log
                        }
                    }
                } else {
                    if (Character.isBmpCodePoint(c)) {
                        active.writeInput(c);
                    } else {
                        active.writeInput(Character.toChars(c));
                    }
                    active.flushInput();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            running.set(false);
            setDirty();
        }
    }

    private synchronized void close(VirtualTerminal terminal) {
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
        active.raise(signal);
    }

    private void handleResize() {
        // Re-compute the layout
        // TODO: implement this correctly
        // TODO: for now, force a tiled-layout
        size.copy(console.getSize());
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
            VirtualTerminal terminal = panes.get(pane);
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
        VirtualTerminal prevActive = active;
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
        active.writeInput(serverOptions.get(OPT_PREFIX));
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
        VirtualTerminal target = active;
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
            active = new VirtualTerminal(term, l2, t0, w2, h0, this::setDirty, this::close);
            active.setAttributes(console.getAttributes());
            panes.add(panes.indexOf(target) + 1, active);
            runner.accept(active);
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
            active = new VirtualTerminal(term, l0, t2, w0, h2, this::setDirty, this::close);
            active.setAttributes(console.getAttributes());
            panes.add(panes.indexOf(target) + 1, active);
            runner.accept(active);
        }
    }

    protected synchronized void redraw() {
        int[] screen = new int[size.getRows() * size.getColumns()];
        // Fill
        Arrays.fill(screen, 0x00ff0020);
        int[] cursor = new int[2];
        for (VirtualTerminal terminal : panes) {
            // Dump terminal
            terminal.dump(screen, terminal.getTop(), terminal.getLeft(), size.getRows(), size.getColumns(),
                    terminal == active ? cursor : null);
            // Draw border
            drawBorder(screen, size, terminal, 0x00ff0000);
        }
        drawBorder(screen, size, active, 0x002f0000);
        // Draw status
        Arrays.fill(screen, (size.getRows() - 1) * size.getColumns(), size.getRows() * size.getColumns(), 0x00f20020);

        // Attribute mask: 0xYXFB0000
        //	X:	Bit 0 - Underlined
        //		Bit 1 - Negative
        //		Bit 2 - Concealed
        //		Bit 3 - Bold
        //  Y:  Bit 0 - Foreground bright
        //      Bit 1 - Background bright
        //	F:	Foreground
        //	B:	Background

        List<String> lines = new ArrayList<>();
        int prevBg = 15;
        int prevFg = 15;
        boolean prevInv = false;
        boolean prevUl = false;
        boolean prevBold = false;
        boolean prevConceal = false;
        boolean prevFgBright = false;
        boolean prevBgBright = false;
        for (int y = 0; y < size.getRows(); y++) {
            Ansi ansi = new Ansi(size.getColumns() * 2);
            for (int x = 0; x < size.getColumns(); x++) {
                int d = screen[y * size.getColumns() + x];
                int c = d & 0xffff;
                int a = d >> 16;
                int bg = a & 0x000f;
                int fg = (a & 0x00f0) >> 4;
                boolean inv = ((a & 0x0200) != 0);
                boolean ul = ((a & 0x0100) != 0);
                boolean bold = ((a & 0x0800) != 0);
                boolean conceal = ((a & 0x0400) != 0);
                boolean fgBright = ((a & 0x1000) != 0);
                boolean bgBright = ((a & 0x2000) != 0);
                if (bg != prevBg || prevBgBright != bgBright) {
                    Color col = Color.DEFAULT;
                    switch (bg) {
                        case 0: col = Color.BLACK; break;
                        case 1: col = Color.RED; break;
                        case 2: col = Color.GREEN; break;
                        case 3: col = Color.YELLOW; break;
                        case 4: col = Color.BLUE; break;
                        case 5: col = Color.MAGENTA; break;
                        case 6: col = Color.CYAN; break;
                        case 7: col = Color.WHITE; break;
                    }
                    if (bgBright) {
                        ansi.bgBright(col);
                    } else {
                        ansi.bg(col);
                    }
                    prevBg = bg;
                    prevBgBright = bgBright;
                }
                if (fg != prevFg || fgBright != prevFgBright) {
                    Color col = Color.DEFAULT;
                    switch (fg) {
                        case 0: col = Color.BLACK; break;
                        case 1: col = Color.RED; break;
                        case 2: col = Color.GREEN; break;
                        case 3: col = Color.YELLOW; break;
                        case 4: col = Color.BLUE; break;
                        case 5: col = Color.MAGENTA; break;
                        case 6: col = Color.CYAN; break;
                        case 7: col = Color.WHITE; break;
                    }
                    if (fgBright) {
                        ansi.fgBright(col);
                    } else {
                        ansi.fg(col);
                    }
                    prevFg = fg;
                    prevFgBright = fgBright;
                }
                if (conceal != prevConceal) {
                    ansi.a(conceal ? Attribute.CONCEAL_ON : Attribute.CONCEAL_OFF);
                    prevConceal = conceal;
                }
                if (inv != prevInv) {
                    ansi.a(inv ? Attribute.NEGATIVE_ON : Attribute.NEGATIVE_OFF);
                    prevInv = inv;
                }
                if (ul != prevUl) {
                    ansi.a(ul ? Attribute.UNDERLINE : Attribute.UNDERLINE_OFF);
                    prevUl = ul;
                }
                if (bold != prevBold) {
                    ansi.a(bold ? Attribute.INTENSITY_BOLD : Attribute.INTENSITY_BOLD_OFF);
                    prevBold = bold;
                }
                ansi.a((char) c);
            }
            ansi.reset();
            lines.add(ansi.toString());
        }
        display.resize(size.getRows(), size.getColumns());
        display.update(lines, cursor[1] * size.getColumns() + cursor[0]);
        console.flush();
    }

    private void drawBorder(int[] screen, Size size, VirtualTerminal terminal, int attr) {
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
        // TODO: fix that to have clean crosses
        drawBorderChar(screen, size, terminal.getLeft() - 1, terminal.getTop() - 1, attr, '┼');
        drawBorderChar(screen, size, terminal.getLeft() + terminal.getWidth(), terminal.getTop() - 1, attr, '┼');
        drawBorderChar(screen, size, terminal.getLeft() - 1, terminal.getTop() + terminal.getHeight(), attr, '┼');
        drawBorderChar(screen, size, terminal.getLeft() + terminal.getWidth(), terminal.getTop() + terminal.getHeight(), attr, '┼');
    }

    private void drawBorderChar(int[] screen, Size size, int x, int y, int attr, int c) {
        if (x >= 0 && x < size.getColumns() && y >= 0 && y < size.getRows() - 1) {
            screen[y * size.getColumns() + x] = attr | c;
        }
    }

    private static final String SCREEN_CAPS =
                    "#\tReconstructed via infocmp from file: /usr/share/terminfo/73/screen\n" +
                    "screen|VT 100/ANSI X3.64 virtual terminal,\n" +
                    "\tam, km, mir, msgr, xenl,\n" +
                    "\tcolors#8, cols#80, it#8, lines#24, ncv#3, pairs#64,\n" +
                    "\tacsc=++\\,\\,--..00``aaffgghhiijjkkllmmnnooppqqrrssttuuvvwwxxyyzz{{||}}~~,\n" +
                    "\tbel=^G, blink=\\E[5m, bold=\\E[1m, cbt=\\E[Z, civis=\\E[?25l,\n" +
                    "\tclear=\\E[H\\E[J, cnorm=\\E[34h\\E[?25h, cr=^M,\n" +
                    "\tcsr=\\E[%i%p1%d;%p2%dr, cub=\\E[%p1%dD, cub1=^H,\n" +
                    "\tcud=\\E[%p1%dB, cud1=^J, cuf=\\E[%p1%dC, cuf1=\\E[C,\n" +
                    "\tcup=\\E[%i%p1%d;%p2%dH, cuu=\\E[%p1%dA, cuu1=\\EM,\n" +
                    "\tcvvis=\\E[34l, dch=\\E[%p1%dP, dch1=\\E[P, dl=\\E[%p1%dM,\n" +
                    "\tdl1=\\E[M, ed=\\E[J, el=\\E[K, el1=\\E[1K, enacs=\\E(B\\E)0,\n" +
                    "\tflash=\\Eg, home=\\E[H, ht=^I, hts=\\EH, ich=\\E[%p1%d@,\n" +
                    "\til=\\E[%p1%dL, il1=\\E[L, ind=^J, is2=\\E)0, kbs=^H, kcbt=\\E[Z,\n" +
                    "\tkcub1=\\EOD, kcud1=\\EOB, kcuf1=\\EOC, kcuu1=\\EOA,\n" +
                    "\tkdch1=\\E[3~, kend=\\E[4~, kf1=\\EOP, kf10=\\E[21~,\n" +
                    "\tkf11=\\E[23~, kf12=\\E[24~, kf2=\\EOQ, kf3=\\EOR, kf4=\\EOS,\n" +
                    "\tkf5=\\E[15~, kf6=\\E[17~, kf7=\\E[18~, kf8=\\E[19~, kf9=\\E[20~,\n" +
                    "\tkhome=\\E[1~, kich1=\\E[2~, kmous=\\E[M, knp=\\E[6~, kpp=\\E[5~,\n" +
                    "\tnel=\\EE, op=\\E[39;49m, rc=\\E8, rev=\\E[7m, ri=\\EM, rmacs=^O,\n" +
                    "\trmcup=\\E[?1049l, rmir=\\E[4l, rmkx=\\E[?1l\\E>, rmso=\\E[23m,\n" +
                    "\trmul=\\E[24m, rs2=\\Ec\\E[?1000l\\E[?25h, sc=\\E7,\n" +
                    "\tsetab=\\E[4%p1%dm, setaf=\\E[3%p1%dm,\n" +
                    "\tsgr=\\E[0%?%p6%t;1%;%?%p1%t;3%;%?%p2%t;4%;%?%p3%t;7%;%?%p4%t;5%;m%?%p9%t\\016%e\\017%;,\n" +
                    "\tsgr0=\\E[m\\017, smacs=^N, smcup=\\E[?1049h, smir=\\E[4h,\n" +
                    "\tsmkx=\\E[?1h\\E=, smso=\\E[3m, smul=\\E[4m, tbc=\\E[3g,\n";

    private static final String SCREEN_256COLOR_CAPS =
                    "#\tReconstructed via infocmp from file: /usr/share/terminfo/73/screen-256color\n" +
                    "screen-256color|GNU Screen with 256 colors,\n" +
                    "\tam, km, mir, msgr, xenl,\n" +
                    "\tcolors#256, cols#80, it#8, lines#24, ncv#3, pairs#32767,\n" +
                    "\tacsc=++\\,\\,--..00``aaffgghhiijjkkllmmnnooppqqrrssttuuvvwwxxyyzz{{||}}~~,\n" +
                    "\tbel=^G, blink=\\E[5m, bold=\\E[1m, cbt=\\E[Z, civis=\\E[?25l,\n" +
                    "\tclear=\\E[H\\E[J, cnorm=\\E[34h\\E[?25h, cr=^M,\n" +
                    "\tcsr=\\E[%i%p1%d;%p2%dr, cub=\\E[%p1%dD, cub1=^H,\n" +
                    "\tcud=\\E[%p1%dB, cud1=^J, cuf=\\E[%p1%dC, cuf1=\\E[C,\n" +
                    "\tcup=\\E[%i%p1%d;%p2%dH, cuu=\\E[%p1%dA, cuu1=\\EM,\n" +
                    "\tcvvis=\\E[34l, dch=\\E[%p1%dP, dch1=\\E[P, dl=\\E[%p1%dM,\n" +
                    "\tdl1=\\E[M, ed=\\E[J, el=\\E[K, el1=\\E[1K, enacs=\\E(B\\E)0,\n" +
                    "\tflash=\\Eg, home=\\E[H, ht=^I, hts=\\EH, ich=\\E[%p1%d@,\n" +
                    "\til=\\E[%p1%dL, il1=\\E[L, ind=^J, initc@, is2=\\E)0, kbs=^H,\n" +
                    "\tkcbt=\\E[Z, kcub1=\\EOD, kcud1=\\EOB, kcuf1=\\EOC, kcuu1=\\EOA,\n" +
                    "\tkdch1=\\E[3~, kend=\\E[4~, kf1=\\EOP, kf10=\\E[21~,\n" +
                    "\tkf11=\\E[23~, kf12=\\E[24~, kf2=\\EOQ, kf3=\\EOR, kf4=\\EOS,\n" +
                    "\tkf5=\\E[15~, kf6=\\E[17~, kf7=\\E[18~, kf8=\\E[19~, kf9=\\E[20~,\n" +
                    "\tkhome=\\E[1~, kich1=\\E[2~, kmous=\\E[M, knp=\\E[6~, kpp=\\E[5~,\n" +
                    "\tnel=\\EE, op=\\E[39;49m, rc=\\E8, rev=\\E[7m, ri=\\EM, rmacs=^O,\n" +
                    "\trmcup=\\E[?1049l, rmir=\\E[4l, rmkx=\\E[?1l\\E>, rmso=\\E[23m,\n" +
                    "\trmul=\\E[24m, rs2=\\Ec\\E[?1000l\\E[?25h, sc=\\E7,\n" +
                    "\tsetab=\\E[%?%p1%{8}%<%t4%p1%d%e%p1%{16}%<%t10%p1%{8}%-%d%e48;5;%p1%d%;m,\n" +
                    "\tsetaf=\\E[%?%p1%{8}%<%t3%p1%d%e%p1%{16}%<%t9%p1%{8}%-%d%e38;5;%p1%d%;m,\n" +
                    "\tsgr=\\E[0%?%p6%t;1%;%?%p1%t;3%;%?%p2%t;4%;%?%p3%t;7%;%?%p4%t;5%;m%?%p9%t\\016%e\\017%;,\n" +
                    "\tsgr0=\\E[m\\017, smacs=^N, smcup=\\E[?1049h, smir=\\E[4h,\n" +
                    "\tsmkx=\\E[?1h\\E=, smso=\\E[3m, smul=\\E[4m, tbc=\\E[3g,\n";

    private static class VirtualTerminal extends AbstractDisciplinedConsole {
        private final ScreenTerminal terminal;
        private final StringBuilder buffer = new StringBuilder();
        private final Consumer<VirtualTerminal> closer;
        private int left;
        private int top;

        public VirtualTerminal(String type, int left, int top, int columns, int rows, Runnable dirty, Consumer<VirtualTerminal> closer) throws IOException {
            super(type, JLine.readerBuilder(), Charset.defaultCharset().name());
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
            size.setColumns(columns);
            size.setRows(rows);
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public void resize(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            size.setColumns(width);
            size.setRows(height);
            terminal.setSize(width, height);
            raise(Signal.WINCH);
        }

        public void writeInput(int c) throws IOException {
            filterInOutWriter.write(c);
        }

        public void writeInput(String str) throws IOException {
            filterInOutWriter.write(str);
        }

        public void writeInput(char[] cbuf) throws IOException {
            filterInOutWriter.write(cbuf);
        }

        public void flushInput() throws IOException {
            filterInOutWriter.flush();
        }

        public void dump(int[] fullscreen, int ftop, int fleft, int fheight, int fwidth, int[] cursor) {
            terminal.dump(fullscreen, ftop, fleft, fheight, fwidth, cursor);
        }

        @Override
        protected void doWriteByte(int c) throws IOException {
            buffer.append((char) c);
        }

        @Override
        protected void doFlush() throws IOException {
            terminal.write(buffer.toString());
            filterInOutWriter.write(terminal.read());
            filterInOutWriter.flush();
            buffer.setLength(0);
        }

        @Override
        protected void doClose() throws IOException {
            doFlush();
        }

        @Override
        public void close() throws IOException {
            super.close();
            closer.accept(this);
        }

    }

}
