/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOError;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.spi.TerminalExt;
import org.jline.utils.AttributedCharSequence;
import org.jline.utils.ColorPalette;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Log;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.Status;
import org.jline.utils.WCWidth;

/**
 * Base implementation of the Terminal interface.
 *
 * <p>
 * This abstract class provides a common foundation for terminal implementations,
 * handling many of the core terminal functions such as signal handling, attribute
 * management, and capability lookup. It implements most of the methods defined in
 * the {@link org.jline.terminal.Terminal} interface, leaving only a few abstract
 * methods to be implemented by concrete subclasses.
 * </p>
 *
 * <p>
 * Terminal implementations typically extend this class and provide implementations
 * for the abstract methods related to their specific platform or environment.
 * This class handles the common functionality, allowing subclasses to focus on
 * platform-specific details.
 * </p>
 *
 * <p>
 * Key features provided by this class include:
 * </p>
 * <ul>
 *   <li>Signal handling infrastructure</li>
 *   <li>Terminal attribute management</li>
 *   <li>Terminal capability lookup and caching</li>
 *   <li>Size and cursor position handling</li>
 *   <li>Mouse and focus tracking support</li>
 * </ul>
 *
 * @see org.jline.terminal.Terminal
 * @see org.jline.terminal.spi.TerminalExt
 */
public abstract class AbstractTerminal implements TerminalExt {

    protected final String name;
    protected final String type;
    protected final Charset encoding;
    protected final Charset inputEncoding;
    protected final Charset outputEncoding;
    protected final Map<Signal, SignalHandler> handlers = new ConcurrentHashMap<>();
    protected final Set<Capability> bools = new HashSet<>();
    protected final Map<Capability, Integer> ints = new HashMap<>();
    protected final Map<Capability, String> strings = new HashMap<>();
    protected final ColorPalette palette;
    protected Status status;
    protected Runnable onClose;
    protected MouseTracking currentMouseTracking = MouseTracking.Off;
    protected volatile boolean closed = false;
    private Boolean graphemeClusterModeSupported;
    private boolean graphemeClusterModeEnabled;
    private boolean graphemeClusterNative;
    private boolean groupsRegionalIndicators;
    private boolean groupsZwjSequences;

    /** Result of the Mode 2027 probe. */
    private enum ProbeResult {
        /** Mode 2027 is supported. */
        SUPPORTED,
        /** Terminal responded but does not support Mode 2027. */
        NOT_SUPPORTED,
        /** Terminal did not respond at all. */
        NO_RESPONSE
    }

    /**
     * Create a terminal with the given name and type using the platform default charset and the default signal handler.
     *
     * @param name the terminal name (may be {@code null})
     * @param type the terminal type (may be {@code null}, a default will be used when absent)
     * @throws IOException if an I/O error occurs while constructing the terminal
     */
    public AbstractTerminal(String name, String type) throws IOException {
        this(name, type, null, SignalHandler.SIG_DFL);
    }

    @SuppressWarnings("this-escape")
    public AbstractTerminal(String name, String type, Charset encoding, SignalHandler signalHandler)
            throws IOException {
        this(name, type, encoding, encoding, encoding, signalHandler);
    }

    @SuppressWarnings("this-escape")
    public AbstractTerminal(
            String name,
            String type,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            SignalHandler signalHandler)
            throws IOException {
        this.name = name;
        this.type = type != null ? type : "ansi";
        this.encoding = encoding != null ? encoding : Charset.defaultCharset();
        this.inputEncoding = inputEncoding != null ? inputEncoding : this.encoding;
        this.outputEncoding = outputEncoding != null ? outputEncoding : this.encoding;
        this.palette = new ColorPalette(this);
        for (Signal signal : Signal.values()) {
            handlers.put(signal, signalHandler);
        }
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public Status getStatus() {
        return getStatus(true);
    }

    public Status getStatus(boolean create) {
        if (status == null && create) {
            status = new Status(this);
        }
        return status;
    }

    public SignalHandler handle(Signal signal, SignalHandler handler) {
        Objects.requireNonNull(signal);
        Objects.requireNonNull(handler);
        return handlers.put(signal, handler);
    }

    public void raise(Signal signal) {
        Objects.requireNonNull(signal);
        SignalHandler handler = handlers.get(signal);
        if (handler == SignalHandler.SIG_DFL) {
            if (status != null && signal == Signal.WINCH) {
                status.resize();
            }
        } else if (handler != SignalHandler.SIG_IGN) {
            handler.handle(signal);
        }
    }

    public final void close() throws IOException {
        try {
            doClose();
        } finally {
            if (onClose != null) {
                onClose.run();
            }
        }
    }

    protected void doClose() throws IOException {
        if (graphemeClusterModeEnabled) {
            setGraphemeClusterMode(false, false);
        }
        if (status != null) {
            status.close();
        }
        closed = true;
    }

    protected void echoSignal(Signal signal) {
        ControlChar cc = null;
        switch (signal) {
            case INT:
                cc = ControlChar.VINTR;
                break;
            case QUIT:
                cc = ControlChar.VQUIT;
                break;
            case TSTP:
                cc = ControlChar.VSUSP;
                break;
        }
        if (cc != null) {
            int vcc = getAttributes().getControlChar(cc);
            if (vcc > 0 && vcc < 32) {
                writer().write(new char[] {'^', (char) (vcc + '@')}, 0, 2);
            }
        }
    }

    public Attributes enterRawMode() {
        Attributes prvAttr = getAttributes();
        Attributes newAttr = new Attributes(prvAttr);
        newAttr.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO, LocalFlag.IEXTEN), false);
        newAttr.setInputFlags(EnumSet.of(InputFlag.IXON, InputFlag.ICRNL, InputFlag.INLCR), false);
        newAttr.setControlChar(ControlChar.VMIN, 0);
        newAttr.setControlChar(ControlChar.VTIME, 1);
        setAttributes(newAttr);
        return prvAttr;
    }

    public boolean echo() {
        return getAttributes().getLocalFlag(LocalFlag.ECHO);
    }

    public boolean echo(boolean echo) {
        Attributes attr = getAttributes();
        boolean prev = attr.getLocalFlag(LocalFlag.ECHO);
        if (prev != echo) {
            attr.setLocalFlag(LocalFlag.ECHO, echo);
            setAttributes(attr);
        }
        return prev;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getKind() {
        return getClass().getSimpleName();
    }

    @Override
    public Charset encoding() {
        return this.encoding;
    }

    @Override
    public Charset inputEncoding() {
        return this.inputEncoding;
    }

    @Override
    public Charset outputEncoding() {
        return this.outputEncoding;
    }

    public void flush() {
        writer().flush();
    }

    public boolean puts(Capability capability, Object... params) {
        String str = getStringCapability(capability);
        if (str == null) {
            return false;
        }
        Curses.tputs(writer(), str, params);
        return true;
    }

    public boolean getBooleanCapability(Capability capability) {
        return bools.contains(capability);
    }

    public Integer getNumericCapability(Capability capability) {
        return ints.get(capability);
    }

    public String getStringCapability(Capability capability) {
        return strings.get(capability);
    }

    protected void parseInfoCmp() {
        String capabilities = null;
        try {
            capabilities = InfoCmp.getInfoCmp(type);
        } catch (Exception e) {
            Log.warn("Unable to retrieve infocmp for type " + type, e);
        }
        if (capabilities == null) {
            capabilities = InfoCmp.getDefaultInfoCmp("ansi");
        }
        InfoCmp.parseInfoCmp(capabilities, bools, ints, strings);
        detectTrueColorSupport();
    }

    /**
     * Detects true color support from environment variables and upgrades
     * {@code max_colors} accordingly. Subclasses for remote terminals can
     * override this to check the remote client's environment instead.
     *
     * @see <a href="https://gist.github.com/XVilka/8346728#true-color-detection">True Color detection</a>
     */
    protected void detectTrueColorSupport() {
        Integer maxColors = ints.get(Capability.max_colors);
        if (maxColors != null && maxColors >= 0x7FFF) {
            return; // already true-color capable
        }
        String colorterm = System.getenv("COLORTERM");
        if (colorterm != null) {
            colorterm = colorterm.toLowerCase(java.util.Locale.ROOT);
        }
        if (colorterm != null && (colorterm.contains("truecolor") || colorterm.contains("24bit"))) {
            ints.put(Capability.max_colors, AttributedCharSequence.TRUE_COLORS);
        } else if (type != null && type.contains("-direct")) {
            ints.put(Capability.max_colors, AttributedCharSequence.TRUE_COLORS);
        }
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return null;
    }

    private MouseEvent lastMouseEvent = new MouseEvent(
            MouseEvent.Type.Moved, MouseEvent.Button.NoButton, EnumSet.noneOf(MouseEvent.Modifier.class), 0, 0);

    @Override
    public boolean hasMouseSupport() {
        return MouseSupport.hasMouseSupport(this);
    }

    @Override
    public MouseTracking getCurrentMouseTracking() {
        return currentMouseTracking;
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        if (MouseSupport.trackMouse(this, tracking)) {
            currentMouseTracking = tracking;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public MouseEvent readMouseEvent() {
        return readMouseEvent(getStringCapability(Capability.key_mouse));
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader) {
        return readMouseEvent(reader, getStringCapability(Capability.key_mouse));
    }

    @Override
    public MouseEvent readMouseEvent(String prefix) {
        return lastMouseEvent = MouseSupport.readMouse(this, lastMouseEvent, prefix);
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier reader, String prefix) {
        return lastMouseEvent = MouseSupport.readMouse(reader, lastMouseEvent, prefix);
    }

    @Override
    public boolean hasFocusSupport() {
        return type.startsWith("xterm");
    }

    @Override
    public boolean trackFocus(boolean tracking) {
        if (hasFocusSupport()) {
            writer().write(tracking ? "\033[?1004h" : "\033[?1004l");
            writer().flush();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean supportsGraphemeClusterMode() {
        if (graphemeClusterModeSupported == null) {
            graphemeClusterModeSupported = probeGraphemeClusterMode();
        }
        return graphemeClusterModeSupported;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getGraphemeClusterMode() {
        return graphemeClusterModeEnabled;
    }

    /**
     * Tests whether the terminal groups the grapheme cluster starting at
     * {@code index} as a single display unit. Used by {@link org.jline.utils.WCWidth}
     * for per-category width computation when partial emoji support is detected.
     *
     * @param cs the character sequence
     * @param index the start index of the cluster
     * @param charCount the number of Java chars in the cluster
     * @return {@code true} if the terminal renders the cluster as a single glyph
     */
    public boolean isClusterGrouped(CharSequence cs, int index, int charCount) {
        if (!graphemeClusterModeEnabled) {
            return false;
        }
        if (groupsRegionalIndicators && groupsZwjSequences) {
            return true;
        }
        if (!groupsRegionalIndicators && !groupsZwjSequences) {
            return false;
        }
        // Partial support — classify and check
        if (charCount <= Character.charCount(Character.codePointAt(cs, index))) {
            return false; // Single codepoint
        }
        int cp = Character.codePointAt(cs, index);
        if (WCWidth.isRegionalIndicator(cp)) {
            return groupsRegionalIndicators;
        }
        return groupsZwjSequences;
    }

    /**
     * Probes the terminal for grapheme cluster support.
     *
     * <p>First attempts Mode 2027 detection via DECRQM ({@link #probeMode2027()}).
     * If the terminal responds but does not support Mode 2027, falls back to a
     * cursor position probe ({@link #probeCursorPosition()}): writes test
     * emoji (a flag and a ZWJ sequence) and measures cursor displacement via
     * DSR/CPR. If the cursor advances by exactly 2 columns, the terminal
     * natively groups that emoji category as a single cluster.</p>
     *
     * <p>The cursor probe is only attempted when the terminal has already
     * responded to the DECRQM/DA1 query, which guarantees it handles escape
     * sequences and avoids blocking indefinitely on an unresponsive terminal.</p>
     *
     * @return {@code true} if the terminal supports grapheme clusters
     */
    private boolean probeGraphemeClusterMode() {
        if (TYPE_DUMB.equals(type) || TYPE_DUMB_COLOR.equals(type)) {
            return false;
        }
        Attributes prev = getAttributes();
        Attributes probeAttrs = new Attributes(prev);
        probeAttrs.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO), false);
        probeAttrs.setControlChar(ControlChar.VMIN, 0);
        probeAttrs.setControlChar(ControlChar.VTIME, 0);
        setAttributes(probeAttrs);
        try {
            ProbeResult mode2027 = probeMode2027();
            if (mode2027 == ProbeResult.SUPPORTED) {
                return true;
            }
            // Cursor probe is only safe when the terminal actually responded to the
            // DECRQM/DA1 query; otherwise getCursorPosition would block indefinitely.
            if (mode2027 == ProbeResult.NOT_SUPPORTED) {
                return probeCursorPosition();
            }
            return false;
        } finally {
            long drainTimeout = getLongProperty(TerminalBuilder.PROP_DRAIN_TIMEOUT, 25);
            drainInput(reader(), drainTimeout, -1);
            setAttributes(prev);
        }
    }

    /**
     * Probes the terminal for mode 2027 support using DECRQM.
     *
     * <p>Sends {@code CSI ? 2027 $ p} followed by a DA1 (Primary Device
     * Attributes) query {@code CSI c} as a sentinel.  DA1 is near-universally
     * supported, so its response acts as a fence: if we receive the DA1
     * response without a preceding DECRPM, the terminal does not support
     * DECRQM and we return immediately instead of waiting for a timeout.</p>
     *
     * <p>The expected DECRPM response is {@code CSI ? 2027 ; Ps $ y} where
     * Ps indicates the mode status.  Both DECRPM and DA1 responses share the
     * {@code CSI ?} prefix, but diverge immediately after ({@code 2027;}
     * vs the DA1 device-type parameter), so they are easy to distinguish.</p>
     *
     * <p>macOS Terminal.app is explicitly skipped because its CSI parser does
     * not handle the {@code $} intermediate byte and leaks the final {@code p}
     * as visible text. A {@code false} (not {@code null}) is returned so the
     * cursor position fallback is still attempted.</p>
     *
     * @return {@link ProbeResult#SUPPORTED} if mode 2027 is supported,
     *         {@link ProbeResult#NOT_SUPPORTED} if the terminal responded but
     *         does not support it, or {@link ProbeResult#NO_RESPONSE} if the
     *         terminal did not respond at all
     */
    private ProbeResult probeMode2027() {
        // Terminal.app's CSI parser does not handle intermediate bytes correctly
        // and leaks the final byte 'p' of the DECRQM sequence as visible text.
        // Skip DECRQM but return NOT_SUPPORTED (not NO_RESPONSE) so the cursor probe runs.
        String termProgram = System.getenv("TERM_PROGRAM");
        if ("Apple_Terminal".equals(termProgram)) {
            return ProbeResult.NOT_SUPPORTED;
        }
        // Send DECRQM query for mode 2027 followed by DA1 as sentinel.
        // readTerminalResponse() reads until the DA1 terminator 'c', capturing
        // both the DECRPM (ESC[?2027;Ps$y) and the DA1 response.
        writer().write("\033[?2027$p\033[c");
        writer().flush();
        String response = readTerminalResponse();
        if (response == null) {
            return ProbeResult.NO_RESPONSE;
        }
        // DECRPM: ESC[?2027;Ps$y where Ps: 1=set, 2=reset (can be set), 3=permanently set
        if (response.contains("\033[?2027;1$y")
                || response.contains("\033[?2027;2$y")
                || response.contains("\033[?2027;3$y")) {
            return ProbeResult.SUPPORTED;
        }
        return ProbeResult.NOT_SUPPORTED;
    }

    /**
     * Probes per-category emoji grouping by measuring cursor displacement.
     *
     * <p>Writes a test emoji (a ZWJ sequence) followed by a DSR cursor-position
     * query. After flushing, reads the CPR response and checks whether the
     * cursor advanced by exactly 2 columns (meaning the terminal grouped
     * the emoji as a single cluster).</p>
     *
     * <p>Some terminals group all emoji categories (flags + ZWJ), others
     * only group regional indicators (flags). Two probes detect this.</p>
     *
     * <p>This method must only be called when the terminal is known to
     * respond to escape sequences (i.e., after {@link #probeMode2027()}
     * received a response), because DSR/CPR blocks until a response
     * arrives.</p>
     *
     * @return {@code true} if the terminal groups the test emoji
     */
    private boolean probeCursorPosition() {
        // Need u6 (CPR response pattern), u7 (DSR query), and save/restore cursor
        if (getStringCapability(Capability.user6) == null
                || getStringCapability(Capability.user7) == null
                || getStringCapability(Capability.save_cursor) == null
                || getStringCapability(Capability.restore_cursor) == null) {
            return false;
        }
        // Two probes cover the three observed terminal behaviours:
        //   1. Full support   → both flag and ZWJ grouped
        //   2. Flags only     → flag grouped, ZWJ not (e.g. Tabby, Alacritty)
        //   3. No support     → neither grouped
        // When ZWJ is grouped we assume skin-tone and VS16 are too (always
        // observed together).  A flag probe alone catches partial support.
        String flagEmoji = "\uD83C\uDDEB\uD83C\uDDF7"; // 🇫🇷 French flag
        String zwjEmoji = "\uD83D\uDC69\u200D\uD83D\uDD2C"; // 👩‍🔬 woman scientist
        long timeout = getLongProperty(TerminalBuilder.PROP_PROBE_TIMEOUT, 200);
        try {
            // Query current cursor column so we can compute displacement
            // rather than relying on the cursor starting at column 1
            puts(Capability.user7);
            writer().flush();
            int startCol = readCprColumn(timeout);
            if (startCol < 0) {
                return false;
            }

            // --- Flag probe ---
            int flagCol = probeEmojiWidth(flagEmoji, startCol, timeout);
            // --- ZWJ probe ---
            int zwjCol = probeEmojiWidth(zwjEmoji, startCol, timeout);

            // Grouped emoji → 2-column displacement; ungrouped → 4
            groupsRegionalIndicators = (flagCol - startCol == 2);
            groupsZwjSequences = (zwjCol - startCol == 2);

            if (groupsRegionalIndicators || groupsZwjSequences) {
                graphemeClusterNative = true;
                graphemeClusterModeEnabled = true;
                return true;
            }
            return false;
        } catch (IOError | IOException e) {
            try {
                puts(Capability.restore_cursor);
                puts(Capability.clr_eol);
                writer().flush();
            } catch (Exception ignored) {
                // Best-effort cleanup; probing failure should not propagate
            }
            groupsRegionalIndicators = false;
            groupsZwjSequences = false;
            return false;
        }
    }

    /**
     * Writes a single emoji to the terminal, queries cursor position via
     * DSR/CPR, then cleans up. Returns the 1-based column value.
     */
    private int probeEmojiWidth(String emoji, int startCol, long timeout) throws IOException {
        PrintWriter out = writer();
        puts(Capability.save_cursor);
        out.write(emoji);
        puts(Capability.user7); // DSR — request cursor position
        out.flush();

        int col = readCprColumn(timeout);

        // Erase the probe glyphs: rewind, overwrite the occupied cells with
        // spaces, then rewind again to leave the cursor in the original saved position.
        puts(Capability.restore_cursor);
        int width = col - startCol;
        if (width > 0) {
            for (int i = 0; i < width; i++) {
                out.write(' ');
            }
            puts(Capability.restore_cursor);
        } else {
            puts(Capability.clr_eol);
        }
        out.flush();
        return col;
    }

    /**
     * Reads a single CPR (Cursor Position Report) response and extracts
     * the column value.
     *
     * <p>Expected format: {@code ESC [ row ; col R}. Returns the raw
     * 1-based column value, or {@code -1} if the response could not be
     * parsed.</p>
     */
    private int readCprColumn(long timeout) throws IOException {
        NonBlockingReader in = reader();
        int c;
        // Skip until ESC
        while ((c = in.read(timeout)) != '\033') {
            if (c < 0) return -1;
        }
        if (in.read(timeout) != '[') return -1;
        // Skip row digits until ';'
        while ((c = in.read(timeout)) != ';') {
            if (c < 0 || c == 'R') return -1;
        }
        // Read column digits until 'R'
        int col = 0;
        while ((c = in.read(timeout)) != 'R') {
            if (c < '0' || c > '9') return -1;
            col = col * 10 + (c - '0');
        }
        return col;
    }

    static long getLongProperty(String key, long defaultValue) {
        try {
            return Math.max(0L, Long.parseLong(System.getProperty(key, Long.toString(defaultValue))));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    String readTerminalResponse() {
        long initialTimeout = getLongProperty(TerminalBuilder.PROP_PROBE_TIMEOUT, 200);
        long subsequentTimeout = getLongProperty(TerminalBuilder.PROP_DRAIN_TIMEOUT, 25);
        NonBlockingReader in = reader();
        try {
            StringBuilder response = new StringBuilder();
            long deadline = System.currentTimeMillis() + initialTimeout;
            long timeout = initialTimeout;
            int c;

            while ((c = in.read(timeout)) >= 0) {
                response.append((char) c);

                // Complete DA1 response: ESC[?...c or ESC[...c
                String responseStr = response.toString();
                if (responseStr.contains("\033[") && responseStr.endsWith("c")) {
                    return responseStr;
                }

                if (response.length() > 200) break;

                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                timeout = Math.min(subsequentTimeout, remaining);
            }

            return response.length() > 0 ? response.toString() : null;
        } catch (IOException ignored) {
            // Best-effort read; errors are expected when the stream is closed
            return null;
        }
    }

    static void drainInput(NonBlockingReader reader, long overallTimeoutMs, int stopChar) {
        try {
            long deadline = System.currentTimeMillis() + overallTimeoutMs;
            long remaining;
            while ((remaining = deadline - System.currentTimeMillis()) > 0) {
                int c = reader.read(remaining);
                if (c < 0 || c == stopChar) return;
            }
        } catch (IOException ignored) {
            // Best-effort drain; errors are expected when the stream is closed
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean setGraphemeClusterMode(boolean enable, boolean force) {
        PrintWriter out = writer();
        if (force) {
            graphemeClusterModeSupported = true;
            // If Mode 2027 was active via escape sequences, disable it
            // before switching to native mode
            if (!enable && graphemeClusterModeEnabled && !graphemeClusterNative) {
                out.write("\033[?2027l");
                out.flush();
            }
            graphemeClusterNative = true;
            graphemeClusterModeEnabled = enable;
            groupsRegionalIndicators = true;
            groupsZwjSequences = true;
            return true;
        }
        if (supportsGraphemeClusterMode()) {
            if (!graphemeClusterNative) {
                out.write(enable ? "\033[?2027h" : "\033[?2027l");
                out.flush();
                // Mode 2027 handles all emoji categories
                groupsRegionalIndicators = enable;
                groupsZwjSequences = enable;
            }
            graphemeClusterModeEnabled = enable;
            return true;
        } else {
            return false;
        }
    }

    protected void checkInterrupted() throws InterruptedIOException {
        if (Thread.interrupted()) {
            throw new InterruptedIOException();
        }
    }

    /**
     * Checks if this terminal has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this terminal has been closed
     */
    protected void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Terminal has been closed");
        }
    }

    @Override
    public boolean canPauseResume() {
        return false;
    }

    @Override
    public void pause() {}

    @Override
    public void pause(boolean wait) throws InterruptedException {}

    @Override
    public void resume() {}

    @Override
    public boolean paused() {
        return false;
    }

    @Override
    public ColorPalette getPalette() {
        return palette;
    }

    @Override
    public String toString() {
        return getKind() + "[" + "name='"
                + name + '\'' + ", type='"
                + type + '\'' + ", size='"
                + getSize() + '\'' + ']';
    }

    /**
     * Get the terminal's default foreground color.
     * This method should be overridden by concrete implementations.
     *
     * @return the RGB value of the default foreground color, or -1 if not available
     */
    public int getDefaultForegroundColor() {
        return -1;
    }

    /**
     * Get the terminal's default background color.
     * This method should be overridden by concrete implementations.
     *
     * @return the RGB value of the default background color, or -1 if not available
     */
    public int getDefaultBackgroundColor() {
        return -1;
    }
}
