/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Terminal wrapper with default ansi capabilities
 */
public class DefaultTerminal2 implements Terminal2 {

    private final Terminal terminal;
    private final Map<String, String> strings = new HashMap<String, String>();
    private final Map<String, Boolean> bools = new HashMap<String, Boolean>();

    public DefaultTerminal2(Terminal terminal) {
        this.terminal = terminal;
        registerCap("^H", "key_backspace", "kbs", "kb");
        registerCap("^G", "bell", "bel", "bl");
        registerCap("^M", "carriage_return", "cr");
        if (isSupported() && isAnsiSupported()) {
            registerCap("\\E[K", "clr_eol", "el", "ce");
            registerCap("\\E[1K", "clr_bol", "el1", "cb");
            registerCap("\\E[A", "cursor_up", "cuu1", "up");
            registerCap("^J", "cursor_down", "cud1", "do");
            registerCap("\\E[%i%p1%dG", "column_address", "hpa", "ch");
            registerCap("\\E[H\\E[2J", "clear_screen", "clear", "cl");
            registerCap("\\E[%p1%dB", "parm_down_cursor", "cud", "DO");
            registerCap("^H", "cursor_left", "cub1", "le");
            registerCap("\\E[C", "cursor_right", "cuf1", "nd");
        }
        if (hasWeirdWrap()) {
            registerCap(true, "eat_newline_glitch", "xenl", "xn");
            registerCap(true, "auto_right_margin", "am");
        }
    }

    public void init() throws Exception {
        terminal.init();
    }

    public void restore() throws Exception {
        terminal.restore();
    }

    public void reset() throws Exception {
        terminal.reset();
    }

    public boolean isSupported() {
        return terminal.isSupported();
    }

    public int getWidth() {
        return terminal.getWidth();
    }

    public int getHeight() {
        return terminal.getHeight();
    }

    public boolean isAnsiSupported() {
        return terminal.isAnsiSupported();
    }

    public OutputStream wrapOutIfNeeded(OutputStream out) {
        return terminal.wrapOutIfNeeded(out);
    }

    public InputStream wrapInIfNeeded(InputStream in) throws IOException {
        return terminal.wrapInIfNeeded(in);
    }

    public boolean hasWeirdWrap() {
        return terminal.hasWeirdWrap();
    }

    public boolean isEchoEnabled() {
        return terminal.isEchoEnabled();
    }

    public void setEchoEnabled(boolean enabled) {
        terminal.setEchoEnabled(enabled);
    }

    public String getOutputEncoding() {
        return terminal.getOutputEncoding();
    }

    private void registerCap(String value, String... keys) {
        for (String key : keys) {
            strings.put(key, value);
        }
    }

    private void registerCap(boolean value, String... keys) {
        for (String key : keys) {
            bools.put(key, value);
        }
    }

    public String getStringCapability(String capability) {
        return strings.get(capability);
    }

    public int getNumericCapability(String capability) {
        return 0;
    }

    public boolean getBooleanCapability(String capability) {
        Boolean b = bools.get(capability);
        return b != null && b;
    }
}
