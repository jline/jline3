/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import static org.jline.keymap.KeyMap.del;
import static org.jline.keymap.KeyMap.ctrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.BufferImpl;

public abstract class Widgets {
    private final LineReader reader;

    public Widgets(LineReader reader) {
        this.reader = reader;
    }

    public void addWidget(String name, Widget widget) {
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

    public void callWidget(String name) {
        reader.callWidget(name);
    }

    public KeyMap<Binding> getKeyMap(String name) {
        return reader.getKeyMaps().get(name);
    }

    public Buffer buffer() {
        return reader.getBuffer();
    }

    public void replaceBuffer(Buffer buffer) {
        reader.getBuffer().copyFrom(buffer);
    }

    public String prevChar() {
        return String.valueOf((char)reader.getBuffer().prevChar());
    }

    public String currChar() {
        return String.valueOf((char)reader.getBuffer().currChar());
    }

    public String lastBinding() {
        return reader.getLastBinding();
    }

    public void putString(String string) {
        reader.getBuffer().write(string);
    }

    public String tailTip() {
        return reader.getTailTip();
    }

    public void enableTailTip(boolean enable) {
        reader.enableTailTip(enable);
    }

    public static class AutopairWidgets extends Widgets {
        /*
         *  Inspired by zsh-autopair
         *  https://github.com/hlissner/zsh-autopair
         */
        private static final Map<String,String> LBOUNDS;
        private static final Map<String,String> RBOUNDS;
        private final Map<String,String> pairs;
        private final Map<String,Binding> defaultBindings = new HashMap<>();
        private boolean autopair = false;
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
            if (addCurlyBrackets) {
                pairs.put("{", "}");
            }
            addWidget("autopair-insert", this::autopairInsert);
            addWidget("autopair-close", this::autopairClose);
            addWidget("autopair-delete", this::autopairDelete);
            addWidget("autopair-toggle", this::toggleKeyBindings);

            KeyMap<Binding> map = getKeyMap(LineReader.MAIN);
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                defaultBindings.put(p.getKey(), map.getBound(p.getKey()));
                if (!p.getKey().equals(p.getValue())) {
                    defaultBindings.put(p.getValue(), map.getBound(p.getValue()));
                }
            }
            defaultBindings.put(ctrl('H'), map.getBound(ctrl('H')));
            defaultBindings.put(del(), map.getBound(del()));
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
            if (pairs.containsValue(lastBinding())
                && currChar().equals(lastBinding())) {
                callWidget(LineReader.FORWARD_CHAR);
            } else {
                callWidget(LineReader.SELF_INSERT);
            }
            return true;
        }

        public boolean autopairDelete() {
            if (pairs.containsKey(prevChar()) && pairs.get(prevChar()).equals(currChar())
                    && canDelete(prevChar())) {
                callWidget(LineReader.DELETE_CHAR);
            }
            callWidget(LineReader.BACKWARD_DELETE_CHAR);
            return true;
        }

        public boolean toggleKeyBindings() {
            if (autopair) {
                defaultBindings();
            } else {
                autopairBindings();
            }
            return autopair;
        }
        /*
         * key bindings...
         *
         */
        private void autopairBindings() {
            KeyMap<Binding> map = getKeyMap(LineReader.MAIN);
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                map.bind(new Reference("autopair-insert"), p.getKey());
                if (!p.getKey().equals(p.getValue())) {
                    map.bind(new Reference("autopair-close"), p.getValue());
                }
            }
            map.bind(new Reference("autopair-delete"), ctrl('H'));
            map.bind(new Reference("autopair-delete"), del());
            autopair = true;
        }

        private void defaultBindings() {
            KeyMap<Binding> map = getKeyMap(LineReader.MAIN);
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                map.bind(defaultBindings.get(p.getKey()), p.getKey());
                if (!p.getKey().equals(p.getValue())) {
                    map.bind(defaultBindings.get(p.getValue()), p.getValue());
                }
            }
            map.bind(defaultBindings.get(ctrl('H')), ctrl('H'));
            map.bind(defaultBindings.get(del()), del());
            autopair = false;
        }
        /*
         * helpers
         */
        private boolean canPair(String d) {
            if (balanced(d) && !nexToBoundary(d)) {
                if (d.equals(" ") && (prevChar().equals(" ") || currChar().equals(" "))) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }

        private boolean canSkip(String d) {
            if (pairs.get(d).equals(d) && d.charAt(0) != ' ' && currChar().equals(d)
                    && balanced(d)) {
                return true;
            }
            return false;
        }

        private boolean canDelete(String d) {
            if (balanced(d)) {
                return true;
            }
            return false;
        }

        private boolean balanced(String d) {
            boolean out = false;
            Buffer buf = buffer();
            String lbuf = buf.upToCursor();
            String rbuf = buf.substring(lbuf.length());
            String regx1 = pairs.get(d).equals(d)? d : "\\"+d;
            String regx2 = pairs.get(d).equals(d)? pairs.get(d) : "\\"+pairs.get(d);
            int llen = lbuf.length() - lbuf.replaceAll(regx1, "").length();
            int rlen = rbuf.length() - rbuf.replaceAll(regx2, "").length();
            if (llen == 0 && rlen == 0) {
                out = true;
            } else if (d.charAt(0) == ' ') {
                   out = true;
            } else if (pairs.get(d).equals(d)) {
                if ( llen == rlen || (llen + rlen) % 2 == 0 ) {
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
            if ((lb.length() > 0 && prevChar().matches(lb))
                    ||
               (rb.length() > 0 && currChar().matches(rb))) {
                return true;
            }
            return false;
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
            for (String k: bk) {
                if (boundary(LBOUNDS.get(k), RBOUNDS.get(k))) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class AutosuggestionWidgets extends Widgets {
        private final Map<Reference, Set<String>> defaultBindings = new HashMap<>();
        private boolean autosuggestion = false;

        public AutosuggestionWidgets(LineReader reader) {
            super(reader);
            addWidget("autosuggest-forward-char", this::autosuggestForwardChar);
            addWidget("autosuggest-end-of-line", this::autosuggestEndOfLine);
            addWidget("autosuggest-forward-word", this::partialAccept);
            addWidget("autosuggest-toggle", this::toggleKeyBindings);
            KeyMap<Binding> map = getKeyMap(LineReader.MAIN);
            for (Map.Entry<String, Binding> bound : map.getBoundKeys().entrySet()) {
                if (bound.getValue() instanceof Reference) {
                    Reference w = (Reference)bound.getValue();
                    if (w.name().equals(LineReader.FORWARD_CHAR)){
                        addKeySequence(w, bound.getKey());
                    } else if (w.name().equals(LineReader.END_OF_LINE)){
                        addKeySequence(w, bound.getKey());
                    } else if (w.name().equals(LineReader.FORWARD_WORD)){
                        addKeySequence(w, bound.getKey());
                    }
                }
            }
        }

        private void addKeySequence(Reference widget, String keySequence) {
            if (!defaultBindings.containsKey(widget)) {
                defaultBindings.put(widget, new HashSet<String>());
            }
            defaultBindings.get(widget).add(keySequence);
        }
        /*
         * Widgets
         */
        public boolean partialAccept() {
            Buffer buffer = buffer();
            if (buffer.cursor() == buffer.length()) {
                int curPos = buffer.cursor();
                buffer.write(tailTip());
                buffer.cursor(curPos);
                replaceBuffer(buffer);
                callWidget(LineReader.FORWARD_WORD);
                Buffer newBuf = new BufferImpl();
                newBuf.write(buffer().substring(0, buffer().cursor()));
                replaceBuffer(newBuf);
            } else {
                callWidget(LineReader.FORWARD_WORD);
            }
            return true;
        }

        public boolean autosuggestForwardChar() {
            return accept(LineReader.FORWARD_CHAR);
        }

        public boolean autosuggestEndOfLine() {
            return accept(LineReader.END_OF_LINE);
        }

        public boolean toggleKeyBindings() {
            if (autosuggestion) {
                defaultBindings();
            } else {
                autosuggestionBindings();
            }
            enableTailTip(autosuggestion);
            return autosuggestion;
        }


        private boolean accept(String widget) {
            Buffer buffer = buffer();
            if (buffer.cursor() == buffer.length()) {
                putString(tailTip());
            } else {
                callWidget(widget);
            }
            return true;
        }
        /*
         * key bindings...
         *
         */
        private void autosuggestionBindings() {
            KeyMap<Binding> map = getKeyMap(LineReader.MAIN);
            for (Map.Entry<Reference, Set<String>> entry : defaultBindings.entrySet()) {
                if (entry.getKey().name().equals(LineReader.FORWARD_CHAR)) {
                    for (String s: entry.getValue()) {
                        map.bind(new Reference("autosuggest-forward-char"), s);
                    }
                } else if (entry.getKey().name().equals(LineReader.END_OF_LINE)) {
                    for (String s: entry.getValue()) {
                        map.bind(new Reference("autosuggest-end-of-line"), s);
                    }
                } else if (entry.getKey().name().equals(LineReader.FORWARD_WORD)) {
                    for (String s: entry.getValue()) {
                        map.bind(new Reference("autosuggest-forward-word"), s);
                    }
                }
            }
            autosuggestion = true;
        }

        private void defaultBindings() {
            KeyMap<Binding> map = getKeyMap(LineReader.MAIN);
            for (Map.Entry<Reference, Set<String>> entry : defaultBindings.entrySet()) {
                for (String s: entry.getValue()) {
                    map.bind(entry.getKey(), s);
                }
            }
            autosuggestion = false;
        }
    }
}
