/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.keymap;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.jline.Console;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;

/**
 * The KeyMap class contains all bindings from keys to operations.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.6
 */
public class KeyMap implements Binding {

    public static final int KEYMAP_LENGTH = 256;

    private Binding[] mapping = new Binding[KEYMAP_LENGTH];
    private Binding anotherKey = null;

    public KeyMap() {
        this(null);
    }

    public KeyMap(Binding[] mapping) {
        this.mapping = mapping != null
                ? Arrays.copyOfRange(mapping, 0, KEYMAP_LENGTH)
                : new Binding[KEYMAP_LENGTH];
    }

    public static String display(String key) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < 32) {
                sb.append('^');
                sb.append((char) (c + 'A' - 1));
            } else if (c == 127) {
                sb.append("^?");
            } else if (c == '^' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c >= 128) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public static String translate(String str) {
        int i;
        if (!str.isEmpty()) {
            char c = str.charAt(0);
            if ((c == '\'' || c == '"') && str.charAt(str.length() - 1) == c) {
                str = str.substring(1, str.length() - 1);
            }
        }
        StringBuilder keySeq = new StringBuilder();
        for (i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                if (++i >= str.length()) {
                    break;
                }
                c = str.charAt(i);
                switch (c) {
                    case 'a':
                        c = 0x07;
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case 'd':
                        c = 0x7f;
                        break;
                    case 'e':
                    case 'E':
                        c = 0x1b;
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'v':
                        c = 0x0b;
                        break;
                    case '\\':
                        c = '\\';
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        c = 0;
                        for (int j = 0; j < 3; j++, i++) {
                            if (i >= str.length()) {
                                break;
                            }
                            int k = Character.digit(str.charAt(i), 8);
                            if (k < 0) {
                                break;
                            }
                            c = (char) (c * 8 + k);
                        }
                        i--;
                        c &= 0xFF;
                        break;
                    case 'x':
                        i++;
                        c = 0;
                        for (int j = 0; j < 2; j++, i++) {
                            if (i >= str.length()) {
                                break;
                            }
                            int k = Character.digit(str.charAt(i), 16);
                            if (k < 0) {
                                break;
                            }
                            c = (char) (c * 16 + k);
                        }
                        i--;
                        c &= 0xFF;
                        break;
                    case 'u':
                        i++;
                        c = 0;
                        for (int j = 0; j < 4; j++, i++) {
                            if (i >= str.length()) {
                                break;
                            }
                            int k = Character.digit(str.charAt(i), 16);
                            if (k < 0) {
                                break;
                            }
                            c = (char) (c * 16 + k);
                        }
                        break;
                    case 'C':
                        if (++i >= str.length()) {
                            break;
                        }
                        c = str.charAt(i);
                        if (c == '-') {
                            if (++i >= str.length()) {
                                break;
                            }
                            c = str.charAt(i);
                        }
                        c = c == '?' ? 0x7f : (char) (Character.toUpperCase(c) & 0x1f);
                        break;
                }
            } else if (c == '^') {
                if (++i >= str.length()) {
                    break;
                }
                c = str.charAt(i);
                if (c != '^') {
                    c = c == '?' ? 0x7f : (char) (Character.toUpperCase(c) & 0x1f);
                }
            }
            keySeq.append(c);
        }
        return keySeq.toString();
    }

    public static Collection<String> range(String range) {
        String[] keys = range.split("-");
        if (keys.length != 2) {
            return null;
        }
        keys[0] = translate(keys[0]);
        keys[1] = translate(keys[1]);
        if (keys[0].length() != keys[1].length()) {
            return null;
        }
        String pfx;
        if (keys[0].length() > 1) {
            pfx = keys[0].substring(0, keys[0].length() - 1);
            if (!keys[1].startsWith(pfx)) {
                return null;
            }
        } else {
            pfx = "";
        }
        char c0 = keys[0].charAt(keys[0].length() - 1);
        char c1 = keys[1].charAt(keys[1].length() - 1);
        if (c0 > c1) {
            return null;
        }
        Collection<String> seqs = new ArrayList<>();
        for (char c = c0; c <= c1; c++) {
            seqs.add(pfx + c);
        }
        return seqs;
    }


    public static String esc() {
        return "\033";
    }

    public static String alt(char c) {
        return "\033" + c;
    }

    public static String del() {
        return "\177";
    }

    public static String ctrl(char key) {
        return key == '?' ? del() : Character.toString((char) (Character.toUpperCase(key) & 0x1f));
    }

    public Object getAnotherKey() {
        return anotherKey;
    }

    public static final Comparator<String> KEYSEQ_COMPARATOR = (s1, s2) -> {
        int len1 = s1.length();
        int len2 = s2.length();
        int lim = Math.min(len1, len2);
        int k = 0;
        while (k < lim) {
            char c1 = s1.charAt(k);
            char c2 = s2.charAt(k);
            if (c1 != c2) {
                int l = len1 - len2;
                return l != 0 ? l : c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    };

    public Map<String, Binding> getBoundKeys() {
        Map<String, Binding> bound = new TreeMap<>(KEYSEQ_COMPARATOR);
        doGetBoundKeys(this, "", bound);
        return bound;
    }

    private static void doGetBoundKeys(KeyMap keyMap, String prefix, Map<String, Binding> bound) {
        if (keyMap.anotherKey != null) {
            bound.put(prefix, keyMap.anotherKey);
        }
        for (int c = 0; c < keyMap.mapping.length; c++) {
            if (keyMap.mapping[c] instanceof KeyMap) {
                doGetBoundKeys((KeyMap) keyMap.mapping[c],
                        prefix + (char) (c),
                        bound);
            } else if (keyMap.mapping[c] != null) {
                bound.put(prefix + (char) (c), keyMap.mapping[c]);
            }
        }
    }

    public Binding getBound(CharSequence keySeq, int[] remaining) {
        remaining[0] = -1;
        if (keySeq != null && keySeq.length() > 0) {
            char c = keySeq.charAt(0);
            if (c >= mapping.length) {
                remaining[0] = Character.codePointCount(keySeq, 0, keySeq.length());
                return null;
            } else {
                if (mapping[c] instanceof KeyMap) {
                    CharSequence sub = keySeq.subSequence(1, keySeq.length());
                    return ((KeyMap) mapping[c]).getBound(sub, remaining);
                } else if (mapping[c] != null) {
                    remaining[0] = keySeq.length() - 1;
                    return mapping[c];
                } else {
                    remaining[0] = keySeq.length();
                    return anotherKey;
                }
            }
        } else {
            return anotherKey;
        }
    }

    public Binding getBound(CharSequence keySeq) {
        int[] remaining = new int[1];
        Binding res = getBound(keySeq, remaining);
        return remaining[0] <= 0 ? res : null;
    }

    public void bindIfNotBound(CharSequence keySeq, Binding function) {
        if (function != null) {
            bind(this, keySeq, function, true);
        }
    }

    public void bind(CharSequence keySeq, Binding function) {
        if (function == null) {
            unbind(keySeq);
        } else {
            bind(this, keySeq, function, false);
        }
    }

    public boolean bind(Console console, Capability capability, Binding function) {
        try {
            String str = console.getStringCapability(capability);
            if (str != null) {
                StringWriter sw = new StringWriter();
                Curses.tputs(sw, str);
                bind(sw.toString(), function);
                return true;
            }
        } catch (IOException e) {
            // Ignore
        }
        return false;
    }

    public Binding unbind(CharSequence keySeq) {
        return unbind(this, keySeq);
    }

    private static Binding unbind(KeyMap map, CharSequence keySeq) {
        KeyMap prev = null;
        if (keySeq != null && keySeq.length() > 0) {
            for (int i = 0; i < keySeq.length() - 1; i++) {
                char c = keySeq.charAt(i);
                if (c > map.mapping.length) {
                    return null;
                }
                if (!(map.mapping[c] instanceof KeyMap)) {
                    return null;
                }
                prev = map;
                map = (KeyMap) map.mapping[c];
            }
            char c = keySeq.charAt(keySeq.length() - 1);
            if (c > map.mapping.length) {
                return null;
            }
            if (map.mapping[c] instanceof KeyMap) {
                KeyMap sub = (KeyMap) map.mapping[c];
                Binding res = sub.anotherKey;
                sub.anotherKey = null;
                return res;
            } else {
                Binding res = map.mapping[c];
                map.mapping[c] = null;
                int nb = 0;
                for (int i = 0; i < map.mapping.length; i++) {
                    if (map.mapping[i] != null) {
                        nb++;
                    }
                }
                if (nb == 0 && prev != null) {
                    prev.mapping[keySeq.charAt(keySeq.length() - 2)] = map.anotherKey;
                }
                return res;
            }
        }
        return null;
    }

    private static void bind(KeyMap map, CharSequence keySeq, Binding function, boolean onlyIfNotBound) {
        if (keySeq != null && keySeq.length() > 0) {
            for (int i = 0; i < keySeq.length(); i++) {
                char c = keySeq.charAt(i);
                if (c >= map.mapping.length) {
                    return;
                }
                if (i < keySeq.length() - 1) {
                    if (!(map.mapping[c] instanceof KeyMap)) {
                        KeyMap m = new KeyMap();
                        m.anotherKey = map.mapping[c];
                        map.mapping[c] = m;
                    }
                    map = (KeyMap) map.mapping[c];
                } else {
                    if (map.mapping[c] instanceof KeyMap) {
                        ((KeyMap) map.mapping[c]).anotherKey = function;
                    } else {
                        Object op = map.mapping[c];
                        if (!onlyIfNotBound || op == null) {
                            map.mapping[c] = function;
                        }
                    }
                }
            }
        }
    }

}
