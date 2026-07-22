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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.UnaryOperator;

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
    protected UnaryOperator<String> envProvider = System::getenv;
    protected Status status;
    protected Runnable onClose;
    protected MouseTracking currentMouseTracking = MouseTracking.Off;
    protected volatile boolean closed = false;

    // ---- Mode probe results ----
    private final EnumMap<Mode, ProbeResult> modeProbeResults = new EnumMap<>(Mode.class);
    private volatile boolean modesProbed;

    // ---- Grapheme cluster state (mode 2027 + cursor-position fallback) ----
    private Boolean graphemeClusterModeSupported;
    private boolean graphemeClusterModeEnabled;
    private boolean graphemeClusterNative;
    private boolean groupsRegionalIndicators;
    private boolean groupsZwjSequences;
    private boolean kittyKeyboardActive;

    /** CSI prefix for DEC private mode sequences ({@code ESC [ ?}). */
    static final String CSI_DEC = "\033[?";

    /** Result of a terminal mode probe. */
    enum ProbeResult {
        /** Mode is supported (DECRPM Ps = 1, 2, or 3; or DA1/protocol-specific positive). */
        SUPPORTED,
        /** Terminal responded but does not support the mode. */
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

    @Override
    public String getenv(String name) {
        return envProvider.apply(name);
    }

    /**
     * Sets the environment variable provider for this terminal and re-detects
     * environment-dependent capabilities (e.g., true-color support).
     *
     * <p>This is called by {@link TerminalBuilder} when a custom environment
     * has been configured, for example to inject the SSH client's environment
     * into a remote terminal.</p>
     *
     * @param env a function that returns the value of an environment variable
     *            given its name, or {@code null} if not defined
     */
    public void setEnv(UnaryOperator<String> env) {
        this.envProvider = Objects.requireNonNull(env);
        detectTrueColorSupport();
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
        if (kittyKeyboardActive) {
            resetKittyKeyboardMode();
        }
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
        // POSIX cfmakeraw(3) also clears ISIG so Ctrl+C/Ctrl+\/Ctrl+Z arrive as 0x03/0x1c/0x1a
        // characters rather than as signals. This is required for raw-mode readers (for example,
        // prompters in the jline-prompt module) whose keymaps bind those bytes to CANCEL/INTERRUPT operations.
        newAttr.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO, LocalFlag.IEXTEN, LocalFlag.ISIG), false);
        newAttr.setInputFlags(EnumSet.of(InputFlag.IXON, InputFlag.ICRNL, InputFlag.INLCR), false);
        // POSIX cfmakeraw(3) defaults — VMIN=0/VTIME=1 made FileInputStream.read() see EOF on every 100 ms idle tick.
        newAttr.setControlChar(ControlChar.VMIN, 1);
        newAttr.setControlChar(ControlChar.VTIME, 0);
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
        String colorterm = getenv("COLORTERM");
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

    // ---- Terminal mode batch probing ----

    @Override
    public boolean isModeSupported(Mode mode) {
        ensureModesProbed();
        synchronized (modeProbeResults) {
            return modeProbeResults.getOrDefault(mode, ProbeResult.NO_RESPONSE) == ProbeResult.SUPPORTED;
        }
    }

    /**
     * Ensures all terminal modes have been probed.
     *
     * <p>On first call, sends a single batch query containing a Kitty
     * keyboard query ({@code CSI ? u}), a DECRQM query for every
     * DEC private {@link Mode} value, and a DA1 sentinel ({@code CSI c}).
     * The combined response is parsed once, populating
     * {@link #modeProbeResults}. Subsequent calls are no-ops.</p>
     *
     * <p>macOS Terminal.app is explicitly skipped because its CSI parser
     * does not handle the {@code $} intermediate byte and leaks the
     * final {@code p} as visible text. All modes are marked
     * {@link ProbeResult#NOT_SUPPORTED} so mode-specific fallbacks (e.g.
     * the cursor position probe for grapheme clusters) can still run.</p>
     *
     * <p>Thread-safe: the {@code modesProbed} flag is volatile and the
     * map is populated under a lock so concurrent callers never observe
     * a partially filled map.</p>
     */
    private void ensureModesProbed() {
        if (modesProbed) {
            return;
        }
        synchronized (modeProbeResults) {
            if (modesProbed) {
                return;
            }
            try {
                if (TYPE_DUMB.equals(type) || TYPE_DUMB_COLOR.equals(type)) {
                    return; // map stays empty → all modes default to NO_RESPONSE
                }

                // Terminal.app's CSI parser does not handle intermediate bytes correctly
                // and leaks the final byte 'p' of the DECRQM sequence as visible text.
                // Mark all modes NOT_SUPPORTED (not NO_RESPONSE) so fallbacks can run.
                String termProgram = getenv("TERM_PROGRAM");
                if ("Apple_Terminal".equals(termProgram)) {
                    for (Mode m : Mode.values()) {
                        modeProbeResults.put(m, ProbeResult.NOT_SUPPORTED);
                    }
                    return;
                }

                Attributes prev = getAttributes();
                Attributes probeAttrs = new Attributes(prev);
                probeAttrs.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO), false);
                probeAttrs.setControlChar(ControlChar.VMIN, 0);
                probeAttrs.setControlChar(ControlChar.VTIME, 0);
                setAttributes(probeAttrs);
                try {
                    probeModes();
                } finally {
                    long drainTimeout = getLongProperty(TerminalBuilder.PROP_DRAIN_TIMEOUT, 25);
                    drainInput(reader(), drainTimeout, -1);
                    setAttributes(prev);
                }
            } finally {
                modesProbed = true;
            }
        }
    }

    /**
     * Sends a batch query for all {@link Mode} values, terminated by a
     * DA1 sentinel.
     *
     * <p>DEC private modes (those with {@code mode() > 0}) are queried
     * via DECRQM ({@code CSI ? Pd $ p}). The Kitty Keyboard Protocol
     * ({@code mode() == 0}) is queried via {@code CSI ? u}. The query
     * takes the form:
     * {@code CSI ? u  CSI ? 2026 $ p  CSI ? 2027 $ p  CSI ? 2048 $ p  CSI c}
     * </p>
     *
     * <p>DA1 is near-universally supported, so its response acts as a
     * fence: if we receive the DA1 response without any preceding DECRPM,
     * the terminal does not support DECRQM and all modes are marked
     * {@link ProbeResult#NOT_SUPPORTED}.</p>
     */
    private void probeModes() {
        // Build batch query: Kitty keyboard + all DECRQM + DA1 sentinel
        StringBuilder query = new StringBuilder();
        query.append(CSI_DEC).append("u"); // Kitty keyboard query
        for (Mode m : Mode.values()) {
            if (m.mode() > 0) {
                query.append(CSI_DEC).append(m.mode()).append("$p");
            }
        }
        query.append("\033[c"); // DA1 sentinel
        writer().write(query.toString());
        writer().flush();

        String response = readTerminalResponse();
        if (response == null) {
            // Terminal did not respond at all
            return;
        }

        // Parse responses for each mode
        for (Mode m : Mode.values()) {
            if (m == Mode.SIXEL) {
                // DA1 attribute 4 indicates Sixel support
                modeProbeResults.put(
                        m, parseSixelFromDa1(response) ? ProbeResult.SUPPORTED : ProbeResult.NOT_SUPPORTED);
            } else if (m == Mode.KITTY_KEYBOARD) {
                // Kitty keyboard: CSI ? flags u
                modeProbeResults.put(
                        m, parseKittyResponse(response) ? ProbeResult.SUPPORTED : ProbeResult.NOT_SUPPORTED);
            } else {
                // DECRPM: CSI ? mode ; Ps $ y
                modeProbeResults.put(m, parseDecrpm(response, m.mode()));
            }
        }
    }

    /**
     * Parses a DECRPM response for a single mode from the combined
     * terminal response string.
     *
     * <p>Looks for the pattern {@code CSI ? mode ; Ps $ y} where
     * Ps indicates the mode status:
     * <ul>
     *   <li>0 — not recognized</li>
     *   <li>1 — set (enabled)</li>
     *   <li>2 — reset, but can be set</li>
     *   <li>3 — permanently set</li>
     *   <li>4 — permanently reset</li>
     * </ul>
     * Ps values 1, 2, and 3 are considered {@link ProbeResult#SUPPORTED}.
     *
     * @param response the raw terminal response
     * @param mode the DEC private mode number to look for
     * @return the probe result for this mode
     */
    static ProbeResult parseDecrpm(String response, int mode) {
        String prefix = CSI_DEC + mode + ";";
        int idx = response.indexOf(prefix);
        if (idx < 0) {
            return ProbeResult.NOT_SUPPORTED;
        }
        idx += prefix.length();
        // Verify full DECRPM sequence: CSI ? mode ; Ps $ y
        if (idx + 2 < response.length() && response.charAt(idx + 1) == '$' && response.charAt(idx + 2) == 'y') {
            char ps = response.charAt(idx);
            if (ps == '1' || ps == '2' || ps == '3') {
                return ProbeResult.SUPPORTED;
            }
        }
        return ProbeResult.NOT_SUPPORTED;
    }

    /**
     * Parses a Kitty Keyboard Protocol flags response from the combined
     * terminal response string.
     *
     * <p>Looks for the pattern {@code CSI ? digits u}. If found the
     * terminal supports the protocol.</p>
     *
     * @param response the raw terminal response
     * @return {@code true} if a Kitty keyboard flags response was found
     */
    static boolean parseKittyResponse(String response) {
        // Look for CSI ? <digits> u  (but not CSI ? <digits> ; <digit> $ y which is DECRPM)
        int idx = 0;
        while (true) {
            idx = response.indexOf(CSI_DEC, idx);
            if (idx < 0) {
                return false;
            }
            int start = idx + 3; // past "CSI ?"
            int pos = start;
            // Read digits
            while (pos < response.length() && response.charAt(pos) >= '0' && response.charAt(pos) <= '9') {
                pos++;
            }
            // Must have at least one digit and terminate with 'u'
            if (pos > start && pos < response.length() && response.charAt(pos) == 'u') {
                return true;
            }
            idx = start; // advance past this CSI ? and continue searching
        }
    }

    /**
     * Checks whether the DA1 (Primary Device Attributes) response
     * indicates Sixel graphics support.
     *
     * <p>The DA1 response has the format {@code CSI ? Pp ; Ps1 ; Ps2 ; ... c}.
     * The first parameter ({@code Pp}) is the device conformance level, not
     * an attribute. Attribute code {@code 4} among the subsequent parameters
     * means the terminal supports Sixel graphics.</p>
     *
     * @param response the raw terminal response (may contain DECRPM, Kitty, and DA1)
     * @return {@code true} if the DA1 response contains attribute code 4
     */
    static boolean parseSixelFromDa1(String response) {
        // DA1 response: CSI ? Pp ; Ps1 ; ... c
        // The first parameter (Pp) is the device type, NOT an attribute.
        // We look for ";4;" (middle) or ";4c" (last) which indicates
        // attribute 4 (Sixel) as a standalone parameter after the device type.
        return response.contains(";4;") || response.contains(";4c");
    }

    // ---- Grapheme cluster support (mode 2027 + cursor-position fallback) ----

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
     * <p>Uses the batch DEC mode probe to check for mode 2027. If the
     * terminal responds but does not support mode 2027, falls back to a
     * cursor position probe ({@link #probeCursorPosition()}): writes test
     * emoji (a flag and a ZWJ sequence) and measures cursor displacement via
     * DSR/CPR. If the cursor advances by exactly 2 columns, the terminal
     * natively groups that emoji category as a single cluster.</p>
     *
     * <p>The cursor probe is only attempted when the batch probe received
     * a response (i.e. the terminal answered DA1), which guarantees it
     * handles escape sequences and avoids blocking indefinitely.</p>
     *
     * @return {@code true} if the terminal supports grapheme clusters
     */
    private boolean probeGraphemeClusterMode() {
        ensureModesProbed();
        ProbeResult result;
        synchronized (modeProbeResults) {
            result = modeProbeResults.getOrDefault(Mode.GRAPHEME_CLUSTER, ProbeResult.NO_RESPONSE);
        }
        if (result == ProbeResult.SUPPORTED) {
            return true;
        }
        // Cursor probe is only safe when the terminal actually responded to the
        // batch query; otherwise DSR/CPR would block indefinitely.
        if (result == ProbeResult.NOT_SUPPORTED) {
            return probeCursorPositionWithSetup();
        }
        return false;
    }

    /**
     * Runs the cursor-position emoji probe with its own raw-attr setup.
     */
    private boolean probeCursorPositionWithSetup() {
        Attributes prev = getAttributes();
        Attributes probeAttrs = new Attributes(prev);
        probeAttrs.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO), false);
        probeAttrs.setControlChar(ControlChar.VMIN, 0);
        probeAttrs.setControlChar(ControlChar.VTIME, 0);
        setAttributes(probeAttrs);
        try {
            return probeCursorPosition();
        } finally {
            long drainTimeout = getLongProperty(TerminalBuilder.PROP_DRAIN_TIMEOUT, 25);
            drainInput(reader(), drainTimeout, -1);
            setAttributes(prev);
        }
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
        // Use a single wall-clock deadline so the spurious EOF a non-blocking
        // slave-tty read produces while the CPR reply is in flight does not abort
        // the read early (see readProbeChar).
        long deadline = System.currentTimeMillis() + timeout;
        int c;
        // Skip until ESC
        while ((c = readProbeChar(in, deadline)) != '\033') {
            if (c < 0) return -1;
        }
        if (readProbeChar(in, deadline) != '[') return -1;
        // Skip row digits until ';'
        while ((c = readProbeChar(in, deadline)) != ';') {
            if (c < 0 || c == 'R') return -1;
        }
        // Read column digits until 'R'
        int col = 0;
        while ((c = readProbeChar(in, deadline)) != 'R') {
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

    /**
     * Reads the next probe-response character, polling until {@code deadline}.
     *
     * <p>The probe runs with {@code VMIN=0/VTIME=0}, so a slave-tty read with no
     * data yet surfaces as a spurious EOF ({@code -1}) while the reply is still in
     * flight; a read timeout ({@code -2}) is likewise not terminal. Both are
     * treated as "nothing yet" and retried until the deadline, rather than
     * aborting on the first one (which would let the late reply leak to the
     * console once echo is restored).
     *
     * @return a character {@code >= 0}, or {@code -1} once the deadline elapses
     */
    static int readProbeChar(NonBlockingReader in, long deadline) throws IOException {
        long remaining;
        while ((remaining = deadline - System.currentTimeMillis()) > 0) {
            int c = in.read(remaining);
            if (c >= 0) {
                return c;
            }
            // EOF (-1) or READ_EXPIRED (-2): no data yet, pace the poll and retry.
            try {
                Thread.sleep(Math.min(2, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        return -1;
    }

    String readTerminalResponse() {
        long initialTimeout = getLongProperty(TerminalBuilder.PROP_PROBE_TIMEOUT, 200);
        NonBlockingReader in = reader();
        long deadline = System.currentTimeMillis() + initialTimeout;
        StringBuilder response = new StringBuilder();
        try {
            int c;
            while ((c = readProbeChar(in, deadline)) >= 0) {
                response.append((char) c);

                // Complete DA1 response: ESC[?...c or ESC[...c
                String responseStr = response.toString();
                if (responseStr.contains("\033[") && responseStr.endsWith("c")) {
                    return responseStr;
                }

                if (response.length() > 200) break;
            }
        } catch (IOException ignored) {
            // Best-effort read; errors are expected when the stream is closed
        }
        return response.length() > 0 ? response.toString() : null;
    }

    static void drainInput(NonBlockingReader reader, long overallTimeoutMs, int stopChar) {
        try {
            long deadline = System.currentTimeMillis() + overallTimeoutMs;
            int c;
            while ((c = readProbeChar(reader, deadline)) >= 0) {
                if (c == stopChar) return;
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

    // ---- Kitty Keyboard Protocol ----

    @Override
    public boolean hasKittyKeyboardSupport() {
        return isModeSupported(Mode.KITTY_KEYBOARD);
    }

    @Override
    public boolean setKittyKeyboardMode(EnumSet<KittyKeyboardMode> modes) {
        if (hasKittyKeyboardSupport()) {
            if (kittyKeyboardActive) {
                // Pop previous flags first to keep the terminal's stack balanced
                writer().write(KittyKeyboardSupport.popFlags());
            }
            writer().write(KittyKeyboardSupport.pushFlags(modes));
            writer().flush();
            kittyKeyboardActive = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean resetKittyKeyboardMode() {
        if (kittyKeyboardActive) {
            writer().write(KittyKeyboardSupport.popFlags());
            writer().flush();
            kittyKeyboardActive = false;
            return true;
        }
        return false;
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
