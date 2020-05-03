/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.alt;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jline.builtins.Options.HelpException;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.Parser;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.reader.impl.BufferImpl;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;
import org.jline.utils.StyleResolver;

/**
 * Create custom widgets by extending Widgets class
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public abstract class Widgets {
    protected static final String AP_TOGGLE = "autopair-toggle";
    protected static final String AP_INSERT = "_autopair-insert";
    protected static final String AP_BACKWARD_DELETE_CHAR = "_autopair-backward-delete-char";
    protected static final String TT_TOGGLE = "tailtip-toggle";
    protected static final String TT_ACCEPT_LINE = "_tailtip-accept-line";

    private final LineReader reader;

    public Widgets(LineReader reader) {
        this.reader = reader;
    }

    /**
     * Add widget to the LineReader
     * @param name the name of widget
     * @param widget widget
     */
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

    /**
     * Call widget. System widget will be call if the name does not start with '_' or ends with '-toggle'
     * i.e. '.' will be added at the beginning of the name.
     * @param name widget name
     */
    public void callWidget(String name) {
        if (!name.startsWith("_") && !name.endsWith("-toggle")) {
            name = "." + name;
        }
        reader.callWidget(name);
    }

    /**
     * Bind widget to ctrl-alt-x and execute it
     * @param name widget name
     */
    public void executeWidget(String name) {
        Binding ref = getKeyMap().getBoundKeys().get(alt(ctrl('X')));
        getKeyMap().bind(new Reference(name), alt(ctrl('X')));
        reader.runMacro(alt(ctrl('X')));
        if (ref != null) {
            getKeyMap().bind(ref, alt(ctrl('X')));
        }
    }

    /**
     * Create alias to widget
     * @param orig widget orginal name
     * @param alias alias name
     */
    public void aliasWidget(String orig, String alias) {
        reader.getWidgets().put(alias, widget(orig));
    }

    /**
     *
     * @param name widget name or alias
     * @return widget name
     */
    public String getWidget(String name) {
        return widget(name).toString();
    }

    /**
     *
     * @param name widget name or alias
     * @return true if widget exists
     */
    public boolean existsWidget(String name) {
        try {
            widget(name);
            return true;
        } catch(Exception e) {
        }
        return false;
    }

    private Widget widget(String name) {
        Widget out = null;
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

    /**
     *
     * @return The LineRearer Parser
     */
    public Parser parser() {
        return reader.getParser();
    }

    /**
     *
     * @return The LineReader Main KeyMap
     */
    public KeyMap<Binding> getKeyMap() {
        return reader.getKeyMaps().get(LineReader.MAIN);
    }

    /**
     *
     * @return The LineReader Buffer
     */
    public Buffer buffer() {
        return reader.getBuffer();
    }

    /**
     *
     * @param buffer buffer that will be copied to the LineReader Buffer
     */
    public void replaceBuffer(Buffer buffer) {
        reader.getBuffer().copyFrom(buffer);
    }

    /**
     *
     * @return command line arguments
     */
    public List<String> args() {
        return reader.getParser().parse(buffer().toString(), 0, ParseContext.COMPLETE).words();
    }

    /**
     *
     * @return Buffer's previous character
     */
    public String prevChar() {
        return String.valueOf((char)reader.getBuffer().prevChar());
    }

    /**
     *
     * @return Buffer's current character
     */
    public String currChar() {
        return String.valueOf((char)reader.getBuffer().currChar());
    }

    /**
     *
     * @return LineReader's last binding
     */
    public String lastBinding() {
        return reader.getLastBinding();
    }

    /**
     *
     * @param string string to be written into LineReader Buffer
     */
    public void putString(String string) {
        reader.getBuffer().write(string);
    }

    /**
     *
     * @return Command line tail tip.
     */
    public String tailTip() {
        return reader.getTailTip();
    }

    /**
     *
     * @param tailTip tail tip to be added to the command line
     */
    public void setTailTip(String tailTip) {
        reader.setTailTip(tailTip);
    }

    /**
     *
     * @param errorPattern error pattern to be set LineReader Highlighter
     */
    public void setErrorPattern(Pattern errorPattern) {
        reader.getHighlighter().setErrorPattern(errorPattern);
    }

    /**
     *
     * @param errorIndex error index to be set LineReader Highlighter
     */
    public void setErrorIndex(int errorIndex) {
        reader.getHighlighter().setErrorIndex(errorIndex);
    }

    /**
     *  Clears command line tail tip
     */
    public void clearTailTip() {
        reader.setTailTip("");
    }

    /**
     *
     * @param type type to be set to the LineReader autosuggestion
     */
    public void setSuggestionType(SuggestionType type) {
        reader.setAutosuggestion(type);
    }

    /**
     *
     * @param desc Text to be dispalyed on terminal status bar
     */
    public void addDescription(List<AttributedString> desc) {
        Status.getStatus(reader.getTerminal()).update(desc);
    }

    /**
     *  Clears terminal status bar
     */
    public void clearDescription() {
        initDescription(0);
    }

    /**
     * Initialize terminal status bar
     * @param size Terminal status bar size in rows
     */
    public void initDescription(int size) {
        Status status = Status.getStatus(reader.getTerminal(), false);
        if (size > 0) {
            if (status == null) {
                status = Status.getStatus(reader.getTerminal());
            }
            status.setBorder(true);
            List<AttributedString> as = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                as.add(new AttributedString(""));
            }
            addDescription(as);
        } else if (status != null) {
            if (size < 0) {
                status.update(null);
            } else {
                status.clear();
            }
        }
    }

    /**
     *  Remove terminal status bar
     */
    public void destroyDescription() {
        initDescription(-1);
    }

    /**
     * Creates and manages widgets that auto-closes, deletes and skips over matching delimiters intelligently.
     */
    public static class AutopairWidgets extends Widgets {
        /*
         *  Inspired by zsh-autopair
         *  https://github.com/hlissner/zsh-autopair
         */
        private static final Map<String,String> LBOUNDS;
        private static final Map<String,String> RBOUNDS;
        private final Map<String,String> pairs;
        private final Map<String,Binding> defaultBindings = new HashMap<>();
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
            addWidget(AP_TOGGLE, this::toggleKeyBindings);

            KeyMap<Binding> map = getKeyMap();
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                defaultBindings.put(p.getKey(), map.getBound(p.getKey()));
                if (!p.getKey().equals(p.getValue())) {
                    defaultBindings.put(p.getValue(), map.getBound(p.getValue()));
                }
            }
        }

        public void enable() {
            if (!enabled) {
                executeWidget(AP_TOGGLE);
            }
        }

        public void disable() {
            if (enabled) {
                executeWidget(AP_TOGGLE);
            }
        }

        public boolean toggle() {
            boolean before = enabled;
            executeWidget(AP_TOGGLE);
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
                callWidget(TT_TOGGLE);
            }
            KeyMap<Binding> map = getKeyMap();
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                map.bind(new Reference(AP_INSERT), p.getKey());
                if (!p.getKey().equals(p.getValue())) {
                    map.bind(new Reference("_autopair-close"), p.getValue());
                }
            }
            aliasWidget(AP_BACKWARD_DELETE_CHAR, LineReader.BACKWARD_DELETE_CHAR);
            if (ttActive) {
                callWidget(TT_TOGGLE);
            }
            enabled = true;
        }

        private void defaultBindings() {
            KeyMap<Binding> map = getKeyMap();
            for (Map.Entry<String, String> p: pairs.entrySet()) {
                map.bind(defaultBindings.get(p.getKey()), p.getKey());
                if (!p.getKey().equals(p.getValue())) {
                    map.bind(defaultBindings.get(p.getValue()), p.getValue());
                }
            }
            aliasWidget("." + LineReader.BACKWARD_DELETE_CHAR, LineReader.BACKWARD_DELETE_CHAR);
            if (tailtipEnabled()) {
                callWidget(TT_TOGGLE);
                callWidget(TT_TOGGLE);
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

    /**
     * Creates and manages widgets for as you type command line suggestions.
     * Suggestions are created using a using command history.
     */
    public static class AutosuggestionWidgets extends Widgets {
        private boolean enabled = false;

        public AutosuggestionWidgets(LineReader reader) {
            super(reader);
            if (existsWidget("_autosuggest-forward-char")) {
                throw new IllegalStateException("AutosuggestionWidgets already created!");
            }
            addWidget("_autosuggest-forward-char", this::autosuggestForwardChar);
            addWidget("_autosuggest-end-of-line", this::autosuggestEndOfLine);
            addWidget("_autosuggest-forward-word", this::partialAccept);
            addWidget("autosuggest-toggle", this::toggleKeyBindings);
        }

        public void disable() {
            defaultBindings();
        }

        public void enable() {
            customBindings();
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
            if (enabled) {
                defaultBindings();
            } else {
                customBindings();
            }
            return enabled;
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
        private void customBindings() {
            if (enabled) {
                return;
            }
            aliasWidget("_autosuggest-forward-char", LineReader.FORWARD_CHAR);
            aliasWidget("_autosuggest-end-of-line", LineReader.END_OF_LINE);
            aliasWidget("_autosuggest-forward-word", LineReader.FORWARD_WORD);
            enabled = true;
            setSuggestionType(SuggestionType.HISTORY);
        }

        private void defaultBindings() {
            if (!enabled) {
                return;
            }
            aliasWidget("." + LineReader.FORWARD_CHAR, LineReader.FORWARD_CHAR);
            aliasWidget("." + LineReader.END_OF_LINE, LineReader.END_OF_LINE);
            aliasWidget("." + LineReader.FORWARD_WORD, LineReader.FORWARD_WORD);
            enabled = false;
            setSuggestionType(SuggestionType.NONE);
        }
    }

    /**
     * Creates and manages widgets for as you type command line suggestions.
     * Suggestions are created using a command completer data and/or positional argument descriptions.
     */
    public static class TailTipWidgets extends Widgets {
        public enum TipType {
            /**
             * Prepare command line suggestions using a command positional argument descriptions.
             */
            TAIL_TIP,
            /**
             * Prepare command line suggestions using a command completer data.
             */
            COMPLETER,
            /**
             * Prepare command line suggestions using either a command positional argument descriptions if exists
             * or command completers data.
             */
            COMBINED
        }
        private boolean enabled = false;
        private CommandDescriptions cmdDescs;
        private TipType tipType;
        private int descriptionSize = 0;
        private boolean descriptionEnabled = true;
        private boolean descriptionCache = false;

        /**
         * Creates tailtip widgets used in command line suggestions. Suggestions are created using a command
         * positional argument names. If argument descriptions do not exists command completer data will be used.
         * Status bar for argument descriptions will not be created.
         *
         * @param reader      LineReader.
         * @param tailTips    Commands options and positional argument descriptions.
         * @throws IllegalStateException     If widgets are already created.
         */
        public TailTipWidgets(LineReader reader, Map<String,CmdDesc> tailTips) {
            this(reader, tailTips, 0, TipType.COMBINED);
        }

        /**
         * Creates tailtip widgets used in command line suggestions.
         * Status bar for argument descriptions will not be created.
         *
         * @param reader      LineReader.
         * @param tailTips    Commands options and positional argument descriptions.
         * @param tipType     Defines which data will be used for suggestions.
         * @throws IllegalStateException     If widgets are already created.
         */
        public TailTipWidgets(LineReader reader, Map<String,CmdDesc> tailTips, TipType tipType) {
            this(reader, tailTips, 0, tipType);
        }

        /**
         * Creates tailtip widgets used in command line suggestions. Suggestions are created using a command
         * positional argument names. If argument descriptions do not exists command completer data will be used.
         *
         * @param reader           LineReader.
         * @param tailTips         Commands options and positional argument descriptions.
         * @param descriptionSize  Size of the status bar.
         * @throws IllegalStateException     If widgets are already created.
         */
        public TailTipWidgets(LineReader reader, Map<String,CmdDesc> tailTips, int descriptionSize) {
            this(reader, tailTips, descriptionSize, TipType.COMBINED);
        }

        /**
         * Creates tailtip widgets used in command line suggestions.
         *
         * @param reader           LineReader.
         * @param tailTips         Commands options and  positional argument descriptions.
         * @param descriptionSize  Size of the status bar.
         * @param tipType          Defines which data will be used for suggestions.
         * @throws IllegalStateException     If widgets are already created.
         */
        public TailTipWidgets(LineReader reader, Map<String,CmdDesc> tailTips, int descriptionSize, TipType tipType) {
            this(reader, tailTips, descriptionSize, tipType, null);
        }

        /**
         * Creates tailtip widgets used in command line suggestions.
         *
         * @param reader           LineReader.
         * @param descFun          Function that returns command description.
         * @param descriptionSize  Size of the status bar.
         * @param tipType          Defines which data will be used for suggestions.
         * @throws IllegalStateException     If widgets are already created.
         */
        public TailTipWidgets(LineReader reader, Function<CmdLine,CmdDesc> descFun, int descriptionSize, TipType tipType) {
            this(reader, null, descriptionSize, tipType, descFun);
        }

        private TailTipWidgets(LineReader reader
                             , Map<String,CmdDesc> tailTips
                             , int descriptionSize, TipType tipType, Function<CmdLine,CmdDesc> descFun) {
            super(reader);
            if (existsWidget(TT_ACCEPT_LINE)) {
                throw new IllegalStateException("TailTipWidgets already created!");
            }
            this.cmdDescs = tailTips != null ? new CommandDescriptions(tailTips) : new CommandDescriptions(descFun);
            this.descriptionSize = descriptionSize;
            this.tipType = tipType;
            addWidget(TT_ACCEPT_LINE, this::tailtipAcceptLine);
            addWidget("_tailtip-self-insert", this::tailtipInsert);
            addWidget("_tailtip-backward-delete-char", this::tailtipBackwardDelete);
            addWidget("_tailtip-delete-char", this::tailtipDelete);
            addWidget("_tailtip-expand-or-complete", this::tailtipComplete);
            addWidget("_tailtip-redisplay", this::tailtipUpdateStatus);
            addWidget("tailtip-window", this::toggleWindow);
            addWidget(TT_TOGGLE, this::toggleKeyBindings);
        }

        public void setTailTips(Map<String,CmdDesc> tailTips) {
            cmdDescs.setDescriptions(tailTips);
        }

        public void setDescriptionSize(int descriptionSize) {
            this.descriptionSize = descriptionSize;
            initDescription(descriptionSize);
        }

        public int getDescriptionSize() {
            return descriptionSize;
        }

        public void setTipType(TipType type) {
            this.tipType = type;
            if (tipType == TipType.TAIL_TIP) {
                setSuggestionType(SuggestionType.TAIL_TIP);
            } else {
                setSuggestionType(SuggestionType.COMPLETER);
            }
        }

        public TipType getTipType() {
            return tipType;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void disable() {
            if (enabled) {
                executeWidget(Widgets.TT_TOGGLE);
            }
        }

        public void enable() {
            if (!enabled) {
                toggleKeyBindings();
            }
        }

        public void setDescriptionCache(boolean cache) {
            this.descriptionCache = cache;
        }

        /*
         * widgets
         */
        public boolean tailtipComplete() {
            return doTailTip(LineReader.EXPAND_OR_COMPLETE);
        }

        public boolean tailtipAcceptLine() {
            if (tipType != TipType.TAIL_TIP) {
                setSuggestionType(SuggestionType.COMPLETER);
            }
            clearDescription();
            setErrorPattern(null);
            setErrorIndex(-1);
            cmdDescs.clearTemporaryDescs();
            return clearTailTip(LineReader.ACCEPT_LINE);
        }

        public boolean tailtipBackwardDelete() {
            return doTailTip(autopairEnabled() ? AP_BACKWARD_DELETE_CHAR : LineReader.BACKWARD_DELETE_CHAR);
        }

        private boolean clearTailTip(String widget) {
            clearTailTip();
            callWidget(widget);
            return true;
        }

        public boolean tailtipDelete() {
            clearTailTip();
            return doTailTip(LineReader.DELETE_CHAR);
        }

        public boolean tailtipInsert() {
            return doTailTip(autopairEnabled() ? AP_INSERT : LineReader.SELF_INSERT);
        }

        public boolean tailtipUpdateStatus() {
            return doTailTip(LineReader.REDISPLAY);
        }

        private boolean doTailTip(String widget) {
            Buffer buffer = buffer();
            callWidget(widget);
            Pair<String,Boolean> cmdkey = null;
            List<String> args = args();
            if (buffer.length() == buffer.cursor()) {
                cmdkey = cmdDescs.evaluateCommandLine(buffer.toString(), args);
            } else {
                cmdkey = cmdDescs.evaluateCommandLine(buffer.toString(), buffer.cursor());
            }
            CmdDesc cmdDesc = cmdDescs.getDescription(cmdkey.getU());
            if (cmdDesc == null) {
                setErrorPattern(null);
                setErrorIndex(-1);
                clearDescription();
                resetTailTip();
            } else if (cmdDesc.isValid()) {
                if (cmdkey.getV()) {
                    if (cmdDesc.isCommand() && buffer.length() == buffer.cursor()) {
                        doCommandTailTip(widget, cmdDesc, args);
                    }
                } else {
                    doDescription(compileMainDescription(cmdDesc, descriptionSize));
                    setErrorPattern(cmdDesc.getErrorPattern());
                    setErrorIndex(cmdDesc.getErrorIndex());
                }
            }
            return true;
        }

        private void doCommandTailTip(String widget, CmdDesc cmdDesc, List<String> args) {
            int argnum = 0;
            String prevArg = "";
            for (String a : args) {
                if (!a.startsWith("-")) {
                    if (!prevArg.matches("-[a-zA-Z]{1}") || !cmdDesc.optionWithValue(prevArg)) {
                        argnum++;
                    }
                }
                prevArg = a;
            }
            String lastArg = "";
            prevArg = args.get(args.size() - 1);
            if (!prevChar().equals(" ") && args.size() > 1) {
                lastArg = args.get(args.size() - 1);
                prevArg = args.get(args.size() - 2);
            }
            int bpsize = argnum;
            boolean doTailTip = true;
            boolean noCompleters = false;
            if (widget.endsWith(LineReader.BACKWARD_DELETE_CHAR)) {
                setSuggestionType(SuggestionType.TAIL_TIP);
                noCompleters = true;
                if (!lastArg.startsWith("-")) {
                    if (!prevArg.matches("-[a-zA-Z]{1}") || !cmdDesc.optionWithValue(prevArg)) {
                        bpsize--;
                    }
                }
                if (prevChar().equals(" ")) {
                    bpsize++;
                }
            } else if (!prevChar().equals(" ")) {
                doTailTip = false;
                doDescription(compileMainDescription(cmdDesc, descriptionSize, cmdDesc.isSubcommand() ? lastArg : null));
            } else if (cmdDesc != null) {
                doDescription(compileMainDescription(cmdDesc, descriptionSize));
            }
            if (cmdDesc != null) {
                if (lastArg.startsWith("-")) {
                    if (lastArg.matches("-[a-zA-Z]{1}[a-zA-Z0-9]+")) {
                        if (cmdDesc.optionWithValue(lastArg.substring(0,2))) {
                            doDescription(compileOptionDescription(cmdDesc, lastArg.substring(0,2), descriptionSize));
                            setTipType(tipType);
                        } else {
                            doDescription(compileOptionDescription(cmdDesc, "-" + lastArg.substring(lastArg.length() - 1), descriptionSize));
                            setSuggestionType(SuggestionType.TAIL_TIP);
                            noCompleters = true;
                        }
                    } else {
                        doDescription(compileOptionDescription(cmdDesc, lastArg, descriptionSize));
                        if (!lastArg.contains("=")) {
                            setSuggestionType(SuggestionType.TAIL_TIP);
                            noCompleters = true;
                        } else {
                            setTipType(tipType);
                        }
                    }
                } else if (!widget.endsWith(LineReader.BACKWARD_DELETE_CHAR)){
                    setTipType(tipType);
                }
                if (bpsize > 0 && doTailTip) {
                    List<ArgDesc> params = cmdDesc.getArgsDesc();
                    if (!noCompleters) {
                        setSuggestionType(tipType == TipType.COMPLETER ? SuggestionType.COMPLETER : SuggestionType.TAIL_TIP);
                    }
                    if (bpsize - 1 < params.size()) {
                        if (!lastArg.startsWith("-")) {
                            List<AttributedString> d = null;
                            if (!prevArg.matches("-[a-zA-Z]{1}") || !cmdDesc.optionWithValue(prevArg)) {
                                d = params.get(bpsize - 1).getDescription();
                            } else {
                                d = compileOptionDescription(cmdDesc, prevArg, descriptionSize);
                            }
                            if (d == null || d.isEmpty()) {
                                d = compileMainDescription(cmdDesc, descriptionSize, cmdDesc.isSubcommand() ? lastArg : null);
                            }
                            doDescription(d);
                        }
                        StringBuilder tip = new StringBuilder();
                        for (int i = bpsize - 1; i < params.size(); i++) {
                            tip.append(params.get(i).getName());
                            tip.append(" ");
                        }
                        setTailTip(tip.toString());
                    } else if (!params.isEmpty() && params.get(params.size() - 1).getName().startsWith("[")) {
                        setTailTip(params.get(params.size() - 1).getName());
                        doDescription(params.get(params.size() - 1).getDescription());
                    }
                } else if (doTailTip) {
                    resetTailTip();
                }
            } else {
                clearDescription();
                resetTailTip();
            }
        }

        private void resetTailTip() {
            setTailTip("");
            if (tipType != TipType.TAIL_TIP) {
                setSuggestionType(SuggestionType.COMPLETER);
            }
        }

        private void doDescription(List<AttributedString> desc) {
            if (descriptionSize == 0 || !descriptionEnabled) {
                return;
            }
            if (desc.isEmpty()) {
                clearDescription();
            } else if (desc.size() == descriptionSize) {
                addDescription(desc);
            } else if (desc.size() > descriptionSize) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.append(desc.get(descriptionSize - 1)).append("...", new AttributedStyle(AttributedStyle.INVERSE));
                List<AttributedString> mod = new ArrayList<>(desc.subList(0, descriptionSize-1));
                mod.add(asb.toAttributedString());
                addDescription(mod);
            } else if (desc.size() < descriptionSize) {
                while (desc.size() != descriptionSize) {
                    desc.add(new AttributedString(""));
                }
                addDescription(desc);
            }
        }

        private boolean autopairEnabled() {
            Binding binding = getKeyMap().getBound("(");
            if (binding instanceof Reference && ((Reference)binding).name().equals(AP_INSERT)) {
                return true;
            }
            return false;
        }

        public boolean toggleWindow() {
            descriptionEnabled = !descriptionEnabled;
            if (descriptionEnabled) {
                initDescription(descriptionSize);
            } else {
                destroyDescription();
            }
            callWidget(LineReader.REDRAW_LINE);
            return true;
        }

        public boolean toggleKeyBindings() {
            if (enabled) {
                defaultBindings();
                destroyDescription();
            } else {
                customBindings();
                if (descriptionEnabled) {
                    initDescription(descriptionSize);
                }
            }
            callWidget(LineReader.REDRAW_LINE);
            return enabled;
        }

        /*
         * key bindings...
         *
         */
        private boolean defaultBindings() {
            if (!enabled) {
                return false;
            }
            aliasWidget("." + LineReader.ACCEPT_LINE, LineReader.ACCEPT_LINE);
            aliasWidget("." + LineReader.BACKWARD_DELETE_CHAR, LineReader.BACKWARD_DELETE_CHAR);
            aliasWidget("." + LineReader.DELETE_CHAR, LineReader.DELETE_CHAR);
            aliasWidget("." + LineReader.EXPAND_OR_COMPLETE, LineReader.EXPAND_OR_COMPLETE);
            aliasWidget("." + LineReader.SELF_INSERT, LineReader.SELF_INSERT);
            aliasWidget("." + LineReader.REDISPLAY, LineReader.REDISPLAY);
            KeyMap<Binding> map = getKeyMap();
            map.bind(new Reference(LineReader.INSERT_CLOSE_PAREN), ")");

            setSuggestionType(SuggestionType.NONE);
            if (autopairEnabled()) {
                callWidget(AP_TOGGLE);
                callWidget(AP_TOGGLE);
            }
            enabled = false;
            return true;
        }

        private void customBindings() {
            if (enabled) {
                return;
            }
            aliasWidget(TT_ACCEPT_LINE, LineReader.ACCEPT_LINE);
            aliasWidget("_tailtip-backward-delete-char", LineReader.BACKWARD_DELETE_CHAR);
            aliasWidget("_tailtip-delete-char", LineReader.DELETE_CHAR);
            aliasWidget("_tailtip-expand-or-complete", LineReader.EXPAND_OR_COMPLETE);
            aliasWidget("_tailtip-self-insert", LineReader.SELF_INSERT);
            aliasWidget("_tailtip-redisplay", LineReader.REDISPLAY);
            KeyMap<Binding> map = getKeyMap();
            map.bind(new Reference("_tailtip-self-insert"), ")");

            if (tipType != TipType.TAIL_TIP) {
                setSuggestionType(SuggestionType.COMPLETER);
            } else {
                setSuggestionType(SuggestionType.TAIL_TIP);
            }
            enabled = true;
        }

        private List<AttributedString> compileMainDescription(CmdDesc cmdDesc, int descriptionSize) {
            return  compileMainDescription(cmdDesc, descriptionSize, null);
        }

        private List<AttributedString> compileMainDescription(CmdDesc cmdDesc, int descriptionSize, String lastArg) {
            List<AttributedString> out = new ArrayList<>();
            List<AttributedString> mainDesc = cmdDesc.getMainDesc();
            if (mainDesc == null) {
                return out;
            }
            if (cmdDesc.isCommand() && cmdDesc.isValid() && !cmdDesc.isHighlighted()) {
                mainDesc = new ArrayList<>();
                StyleResolver resolver = HelpException.defaultStyle();
                for (AttributedString as : cmdDesc.getMainDesc()) {
                    mainDesc.add(HelpException.highlightSyntax(as.toString(), resolver));                    
                }
            }
            if (mainDesc.size() <= descriptionSize && lastArg == null) {
                out.addAll(mainDesc);
            } else {
                int tabs = 0;
                int row = 0;
                for (AttributedString as: mainDesc) {
                    if (as.columnLength() >= tabs) {
                        tabs = as.columnLength() + 2;
                    }
                    row++;
                }
                row = 0;
                List<AttributedString> descList = new ArrayList<>();
                for (int i = 0; i < descriptionSize; i++) {
                    descList.add(new AttributedString(""));
                }
                for (AttributedString as: mainDesc) {
                    if (lastArg != null && !as.toString().startsWith(lastArg)) {
                        continue;
                    }
                    AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                    asb.append(descList.get(row));
                    asb.append(as);
                    asb.append("\t");
                    descList.remove(row);
                    descList.add(row, asb.toAttributedString());
                    row++;
                    if (row >= descriptionSize) {
                        row = 0;
                    }
                }
                out = new ArrayList<>(descList);
            }
            return out;
        }

        private List<AttributedString> compileOptionDescription(CmdDesc cmdDesc, String opt, int descriptionSize) {
            List<AttributedString> out = new ArrayList<>();
            Map<String, List<AttributedString>> optsDesc = cmdDesc.getOptsDesc();
            StyleResolver resolver = HelpException.defaultStyle();

            if (!opt.startsWith("-")) {
                return out;
            } else {
                int ind = opt.indexOf("=");
                if (ind > 0) {
                    opt = opt.substring(0, ind);
                }
            }
            List<String> matched = new ArrayList<>();
            int tabs = 0;
            for (String key: optsDesc.keySet()) {
                for (String k: key.split("\\s+")) {
                    if (k.trim().startsWith(opt)) {
                        matched.add(key);
                        if (key.length() >= tabs) {
                            tabs = key.length() + 2;
                        }
                        break;
                    }
                }
            }
            if (matched.size() == 1) {
                out.add(HelpException.highlightSyntax(matched.get(0), resolver));
                for (AttributedString as: optsDesc.get(matched.get(0))) {
                    AttributedStringBuilder asb = new AttributedStringBuilder().tabs(8);
                    asb.append("\t");
                    asb.append(as);
                    out.add(asb.toAttributedString());
                }
            } else if (matched.size() <= descriptionSize) {
                for (String key: matched) {
                    AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                    asb.append(HelpException.highlightSyntax(key, resolver));
                    asb.append("\t");
                    asb.append(cmdDesc.optionDescription(key));
                    out.add(asb.toAttributedString());
                }
            } else if (matched.size() <= 2*descriptionSize) {
                List<AttributedString> keyList = new ArrayList<>();
                int row = 0;
                int columnWidth = 2*tabs;
                while (columnWidth < 50) {
                    columnWidth += tabs;
                }
                for (String key: matched) {
                    AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                    if (row < descriptionSize) {
                        asb.append(HelpException.highlightSyntax(key, resolver));
                        asb.append("\t");
                        asb.append(cmdDesc.optionDescription(key));
                        if (asb.columnLength() > columnWidth - 2) {
                            AttributedString trunc = asb.columnSubSequence(0, columnWidth - 5);
                            asb = new AttributedStringBuilder().tabs(tabs);
                            asb.append(trunc);
                            asb.append("...", new AttributedStyle(AttributedStyle.INVERSE));
                            asb.append("  ");
                        } else {
                            for (int i = asb.columnLength(); i < columnWidth; i++) {
                                asb.append(" ");
                            }
                        }
                        keyList.add(asb.toAttributedString().columnSubSequence(0, columnWidth));
                    } else {
                        asb.append(keyList.get(row - descriptionSize));
                        asb.append(HelpException.highlightSyntax(key, resolver));
                        asb.append("\t");
                        asb.append(cmdDesc.optionDescription(key));
                        keyList.remove(row - descriptionSize);
                        keyList.add(row - descriptionSize, asb.toAttributedString());

                    }
                    row++;
                }
                out = new ArrayList<>(keyList);
            } else {
                List<AttributedString> keyList = new ArrayList<>();
                for (int i = 0; i < descriptionSize; i++) {
                    keyList.add(new AttributedString(""));
                }
                int row = 0;
                for (String key: matched) {
                    AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                    asb.append(keyList.get(row));
                    asb.append(HelpException.highlightSyntax(key, resolver));
                    asb.append("\t");
                    keyList.remove(row);
                    keyList.add(row, asb.toAttributedString());
                    row++;
                    if (row >= descriptionSize) {
                        row = 0;
                    }
                }
                out = new ArrayList<>(keyList);
            }
            return out;
        }

        private class CommandDescriptions {
            Map<String,CmdDesc> descriptions = new HashMap<>();
            Map<String,CmdDesc> temporaryDescs = new HashMap<>();
            Map<String,CmdDesc> volatileDescs = new HashMap<>();
            Function<CmdLine,CmdDesc> descFun;

            public CommandDescriptions(Map<String,CmdDesc> descriptions) {
                this.descriptions = new HashMap<>(descriptions);
            }

            public CommandDescriptions(Function<CmdLine,CmdDesc> descFun) {
                this.descFun = descFun;
            }

            public void setDescriptions(Map<String,CmdDesc> descriptions) {
                this.descriptions = new HashMap<>(descriptions);
            }

            public Pair<String,Boolean> evaluateCommandLine(String line, int curPos) {
                return evaluateCommandLine(line, args(), curPos);
            }

            public Pair<String,Boolean> evaluateCommandLine(String line, List<String> args) {
                return evaluateCommandLine(line, args, line.length());
            }

            private Pair<String,Boolean> evaluateCommandLine(String line, List<String> args, int curPos) {
                String cmd = null;
                CmdLine.DescriptionType descType = CmdLine.DescriptionType.METHOD;
                String head = line.substring(0, curPos);
                String tail = line.substring(curPos);
                if (prevChar().equals(")")) {
                    descType = CmdLine.DescriptionType.SYNTAX;
                    cmd = head;
                } else {
                    if (line.length() == curPos) {
                        cmd = args != null && (args.size() > 1 || (args.size() == 1
                                 && line.endsWith(" "))) ? parser().getCommand(args.get(0)) : null;
                        descType = CmdLine.DescriptionType.COMMAND;
                    }
                    int brackets = 0;
                    for (int i = head.length() - 1; i >= 0; i--) {
                        if (head.charAt(i) == ')') {
                            brackets++;
                        } else if (head.charAt(i) == '(') {
                            brackets--;
                        }
                        if (brackets < 0) {
                            descType = CmdLine.DescriptionType.METHOD;
                            head = head.substring(0, i);
                            cmd = head;
                            break;
                        }
                    }
                    if (descType == CmdLine.DescriptionType.METHOD) {
                        brackets = 0;
                        for (int i = 0; i < tail.length(); i++) {
                            if (tail.charAt(i) == ')') {
                                brackets++;
                            } else if (tail.charAt(i) == '(') {
                                brackets--;
                            }
                            if (brackets > 0) {
                                tail = tail.substring(i + 1);
                                break;
                            }
                        }
                    }
                }
                if (cmd != null && descFun != null) {
                    if (!descriptionCache && descType == CmdLine.DescriptionType.COMMAND) {
                        CmdDesc c = descFun.apply(new CmdLine(line, head, tail, args, descType));
                        volatileDescs.put(cmd, c);
                    } else if (!descriptions.containsKey(cmd) && !temporaryDescs.containsKey(cmd)) {
                        if (descType == CmdLine.DescriptionType.COMMAND) {
                            CmdDesc c = descFun.apply(new CmdLine(line, head, tail, args, descType));
                            if (c != null) {
                                descriptions.put(cmd, c);
                            } else {
                                temporaryDescs.put(cmd, c);
                            }
                        } else if (descType == CmdLine.DescriptionType.METHOD) {
                            temporaryDescs.put(cmd, descFun.apply(new CmdLine(line, head, tail, args, descType)));
                        } else {
                            temporaryDescs.put(cmd, descFun.apply(new CmdLine(line, head, tail, args, descType)));
                        }
                    }
                }
                return new Pair<String,Boolean>(cmd, descType == CmdLine.DescriptionType.COMMAND ? true : false);
            }

            public CmdDesc getDescription(String command) {
                CmdDesc out = null;
                if (descriptions.containsKey(command)) {
                    out = descriptions.get(command);
                } else if (temporaryDescs.containsKey(command)) {
                    out = temporaryDescs.get(command);
                } else if (volatileDescs.containsKey(command)) {
                    out = volatileDescs.get(command);
                    volatileDescs.remove(command);
                }
                return out;
            }

            public void clearTemporaryDescs() {
                temporaryDescs = new HashMap<>();
            }

        }

    }

    public static class CmdLine {
        public enum DescriptionType {
            /**
             * Cursor is at the end of line. The args[0] is completed, the line does not have unclosed opening parenthesis
             * and does not end to the closing parenthesis.
             */
            COMMAND,
            /**
             * The part of the line from beginning til cursor has unclosed opening parenthesis.
             */
            METHOD,
            /**
             * The part of the line from beginning til cursor ends to the closing parenthesis.
             */
            SYNTAX};
        private String line;
        private String head;
        private String tail;
        private List<String> args;
        private DescriptionType descType;

        /**
         * CmdLine class constructor.
         * @param line     Command line
         * @param head     Command line til cursor, method parameters and opening parenthesis before the cursor are removed.
         * @param tail     Command line after cursor, method parameters and closing parenthesis after the cursor are removed.
         * @param args     Parsed command line arguments.
         * @param descType Request COMMAND, METHOD or SYNTAX description
         */
        public CmdLine(String line, String head, String tail, List<String> args, DescriptionType descType) {
            this.line = line;
            this.head = head;
            this.tail = tail;
            this.args = new ArrayList<>(args);
            this.descType = descType;
        }

        public String getLine() {
            return line;
        }

        public String getHead() {
            return head;
        }

        public String getTail() {
            return tail;
        }

        public List<String> getArgs() {
            return args;
        }

        public DescriptionType getDescriptionType() {
            return descType;
        }
    }

    static class Pair<U,V> {
        final U u; final V v;
        public Pair(U u, V v) {
            this.u = u;
            this.v = v;
        }
        public U getU() {
            return u;
        }
        public V getV() {
            return v;
        }
    }

}
