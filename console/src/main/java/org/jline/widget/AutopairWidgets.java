/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;

/**
 * Creates and manages widgets that auto-closes, deletes and skips over matching delimiters intelligently.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class AutopairWidgets extends Widgets {
    /*
     *  Inspired by zsh-autopair
     *  https://github.com/hlissner/zsh-autopair
     */
    private static final Map<String, String> LBOUNDS;
    private static final Map<String, String> RBOUNDS;
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

    public AutopairWidgets(LineReader reader) {
        this(reader, false);
    }

    public AutopairWidgets(LineReader reader, boolean addCurlyBrackets) {
        super(reader);
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

    public void enable() {
        if (!enabled) {
            toggle();
        }
    }

    public void disable() {
        if (enabled) {
            toggle();
        }
    }

    public boolean toggle() {
        boolean before = enabled;
        toggleKeyBindings();
        return !before;
    }

    /*
     * Widgets
     */
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
    /*
     * key bindings...
     *
     */
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
    /*
     * helpers
     */
    private boolean tailtipEnabled() {
        return getWidget(LineReader.ACCEPT_LINE).equals(TT_ACCEPT_LINE);
    }

    private boolean canPair(String d) {
        if (balanced(d) && !nexToBoundary(d)) {
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

    private boolean nexToBoundary(String d) {
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
}
