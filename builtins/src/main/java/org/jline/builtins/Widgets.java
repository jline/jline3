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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.reader.impl.LineReaderImpl;

public class Widgets {

    private static void addWidget(Map<String, Widget> widgets, String name, Widget widget) {
        widgets.put(name, namedWidget(name, widget));
    }

    private static Widget namedWidget(final String name, final Widget widget) {
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

    public static class AutopairWidgets {
        /*
         *  Inspired by zsh-autopair
         *  https://github.com/hlissner/zsh-autopair
         */
        private static final Map<String,String> LBOUNDS;
        private static final Map<String,String> RBOUNDS;
        private final Map<String,String> pairs;
        private final LineReaderImpl reader;
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
            this.reader = (LineReaderImpl)reader;
            if (addCurlyBrackets) {
                pairs.put("{", "}");
            }
            addWidget(reader.getWidgets(), "autopair-insert", this::autopairInsert);
            addWidget(reader.getWidgets(), "autopair-close", this::autopairClose);
            addWidget(reader.getWidgets(), "autopair-delete", this::autopairDelete);
        }

        /*
         * Widgets
         */
        public boolean autopairInsert() {
            if (pairs.containsKey(reader.getLastBinding())) {
                if (canSkip(reader.getLastBinding())) {
                    reader.callWidget(LineReader.FORWARD_CHAR);
                } else if (canPair(reader.getLastBinding())) {
                    reader.callWidget(LineReader.SELF_INSERT);
                    reader.putString(pairs.get(reader.getLastBinding()));
                    reader.callWidget(LineReader.BACKWARD_CHAR);
                } else {
                    reader.callWidget(LineReader.SELF_INSERT);
                }
            } else {
                reader.callWidget(LineReader.SELF_INSERT);
            }
            return true;
        }

        public boolean autopairClose() {
            if (pairs.containsValue(reader.getLastBinding())
                && currChar().equals(reader.getLastBinding())) {
                reader.callWidget(LineReader.FORWARD_CHAR);
            } else {
                reader.callWidget(LineReader.SELF_INSERT);
            }
            return true;
        }

        public boolean autopairDelete() {
            if (pairs.containsKey(prevChar()) && pairs.get(prevChar()).equals(currChar())
                    && canDelete(prevChar())) {
                reader.callWidget(LineReader.DELETE_CHAR);
            }
            reader.callWidget(LineReader.BACKWARD_DELETE_CHAR);
            return true;
        }

        /*
         * key bindings...
         *
         */
        public boolean toggleKeyBindings() {
            if(autopair) {
                defaultBindings();
            } else {
                autopairBindings();
            }
            return autopair;
        }

        private void autopairBindings() {
            if (autopair) {
                return;
            }
            KeyMap<Binding> map = reader.getKeyMaps().get(LineReader.MAIN);
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                map.bind(new Reference("autopair-insert"), p.getKey());
                if (!p.getKey().equals(p.getValue())) {
                    map.bind(new Reference("autopair-close"), p.getValue());
                }
            }
            map.bind(new Reference("autopair-delete"), del());
            autopair = true;
        }

        private void defaultBindings() {
            if (!autopair) {
                return;
            }
            KeyMap<Binding> map = reader.getKeyMaps().get(LineReader.MAIN);
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                map.bind(new Reference(LineReader.SELF_INSERT), p.getKey());
                if (p.getKey().equals(p.getValue())) {
                    map.bind(new Reference(LineReader.SELF_INSERT), p.getValue());
                }
            }
            map.bind(new Reference(LineReader.BACKWARD_DELETE_CHAR), del());
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
            Buffer buf = reader.getBuffer();
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

        private String prevChar() {
            return String.valueOf((char)reader.getBuffer().prevChar());
        }

        private String currChar() {
            return String.valueOf((char)reader.getBuffer().currChar());
        }

        private boolean boundary(String lb, String rb) {
            if((lb.length() > 0 && prevChar().matches(lb))
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
}
