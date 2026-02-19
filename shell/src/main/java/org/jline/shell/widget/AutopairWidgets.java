/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.widget;

import java.security.InvalidParameterException;
import java.util.*;

import org.jline.keymap.KeyMap;
import org.jline.reader.*;

/**
 * Provides intelligent auto-pairing of matching delimiters in the command line.
 * <p>
 * When enabled, this widget automatically:
 * <ul>
 * <li>Inserts closing delimiters when an opening delimiter is typed
 *     (e.g., typing {@code (} inserts {@code ()})</li>
 * <li>Deletes matching pairs when backspace is pressed between them</li>
 * <li>Skips over closing delimiters when typed and already present</li>
 * </ul>
 * <p>
 * The default paired delimiters are: {@code () [] "" '' `` " "} (space).
 * Curly braces {@code {}} can be optionally enabled.
 * <p>
 * Inspired by <a href="https://github.com/hlissner/zsh-autopair">zsh-autopair</a>.
 * <p>
 * Example:
 * <pre>
 * AutopairWidgets autopair = new AutopairWidgets(reader);
 * autopair.enable();
 * </pre>
 *
 * @since 4.0
 */
public class AutopairWidgets {

    private static final String AUTOPAIR_TOGGLE = "autopair-toggle";
    private static final String TAILTIP_TOGGLE = "tailtip-toggle";
    private static final String AP_INSERT = "_autopair-insert";
    private static final String AP_BACKWARD_DELETE_CHAR = "_autopair-backward-delete-char";
    private static final String TT_ACCEPT_LINE = "_tailtip-accept-line";

    private static final Map<String, String> LBOUNDS;
    private static final Map<String, String> RBOUNDS;

    static {
        LBOUNDS = new HashMap<>();
        LBOUNDS.put("all", "[.:/\\!]");
        LBOUNDS.put("quotes", "[\\]})a-zA-Z0-9]");
        LBOUNDS.put("spaces", "[^{(\\[]");
        LBOUNDS.put("braces", "");
        LBOUNDS.put("`", "`");
        LBOUNDS.put("\"", "\"");
        LBOUNDS.put("'", "'");
        RBOUNDS = new HashMap<>();
        RBOUNDS.put("all", "[\\[{(<,.:?/%$!a-zA-Z0-9]");
        RBOUNDS.put("quotes", "[a-zA-Z0-9]");
        RBOUNDS.put("spaces", "[^\\]})]");
        RBOUNDS.put("braces", "");
        RBOUNDS.put("`", "");
        RBOUNDS.put("\"", "");
        RBOUNDS.put("'", "");
    }

    protected final LineReader reader;
    private final Map<String, String> pairs;
    private final Map<String, Binding> defaultBindings = new HashMap<>();
    private boolean enabled;

    {
        pairs = new HashMap<>();
        pairs.put("`", "`");
        pairs.put("'", "'");
        pairs.put("\"", "\"");
        pairs.put("[", "]");
        pairs.put("(", ")");
        pairs.put(" ", " ");
    }

    /**
     * Creates autopair widgets without curly brace support.
     *
     * @param reader the line reader
     */
    public AutopairWidgets(LineReader reader) {
        this(reader, false);
    }

    /**
     * Creates autopair widgets with optional curly brace support.
     *
     * @param reader           the line reader
     * @param addCurlyBrackets true to include {@code {}} as a paired delimiter
     */
    @SuppressWarnings("this-escape")
    public AutopairWidgets(LineReader reader, boolean addCurlyBrackets) {
        this.reader = reader;
        if (existsWidget(AP_INSERT)) {
            throw new IllegalStateException("AutopairWidgets already created!");
        }
        if (addCurlyBrackets) {
            pairs.put("{", "}");
        }
        addWidget(AP_INSERT, this::autopairInsert);
        addWidget("_autopair-close", this::autopairClose);
        addWidget(AP_BACKWARD_DELETE_CHAR, this::autopairDelete);
        addWidget(AUTOPAIR_TOGGLE, this::toggleKeyBindings);

        KeyMap<Binding> map = getKeyMap();
        for (Map.Entry<String, String> p : pairs.entrySet()) {
            defaultBindings.put(p.getKey(), map.getBound(p.getKey()));
            if (!p.getKey().equals(p.getValue())) {
                defaultBindings.put(p.getValue(), map.getBound(p.getValue()));
            }
        }
    }

    /**
     * Enables auto-pairing if not already enabled.
     */
    public void enable() {
        if (!enabled) {
            toggle();
        }
    }

    /**
     * Disables auto-pairing if currently enabled.
     */
    public void disable() {
        if (enabled) {
            toggle();
        }
    }

    /**
     * Toggles auto-pairing on or off.
     *
     * @return true if auto-pairing is now enabled
     */
    public boolean toggle() {
        boolean before = enabled;
        toggleKeyBindings();
        return !before;
    }

    // -- widget methods --

    public boolean autopairInsert() {
        if (pairs.containsKey(lastBinding())) {
            if (canSkip(lastBinding())) {
                callWidget(LineReader.FORWARD_CHAR);
            } else if (canPair(lastBinding())) {
                callWidget(LineReader.SELF_INSERT);
                putString(pairs.get(lastBinding()));
                callWidget(LineReader.BACKWARD_CHAR);
            } else {
                callWidget(LineReader.SELF_INSERT);
            }
        } else {
            callWidget(LineReader.SELF_INSERT);
        }
        return true;
    }

    public boolean autopairClose() {
        if (pairs.containsValue(lastBinding()) && currChar().equals(lastBinding())) {
            callWidget(LineReader.FORWARD_CHAR);
        } else {
            callWidget(LineReader.SELF_INSERT);
        }
        return true;
    }

    public boolean autopairDelete() {
        if (pairs.containsKey(prevChar()) && pairs.get(prevChar()).equals(currChar()) && canDelete(prevChar())) {
            callWidget(LineReader.DELETE_CHAR);
        }
        callWidget(LineReader.BACKWARD_DELETE_CHAR);
        return true;
    }

    public boolean toggleKeyBindings() {
        if (enabled) {
            defaultBindings();
        } else {
            customBindings();
        }
        return enabled;
    }

    // -- key binding management --

    private void customBindings() {
        boolean ttActive = tailtipEnabled();
        if (ttActive) {
            callWidget(TAILTIP_TOGGLE);
        }
        KeyMap<Binding> map = getKeyMap();
        for (Map.Entry<String, String> p : pairs.entrySet()) {
            map.bind(new Reference(AP_INSERT), p.getKey());
            if (!p.getKey().equals(p.getValue())) {
                map.bind(new Reference("_autopair-close"), p.getValue());
            }
        }
        aliasWidget(AP_BACKWARD_DELETE_CHAR, LineReader.BACKWARD_DELETE_CHAR);
        if (ttActive) {
            callWidget(TAILTIP_TOGGLE);
        }
        enabled = true;
    }

    private void defaultBindings() {
        KeyMap<Binding> map = getKeyMap();
        for (Map.Entry<String, String> p : pairs.entrySet()) {
            map.bind(defaultBindings.get(p.getKey()), p.getKey());
            if (!p.getKey().equals(p.getValue())) {
                map.bind(defaultBindings.get(p.getValue()), p.getValue());
            }
        }
        aliasWidget("." + LineReader.BACKWARD_DELETE_CHAR, LineReader.BACKWARD_DELETE_CHAR);
        if (tailtipEnabled()) {
            callWidget(TAILTIP_TOGGLE);
            callWidget(TAILTIP_TOGGLE);
        }
        enabled = false;
    }

    // -- helpers --

    private boolean tailtipEnabled() {
        return getWidget(LineReader.ACCEPT_LINE).equals(TT_ACCEPT_LINE);
    }

    private boolean canPair(String d) {
        if (balanced(d) && !nextToBoundary(d)) {
            return !d.equals(" ") || (!prevChar().equals(" ") && !currChar().equals(" "));
        }
        return false;
    }

    private boolean canSkip(String d) {
        return pairs.get(d).equals(d) && d.charAt(0) != ' ' && currChar().equals(d) && balanced(d);
    }

    private boolean canDelete(String d) {
        return balanced(d);
    }

    private boolean balanced(String d) {
        boolean out = false;
        Buffer buf = buffer();
        String lbuf = buf.upToCursor();
        String rbuf = buf.substring(lbuf.length());
        String regx1 = pairs.get(d).equals(d) ? d : "\\" + d;
        String regx2 = pairs.get(d).equals(d) ? pairs.get(d) : "\\" + pairs.get(d);
        int llen = lbuf.length() - lbuf.replaceAll(regx1, "").length();
        int rlen = rbuf.length() - rbuf.replaceAll(regx2, "").length();
        if (llen == 0 && rlen == 0) {
            out = true;
        } else if (d.charAt(0) == ' ') {
            out = true;
        } else if (pairs.get(d).equals(d)) {
            if (llen == rlen || (llen + rlen) % 2 == 0) {
                out = true;
            }
        } else {
            int l2len = lbuf.length() - lbuf.replaceAll(regx2, "").length();
            int r2len = rbuf.length() - rbuf.replaceAll(regx1, "").length();
            int ltotal = llen - l2len;
            int rtotal = rlen - r2len;
            if (ltotal < 0) {
                ltotal = 0;
            }
            if (ltotal >= rtotal) {
                out = true;
            }
        }
        return out;
    }

    private boolean boundary(String lb, String rb) {
        return (lb.length() > 0 && prevChar().matches(lb))
                || (rb.length() > 0 && currChar().matches(rb));
    }

    private boolean nextToBoundary(String d) {
        List<String> bk = new ArrayList<>();
        bk.add("all");
        if (d.matches("['\"`]")) {
            bk.add("quotes");
        } else if (d.matches("[{\\[(<]")) {
            bk.add("braces");
        } else if (d.charAt(0) == ' ') {
            bk.add("spaces");
        }
        if (LBOUNDS.containsKey(d) && RBOUNDS.containsKey(d)) {
            bk.add(d);
        }
        for (String k : bk) {
            if (boundary(LBOUNDS.get(k), RBOUNDS.get(k))) {
                return true;
            }
        }
        return false;
    }

    // -- utility methods (replicated from Widgets base class) --

    private void addWidget(String name, Widget widget) {
        reader.getWidgets().put(name, namedWidget(name, widget));
    }

    private Widget namedWidget(final String name, final Widget widget) {
        return new Widget() {
            @Override
            public String toString() {
                return name;
            }

            @Override
            public boolean apply() {
                return widget.apply();
            }
        };
    }

    private void callWidget(String name) {
        if (!name.startsWith("_") && !name.endsWith("-toggle")) {
            name = "." + name;
        }
        reader.callWidget(name);
    }

    private void aliasWidget(String orig, String alias) {
        reader.getWidgets().put(alias, widget(orig));
    }

    private String getWidget(String name) {
        return widget(name).toString();
    }

    private boolean existsWidget(String name) {
        try {
            widget(name);
            return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private Widget widget(String name) {
        Widget out;
        if (name.startsWith(".")) {
            out = reader.getBuiltinWidgets().get(name.substring(1));
        } else {
            out = reader.getWidgets().get(name);
        }
        if (out == null) {
            throw new InvalidParameterException("widget: no such widget " + name);
        }
        return out;
    }

    private KeyMap<Binding> getKeyMap() {
        return reader.getKeyMaps().get(LineReader.MAIN);
    }

    private Buffer buffer() {
        return reader.getBuffer();
    }

    private String prevChar() {
        return String.valueOf((char) reader.getBuffer().prevChar());
    }

    private String currChar() {
        return String.valueOf((char) reader.getBuffer().currChar());
    }

    private String lastBinding() {
        return reader.getLastBinding();
    }

    private void putString(String string) {
        reader.getBuffer().write(string);
    }
}
