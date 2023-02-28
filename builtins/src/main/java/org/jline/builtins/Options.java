/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.StyleResolver;

/**
 * Yet another GNU long options parser. This one is configured by parsing its Usage string.
 *
 * Code coming from Apache Felix Gogo Shell
 */
public class Options {

    public static final String NL = System.getProperty("line.separator", "\n");

    // Note: need to double \ within ""
    private static final String regex = "(?x)\\s*" + "(?:-([^-]))?" + // 1: short-opt-1
            "(?:,?\\s*-(\\w))?"
            + // 2: short-opt-2
            "(?:,?\\s*--(\\w[\\w-]*)(=\\w+)?)?"
            + // 3: long-opt-1 and 4:arg-1
            "(?:,?\\s*--(\\w[\\w-]*))?"
            + // 5: long-opt-2
            ".*?(?:\\(default=(.*)\\))?\\s*"; // 6: default

    private static final int GROUP_SHORT_OPT_1 = 1;
    private static final int GROUP_SHORT_OPT_2 = 2;
    private static final int GROUP_LONG_OPT_1 = 3;
    private static final int GROUP_ARG_1 = 4;
    private static final int GROUP_LONG_OPT_2 = 5;
    private static final int GROUP_DEFAULT = 6;

    private static final Pattern parser = Pattern.compile(regex);
    private static final Pattern uname = Pattern.compile("^Usage:\\s+(\\w+)");

    private final Map<String, Boolean> unmodifiableOptSet;
    private final Map<String, Object> unmodifiableOptArg;
    private final Map<String, Boolean> optSet = new HashMap<>();
    private final Map<String, Object> optArg = new HashMap<>();

    private final Map<String, String> optName = new HashMap<>();
    private final Map<String, String> optAlias = new HashMap<>();
    private final List<Object> xargs = new ArrayList<>();
    private List<String> args = null;

    private static final String UNKNOWN = "unknown";
    private String usageName = UNKNOWN;
    private int usageIndex = 0;

    private final String[] spec;
    private final String[] gspec;
    private final String defOpts;
    private final String[] defArgs;
    private String error = null;

    private boolean optionsFirst = false;
    private boolean stopOnBadOption = false;

    public static Options compile(String[] optSpec) {
        return new Options(optSpec, null, null, System::getenv);
    }

    public static Options compile(String[] optSpec, Function<String, String> env) {
        return new Options(optSpec, null, null, env);
    }

    public static Options compile(String optSpec) {
        return compile(optSpec.split("\\n"), System::getenv);
    }

    public static Options compile(String optSpec, Function<String, String> env) {
        return compile(optSpec.split("\\n"), env);
    }

    public static Options compile(String[] optSpec, Options gopt) {
        return new Options(optSpec, null, gopt, System::getenv);
    }

    public static Options compile(String[] optSpec, String[] gspec) {
        return new Options(optSpec, gspec, null, System::getenv);
    }

    public Options setStopOnBadOption(boolean stopOnBadOption) {
        this.stopOnBadOption = stopOnBadOption;
        return this;
    }

    public Options setOptionsFirst(boolean optionsFirst) {
        this.optionsFirst = optionsFirst;
        return this;
    }

    public boolean isSet(String name) {
        Boolean isSet = optSet.get(name);
        if (isSet == null) {
            throw new IllegalArgumentException("option not defined in spec: " + name);
        }
        return isSet;
    }

    public Object getObject(String name) {
        if (!optArg.containsKey(name)) throw new IllegalArgumentException("option not defined with argument: " + name);

        List<Object> list = getObjectList(name);

        return list.isEmpty() ? "" : list.get(list.size() - 1);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getObjectList(String name) {
        List<Object> list;
        Object arg = optArg.get(name);

        if (arg == null) {
            throw new IllegalArgumentException("option not defined with argument: " + name);
        }

        if (arg instanceof String) { // default value
            list = new ArrayList<>();
            if (!"".equals(arg)) list.add(arg);
        } else {
            list = (List<Object>) arg;
        }

        return list;
    }

    public List<String> getList(String name) {
        ArrayList<String> list = new ArrayList<>();
        for (Object o : getObjectList(name)) {
            try {
                list.add((String) o);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("option not String: " + name);
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private void addArg(String name, Object value) {
        List<Object> list;
        Object arg = optArg.get(name);

        if (arg instanceof String) { // default value
            list = new ArrayList<>();
            optArg.put(name, list);
        } else {
            list = (List<Object>) arg;
        }

        list.add(value);
    }

    public String get(String name) {
        try {
            return (String) getObject(name);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("option not String: " + name);
        }
    }

    public int getNumber(String name) {
        String number = get(name);
        try {
            if (number != null) return Integer.parseInt(number);
            return 0;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("option '" + name + "' not Number: " + number);
        }
    }

    public List<Object> argObjects() {
        return xargs;
    }

    public List<String> args() {
        if (args == null) {
            args = new ArrayList<>();
            for (Object arg : xargs) {
                args.add(arg == null ? "null" : arg.toString());
            }
        }
        return args;
    }

    // Added for backword compability
    public void usage(PrintStream err) {
        err.print(usage());
    }

    public String usage() {
        StringBuilder buf = new StringBuilder();
        int index = 0;

        if (error != null) {
            buf.append(error);
            buf.append(NL);
            index = usageIndex;
        }

        for (int i = index; i < spec.length; ++i) {
            buf.append(spec[i]);
            buf.append(NL);
        }

        return buf.toString();
    }

    /**
     * Prints usage message and returns IllegalArgumentException, for you to throw.
     *
     * @param s the message to display
     * @return an exception with the generated message
     */
    public IllegalArgumentException usageError(String s) {
        error = usageName + ": " + s;
        return new IllegalArgumentException(error);
    }

    // internal constructor
    private Options(String[] spec, String[] gspec, Options opt, Function<String, String> env) {
        this.gspec = gspec;

        if (gspec == null && opt == null) {
            this.spec = spec;
        } else {
            ArrayList<String> list = new ArrayList<>();
            list.addAll(Arrays.asList(spec));
            list.addAll(Arrays.asList(gspec != null ? gspec : opt.gspec));
            this.spec = list.toArray(new String[list.size()]);
        }

        Map<String, Boolean> myOptSet = new HashMap<>();
        Map<String, Object> myOptArg = new HashMap<>();

        parseSpec(myOptSet, myOptArg);

        if (opt != null) {
            for (Entry<String, Boolean> e : opt.optSet.entrySet()) {
                if (e.getValue()) myOptSet.put(e.getKey(), true);
            }

            for (Entry<String, Object> e : opt.optArg.entrySet()) {
                if (!e.getValue().equals("")) myOptArg.put(e.getKey(), e.getValue());
            }

            opt.reset();
        }

        unmodifiableOptSet = Collections.unmodifiableMap(myOptSet);
        unmodifiableOptArg = Collections.unmodifiableMap(myOptArg);

        defOpts = env != null ? env.apply(usageName.toUpperCase() + "_OPTS") : null;
        defArgs = (defOpts != null) ? defOpts.split("\\s+") : new String[0];
    }

    /**
     * parse option spec.
     */
    private void parseSpec(Map<String, Boolean> myOptSet, Map<String, Object> myOptArg) {
        int index = 0;
        for (String line : spec) {
            Matcher m = parser.matcher(line);

            if (m.matches()) {
                final String opt = m.group(GROUP_LONG_OPT_1);
                final String name = (opt != null) ? opt : m.group(GROUP_SHORT_OPT_1);

                if (name != null) {
                    if (myOptSet.putIfAbsent(name, false) != null)
                        throw new IllegalArgumentException("duplicate option in spec: --" + name);
                }

                String dflt = (m.group(GROUP_DEFAULT) != null) ? m.group(GROUP_DEFAULT) : "";
                if (m.group(GROUP_ARG_1) != null) myOptArg.put(opt, dflt);

                String opt2 = m.group(GROUP_LONG_OPT_2);
                if (opt2 != null) {
                    optAlias.put(opt2, opt);
                    myOptSet.put(opt2, false);
                    if (m.group(GROUP_ARG_1) != null) myOptArg.put(opt2, "");
                }

                for (int i = 0; i < 2; ++i) {
                    String sopt = m.group(i == 0 ? GROUP_SHORT_OPT_1 : GROUP_SHORT_OPT_2);
                    if (sopt != null) {
                        if (optName.putIfAbsent(sopt, name) != null)
                            throw new IllegalArgumentException("duplicate option in spec: -" + sopt);
                    }
                }
            }

            if (Objects.equals(usageName, UNKNOWN)) {
                Matcher u = uname.matcher(line);
                if (u.find()) {
                    usageName = u.group(1);
                    usageIndex = index;
                }
            }

            index++;
        }
    }

    private void reset() {
        optSet.clear();
        optSet.putAll(unmodifiableOptSet);
        optArg.clear();
        optArg.putAll(unmodifiableOptArg);
        xargs.clear();
        args = null;
        error = null;
    }

    public Options parse(Object[] argv) {
        return parse(argv, false);
    }

    public Options parse(List<?> argv) {
        return parse(argv, false);
    }

    public Options parse(Object[] argv, boolean skipArg0) {
        if (null == argv) throw new IllegalArgumentException("argv is null");

        return parse(Arrays.asList(argv), skipArg0);
    }

    public Options parse(List<?> argv, boolean skipArg0) {
        reset();
        List<Object> args = new ArrayList<>();
        args.addAll(Arrays.asList(defArgs));

        for (Object arg : argv) {
            if (skipArg0) {
                skipArg0 = false;
                usageName = arg.toString();
            } else {
                args.add(arg);
            }
        }

        String needArg = null;
        String needOpt = null;
        boolean endOpt = false;

        for (Object oarg : args) {
            String arg = oarg == null ? "null" : oarg.toString();

            if (endOpt) {
                xargs.add(oarg);
            } else if (needArg != null) {
                addArg(needArg, oarg);
                needArg = null;
                needOpt = null;
            } else if (!arg.startsWith("-")
                    || (arg.length() > 1 && Character.isDigit(arg.charAt(1)))
                    || "-".equals(oarg)) {
                if (optionsFirst) endOpt = true;
                xargs.add(oarg);
            } else {
                if (arg.equals("--")) endOpt = true;
                else if (arg.startsWith("--")) {
                    int eq = arg.indexOf("=");
                    String value = (eq == -1) ? null : arg.substring(eq + 1);
                    String name = arg.substring(2, ((eq == -1) ? arg.length() : eq));
                    List<String> names = new ArrayList<>();

                    if (optSet.containsKey(name)) {
                        names.add(name);
                    } else {
                        for (String k : optSet.keySet()) {
                            if (k.startsWith(name)) names.add(k);
                        }
                    }

                    switch (names.size()) {
                        case 1:
                            name = names.get(0);
                            optSet.put(name, true);
                            if (optArg.containsKey(name)) {
                                if (value != null) addArg(name, value);
                                else needArg = name;
                            } else if (value != null) {
                                throw usageError("option '--" + name + "' doesn't allow an argument");
                            }
                            break;

                        case 0:
                            if (stopOnBadOption) {
                                endOpt = true;
                                xargs.add(oarg);
                                break;
                            } else throw usageError("invalid option '--" + name + "'");

                        default:
                            throw usageError("option '--" + name + "' is ambiguous: " + names);
                    }
                } else {
                    for (int i = 1; i < arg.length(); i++) {
                        String c = String.valueOf(arg.charAt(i));
                        if (optName.containsKey(c)) {
                            String name = optName.get(c);
                            optSet.put(name, true);
                            if (optArg.containsKey(name)) {
                                int k = i + 1;
                                if (k < arg.length()) {
                                    addArg(name, arg.substring(k));
                                } else {
                                    needOpt = c;
                                    needArg = name;
                                }
                                break;
                            }
                        } else {
                            if (stopOnBadOption) {
                                xargs.add("-" + c);
                                endOpt = true;
                            } else throw usageError("invalid option '" + c + "'");
                        }
                    }
                }
            }
        }

        if (needArg != null) {
            String name = (needOpt != null) ? needOpt : "--" + needArg;
            throw usageError("option '" + name + "' requires an argument");
        }

        // remove long option aliases
        for (Entry<String, String> alias : optAlias.entrySet()) {
            if (optSet.get(alias.getKey())) {
                optSet.put(alias.getValue(), true);
                if (optArg.containsKey(alias.getKey())) optArg.put(alias.getValue(), optArg.get(alias.getKey()));
            }
            optSet.remove(alias.getKey());
            optArg.remove(alias.getKey());
        }

        return this;
    }

    @Override
    public String toString() {
        return "isSet" + optSet + "\nArg" + optArg + "\nargs" + xargs;
    }

    /**
     * Exception thrown when using the <code>--help</code> option on a built-in command.
     * It can be highlighted using the {@link #highlight(String, StyleResolver)} method and then printed
     * to the {@link org.jline.terminal.Terminal}.
     */
    @SuppressWarnings("serial")
    public static class HelpException extends Exception {

        public HelpException(String message) {
            super(message);
        }

        public static StyleResolver defaultStyle() {
            return Styles.helpStyle();
        }

        public static AttributedString highlight(String msg, StyleResolver resolver) {
            Matcher tm = Pattern.compile("(^|\\n)(Usage|Summary)(:)").matcher(msg);
            if (tm.find()) {
                boolean subcommand = tm.group(2).equals("Summary");
                AttributedStringBuilder asb = new AttributedStringBuilder(msg.length());
                // Command
                AttributedStringBuilder acommand = new AttributedStringBuilder()
                        .append(msg.substring(0, tm.start(2)))
                        .styleMatches(
                                Pattern.compile("(?:^\\s*)([a-z]+[a-zA-Z0-9-]*)\\b"),
                                Collections.singletonList(resolver.resolve(".co")));
                asb.append(acommand);
                // Title
                asb.styled(resolver.resolve(".ti"), tm.group(2)).append(":");
                // Syntax
                for (String line : msg.substring(tm.end(3)).split("\n")) {
                    int ind = line.lastIndexOf("  ");
                    String syntax, comment;
                    if (ind > 20) {
                        syntax = line.substring(0, ind);
                        comment = line.substring(ind + 1);
                    } else {
                        syntax = line;
                        comment = "";
                    }
                    asb.append(_highlightSyntax(syntax, resolver, subcommand));
                    asb.append(_highlightComment(comment, resolver));
                    asb.append("\n");
                }
                return asb.toAttributedString();
            } else {
                return AttributedString.fromAnsi(msg);
            }
        }

        public static AttributedString highlightSyntax(String syntax, StyleResolver resolver, boolean subcommands) {
            return _highlightSyntax(syntax, resolver, subcommands).toAttributedString();
        }

        public static AttributedString highlightSyntax(String syntax, StyleResolver resolver) {
            return _highlightSyntax(syntax, resolver, false).toAttributedString();
        }

        public static AttributedString highlightComment(String comment, StyleResolver resolver) {
            return _highlightComment(comment, resolver).toAttributedString();
        }

        private static AttributedStringBuilder _highlightSyntax(
                String syntax, StyleResolver resolver, boolean subcommand) {
            StringBuilder indent = new StringBuilder();
            for (char c : syntax.toCharArray()) {
                if (c != ' ') {
                    break;
                }
                indent.append(c);
            }
            AttributedStringBuilder asyntax = new AttributedStringBuilder().append(syntax.substring(indent.length()));
            // command
            asyntax.styleMatches(
                    Pattern.compile("(?:^)([a-z]+[a-zA-Z0-9-]*)\\b"),
                    Collections.singletonList(resolver.resolve(".co")));
            if (!subcommand) {
                // argument
                asyntax.styleMatches(
                        Pattern.compile("(?:<|\\[|\\s|=)([A-Za-z]+[A-Za-z_-]*)\\b"),
                        Collections.singletonList(resolver.resolve(".ar")));
                // option
                asyntax.styleMatches(
                        Pattern.compile("(?:^|\\s|\\[)(-\\$|-\\?|[-]{1,2}[A-Za-z-]+\\b)"),
                        Collections.singletonList(resolver.resolve(".op")));
            }
            return new AttributedStringBuilder().append(indent).append(asyntax);
        }

        private static AttributedStringBuilder _highlightComment(String comment, StyleResolver resolver) {
            AttributedStringBuilder acomment = new AttributedStringBuilder().append(comment);
            // option
            acomment.styleMatches(
                    Pattern.compile("(?:\\s|\\[)(-\\$|-\\?|[-]{1,2}[A-Za-z-]+\\b)"),
                    Collections.singletonList(resolver.resolve(".op")));
            // argument in comment
            acomment.styleMatches(
                    Pattern.compile("(?:\\s)([a-z]+[-]+[a-z]+|[A-Z_]{2,})(?:\\s)"),
                    Collections.singletonList(resolver.resolve(".ar")));
            return acomment;
        }
    }
}
