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
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.Parser.ParseContext;
import org.jline.shell.ArgumentDescription;
import org.jline.shell.CommandDescription;
import org.jline.shell.CommandDispatcher;
import org.jline.shell.CommandLine;
import org.jline.utils.*;

import static org.jline.keymap.KeyMap.key;

/**
 * Provides real-time command-line suggestions as the user types, using
 * {@link CommandDescription} for positional argument hints and option help.
 * <p>
 * Suggestions are displayed either as inline tail-tips after the cursor,
 * as completer-driven suggestions, or both. A terminal status bar can
 * optionally show detailed argument and option descriptions.
 * <p>
 * This widget works with the new shell API ({@link CommandDescription},
 * {@link CommandLine}, {@link ArgumentDescription}) and can be configured
 * with a static map, a dynamic lookup function, or a {@link CommandDispatcher}.
 * <p>
 * Example using a {@link CommandDispatcher}:
 * <pre>
 * CommandTailTipWidgets widgets = new CommandTailTipWidgets(reader, dispatcher, 5);
 * widgets.enable();
 * </pre>
 * <p>
 * Example using a static map:
 * <pre>
 * Map&lt;String, CommandDescription&gt; descriptions = Map.of("ls", lsDesc, "cd", cdDesc);
 * CommandTailTipWidgets widgets = new CommandTailTipWidgets(reader, descriptions, 5, TipType.COMBINED);
 * widgets.enable();
 * </pre>
 *
 * @see CommandDescription
 * @see CommandLine
 * @see CommandDispatcher
 * @since 4.0
 */
public class CommandTailTipWidgets {

    /**
     * The type of suggestions to display.
     */
    public enum TipType {
        /**
         * Show positional argument descriptions as tail-tips.
         */
        TAIL_TIP,
        /**
         * Show completer-driven suggestions.
         */
        COMPLETER,
        /**
         * Show argument descriptions if available, otherwise completer suggestions.
         */
        COMBINED
    }

    private static final String TAILTIP_TOGGLE = "tailtip-toggle";
    private static final String TAILTIP_PANE = "tailtip-window";
    private static final String TT_ACCEPT_LINE = "_tailtip-accept-line";
    private static final String AP_INSERT = "_autopair-insert";
    private static final String AP_BACKWARD_DELETE_CHAR = "_autopair-backward-delete-char";
    private static final String AUTOPAIR_TOGGLE = "autopair-toggle";

    protected final LineReader reader;
    private boolean enabled = false;
    private final DescriptionResolver resolver;
    private TipType tipType;
    private int descriptionSize;
    private boolean descriptionEnabled = true;
    private boolean descriptionCache = false;
    private Object readerErrors;
    private Function<AttributedString, AttributedString> syntaxHighlighter;

    /**
     * Creates widgets using a {@link CommandDispatcher} for dynamic description lookup.
     * <p>
     * Uses {@link TipType#COMBINED} and no status bar.
     *
     * @param reader     the line reader
     * @param dispatcher the command dispatcher
     */
    public CommandTailTipWidgets(LineReader reader, CommandDispatcher dispatcher) {
        this(reader, dispatcher, 0, TipType.COMBINED);
    }

    /**
     * Creates widgets using a {@link CommandDispatcher} with a status bar.
     *
     * @param reader          the line reader
     * @param dispatcher      the command dispatcher
     * @param descriptionSize the number of lines in the status bar (0 for none)
     */
    public CommandTailTipWidgets(LineReader reader, CommandDispatcher dispatcher, int descriptionSize) {
        this(reader, dispatcher, descriptionSize, TipType.COMBINED);
    }

    /**
     * Creates widgets using a {@link CommandDispatcher} with full configuration.
     *
     * @param reader          the line reader
     * @param dispatcher      the command dispatcher
     * @param descriptionSize the number of lines in the status bar (0 for none)
     * @param tipType         the type of suggestions to display
     */
    public CommandTailTipWidgets(
            LineReader reader, CommandDispatcher dispatcher, int descriptionSize, TipType tipType) {
        this(reader, (Map<String, CommandDescription>) null, descriptionSize, tipType, cmdLine -> {
            CommandDescription desc = dispatcher.describe(cmdLine);
            return desc;
        });
    }

    /**
     * Creates widgets using a static map of command descriptions.
     * <p>
     * Uses {@link TipType#COMBINED} and no status bar.
     *
     * @param reader   the line reader
     * @param tailTips map of command names to descriptions
     */
    public CommandTailTipWidgets(LineReader reader, Map<String, CommandDescription> tailTips) {
        this(reader, tailTips, 0, TipType.COMBINED);
    }

    /**
     * Creates widgets using a static map with a specific tip type.
     *
     * @param reader   the line reader
     * @param tailTips map of command names to descriptions
     * @param tipType  the type of suggestions to display
     */
    public CommandTailTipWidgets(LineReader reader, Map<String, CommandDescription> tailTips, TipType tipType) {
        this(reader, tailTips, 0, tipType);
    }

    /**
     * Creates widgets using a static map with a status bar.
     *
     * @param reader          the line reader
     * @param tailTips        map of command names to descriptions
     * @param descriptionSize the number of lines in the status bar (0 for none)
     */
    public CommandTailTipWidgets(LineReader reader, Map<String, CommandDescription> tailTips, int descriptionSize) {
        this(reader, tailTips, descriptionSize, TipType.COMBINED);
    }

    /**
     * Creates widgets using a static map with full configuration.
     *
     * @param reader          the line reader
     * @param tailTips        map of command names to descriptions
     * @param descriptionSize the number of lines in the status bar (0 for none)
     * @param tipType         the type of suggestions to display
     */
    public CommandTailTipWidgets(
            LineReader reader, Map<String, CommandDescription> tailTips, int descriptionSize, TipType tipType) {
        this(reader, tailTips, descriptionSize, tipType, null);
    }

    /**
     * Creates widgets using a dynamic description function.
     *
     * @param reader          the line reader
     * @param descFun         function that returns a description for a command line
     * @param descriptionSize the number of lines in the status bar (0 for none)
     * @param tipType         the type of suggestions to display
     */
    public CommandTailTipWidgets(
            LineReader reader,
            Function<CommandLine, CommandDescription> descFun,
            int descriptionSize,
            TipType tipType) {
        this(reader, null, descriptionSize, tipType, descFun);
    }

    @SuppressWarnings("this-escape")
    private CommandTailTipWidgets(
            LineReader reader,
            Map<String, CommandDescription> tailTips,
            int descriptionSize,
            TipType tipType,
            Function<CommandLine, CommandDescription> descFun) {
        this.reader = reader;
        if (existsWidget(TT_ACCEPT_LINE)) {
            throw new IllegalStateException("CommandTailTipWidgets already created!");
        }
        this.resolver = tailTips != null ? new DescriptionResolver(tailTips) : new DescriptionResolver(descFun);
        this.descriptionSize = descriptionSize;
        this.tipType = tipType;
        addWidget(TT_ACCEPT_LINE, this::tailtipAcceptLine);
        addWidget("_tailtip-self-insert", this::tailtipInsert);
        addWidget("_tailtip-backward-delete-char", this::tailtipBackwardDelete);
        addWidget("_tailtip-delete-char", this::tailtipDelete);
        addWidget("_tailtip-expand-or-complete", this::tailtipComplete);
        addWidget("_tailtip-redisplay", this::tailtipUpdateStatus);
        addWidget("_tailtip-kill-line", this::tailtipKillLine);
        addWidget("_tailtip-kill-whole-line", this::tailtipKillWholeLine);
        addWidget(TAILTIP_PANE, this::toggleWindow);
        addWidget(TAILTIP_TOGGLE, this::toggleKeyBindings);
    }

    /**
     * Sets a function to highlight syntax in description text.
     * <p>
     * If set, this function is applied to main description lines when
     * the description is not already marked as highlighted.
     *
     * @param highlighter function to highlight an attributed string
     */
    public void setSyntaxHighlighter(Function<AttributedString, AttributedString> highlighter) {
        this.syntaxHighlighter = highlighter;
    }

    /**
     * Replaces the description map with a new set of command descriptions.
     *
     * @param tailTips the new command descriptions
     */
    public void setDescriptions(Map<String, CommandDescription> tailTips) {
        resolver.setDescriptions(tailTips);
    }

    /**
     * Sets the status bar size.
     *
     * @param descriptionSize the number of lines (0 to disable)
     */
    public void setDescriptionSize(int descriptionSize) {
        this.descriptionSize = descriptionSize;
        initDescription();
    }

    /**
     * Returns the current status bar size.
     *
     * @return the description size
     */
    public int descriptionSize() {
        return descriptionSize;
    }

    /**
     * Sets the tip type.
     *
     * @param type the new tip type
     */
    public void setTipType(TipType type) {
        this.tipType = type;
        if (tipType == TipType.TAIL_TIP) {
            setSuggestionType(SuggestionType.TAIL_TIP);
        } else {
            setSuggestionType(SuggestionType.COMPLETER);
        }
    }

    /**
     * Returns the current tip type.
     *
     * @return the tip type
     */
    public TipType tipType() {
        return tipType;
    }

    /**
     * Returns whether the widgets are currently enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Disables the widgets, restoring default key bindings.
     */
    public void disable() {
        if (enabled) {
            toggleKeyBindings();
        }
    }

    /**
     * Enables the widgets, installing custom key bindings.
     */
    public void enable() {
        if (!enabled) {
            toggleKeyBindings();
        }
    }

    /**
     * Sets whether resolved descriptions should be cached permanently.
     *
     * @param cache true to cache descriptions
     */
    public void setDescriptionCache(boolean cache) {
        this.descriptionCache = cache;
    }

    // -- widget methods --

    public boolean tailtipComplete() {
        if (doTailTip(LineReader.EXPAND_OR_COMPLETE)) {
            if ("\t".equals(lastBinding())) {
                callWidget(LineReader.BACKWARD_CHAR);
                reader.runMacro(key(reader.getTerminal(), InfoCmp.Capability.key_right));
            }
            return true;
        }
        return false;
    }

    public boolean tailtipAcceptLine() {
        if (tipType != TipType.TAIL_TIP) {
            setSuggestionType(SuggestionType.COMPLETER);
        }
        clearDescription();
        setErrorPattern(null);
        setErrorIndex(-1);
        resolver.clearTemporaryDescs();
        return clearTailTip(LineReader.ACCEPT_LINE);
    }

    public boolean tailtipBackwardDelete() {
        return doTailTip(autopairEnabled() ? AP_BACKWARD_DELETE_CHAR : LineReader.BACKWARD_DELETE_CHAR);
    }

    public boolean tailtipDelete() {
        clearTailTip();
        return doTailTip(LineReader.DELETE_CHAR);
    }

    public boolean tailtipKillLine() {
        clearTailTip();
        return doTailTip(LineReader.KILL_LINE);
    }

    public boolean tailtipKillWholeLine() {
        callWidget(LineReader.KILL_WHOLE_LINE);
        return doTailTip(LineReader.REDISPLAY);
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
        List<String> args = args();
        Pair<String, Boolean> cmdkey = resolver.evaluateCommandLine(buffer.toString(), args, buffer.cursor());
        CommandDescription cmdDesc = resolver.getDescription(cmdkey.first);
        if (cmdDesc == null) {
            setErrorPattern(null);
            setErrorIndex(-1);
            clearDescription();
            resetTailTip();
        } else if (cmdDesc.isValid()) {
            if (cmdkey.second) {
                if (cmdDesc.isCommand() && buffer.length() == buffer.cursor()) {
                    doCommandTailTip(widget, cmdDesc, args);
                }
            } else {
                doDescription(compileMainDescription(cmdDesc, descriptionSize));
                setErrorPattern(cmdDesc.errorPattern());
                setErrorIndex(cmdDesc.errorIndex());
            }
        }
        return true;
    }

    private void doCommandTailTip(String widget, CommandDescription cmdDesc, List<String> args) {
        int argnum = 0;
        String prevArg = "";
        for (String a : args) {
            if (!a.startsWith("-")) {
                if (!prevArg.matches("-[a-zA-Z]") || !cmdDesc.optionWithValue(prevArg)) {
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
                if (!prevArg.matches("-[a-zA-Z]") || !cmdDesc.optionWithValue(prevArg)) {
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
            if (prevArg.startsWith("-")
                    && !prevArg.contains("=")
                    && !prevArg.matches("-[a-zA-Z][\\S]+")
                    && cmdDesc.optionWithValue(prevArg)) {
                doDescription(compileOptionDescription(cmdDesc, prevArg, descriptionSize));
                setTipType(tipType);
            } else if (lastArg.matches("-[a-zA-Z][\\S]+") && cmdDesc.optionWithValue(lastArg.substring(0, 2))) {
                doDescription(compileOptionDescription(cmdDesc, lastArg.substring(0, 2), descriptionSize));
                setTipType(tipType);
            } else if (lastArg.startsWith("-")) {
                doDescription(compileOptionDescription(cmdDesc, lastArg, descriptionSize));
                if (!lastArg.contains("=")) {
                    setSuggestionType(SuggestionType.TAIL_TIP);
                    noCompleters = true;
                } else {
                    setTipType(tipType);
                }
            } else if (!widget.endsWith(LineReader.BACKWARD_DELETE_CHAR)) {
                setTipType(tipType);
            }
            if (bpsize > 0 && doTailTip) {
                List<ArgumentDescription> params = cmdDesc.arguments();
                if (!noCompleters) {
                    setSuggestionType(
                            tipType == TipType.COMPLETER ? SuggestionType.COMPLETER : SuggestionType.TAIL_TIP);
                }
                if (bpsize - 1 < params.size()) {
                    if (!lastArg.startsWith("-")) {
                        List<AttributedString> d;
                        if (!prevArg.startsWith("-") || !cmdDesc.optionWithValue(prevArg)) {
                            d = params.get(bpsize - 1).description();
                        } else {
                            d = compileOptionDescription(cmdDesc, prevArg, descriptionSize);
                        }
                        if (d == null || d.isEmpty()) {
                            d = compileMainDescription(
                                    cmdDesc, descriptionSize, cmdDesc.isSubcommand() ? lastArg : null);
                        }
                        doDescription(d);
                    }
                    StringBuilder tip = new StringBuilder();
                    for (int i = bpsize - 1; i < params.size(); i++) {
                        tip.append(params.get(i).name());
                        tip.append(" ");
                    }
                    setTailTip(tip.toString());
                } else if (!params.isEmpty()
                        && params.get(params.size() - 1).name().startsWith("[")) {
                    setTailTip(params.get(params.size() - 1).name());
                    doDescription(params.get(params.size() - 1).description());
                }
            } else if (doTailTip) {
                resetTailTip();
            }
        } else {
            clearDescription();
            resetTailTip();
        }
    }

    private boolean clearTailTip(String widget) {
        clearTailTip();
        callWidget(widget);
        return true;
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
        List<AttributedString> list = desc;
        if (list.size() > descriptionSize) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(list.get(descriptionSize - 1)).append("…", new AttributedStyle(AttributedStyle.INVERSE));
            List<AttributedString> mod = new ArrayList<>(list.subList(0, descriptionSize - 1));
            mod.add(asb.toAttributedString());
            list = mod;
        } else if (list.size() < descriptionSize) {
            List<AttributedString> mod = new ArrayList<>(list);
            while (mod.size() != descriptionSize) {
                mod.add(new AttributedString(""));
            }
            list = mod;
        }
        setDescription(list);
    }

    /**
     * Initializes the terminal status bar with a border.
     */
    public void initDescription() {
        Status.getStatus(reader.getTerminal()).setBorder(true);
        clearDescription();
    }

    public boolean toggleWindow() {
        descriptionEnabled = !descriptionEnabled;
        if (descriptionEnabled) {
            initDescription();
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
            reader.setVariable(LineReader.ERRORS, readerErrors);
        } else {
            customBindings();
            if (descriptionEnabled) {
                initDescription();
            }
            readerErrors = reader.getVariable(LineReader.ERRORS);
            reader.setVariable(LineReader.ERRORS, 0);
        }
        try {
            callWidget(LineReader.REDRAW_LINE);
        } catch (Exception e) {
            // ignore
        }
        return enabled;
    }

    // -- key binding management --

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
        aliasWidget("." + LineReader.KILL_LINE, LineReader.KILL_LINE);
        aliasWidget("." + LineReader.KILL_WHOLE_LINE, LineReader.KILL_WHOLE_LINE);
        KeyMap<Binding> map = getKeyMap();
        map.bind(new Reference(LineReader.INSERT_CLOSE_PAREN), ")");

        setSuggestionType(SuggestionType.NONE);
        if (autopairEnabled()) {
            callWidget(AUTOPAIR_TOGGLE);
            callWidget(AUTOPAIR_TOGGLE);
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
        aliasWidget("_tailtip-kill-line", LineReader.KILL_LINE);
        aliasWidget("_tailtip-kill-whole-line", LineReader.KILL_WHOLE_LINE);
        KeyMap<Binding> map = getKeyMap();
        map.bind(new Reference("_tailtip-self-insert"), ")");

        if (tipType != TipType.TAIL_TIP) {
            setSuggestionType(SuggestionType.COMPLETER);
        } else {
            setSuggestionType(SuggestionType.TAIL_TIP);
        }
        enabled = true;
    }

    // -- description compilation --

    private List<AttributedString> compileMainDescription(CommandDescription cmdDesc, int descriptionSize) {
        return compileMainDescription(cmdDesc, descriptionSize, null);
    }

    private List<AttributedString> compileMainDescription(
            CommandDescription cmdDesc, int descriptionSize, String lastArg) {
        if (descriptionSize == 0 || !descriptionEnabled) {
            return new ArrayList<>();
        }
        List<AttributedString> out = new ArrayList<>();
        List<AttributedString> mainDesc = cmdDesc.mainDescription();
        if (mainDesc == null || mainDesc.isEmpty()) {
            return out;
        }
        if (cmdDesc.isCommand() && cmdDesc.isValid() && !cmdDesc.isHighlighted() && syntaxHighlighter != null) {
            List<AttributedString> highlighted = new ArrayList<>();
            for (AttributedString as : mainDesc) {
                highlighted.add(syntaxHighlighter.apply(as));
            }
            mainDesc = highlighted;
        }
        if (mainDesc.size() <= descriptionSize && lastArg == null) {
            out.addAll(mainDesc);
        } else {
            int tabs = 0;
            for (AttributedString as : mainDesc) {
                if (as.columnLength(reader.getTerminal()) >= tabs) {
                    tabs = as.columnLength(reader.getTerminal()) + 2;
                }
            }
            int row = 0;
            int col = 0;
            List<AttributedString> descList = new ArrayList<>();
            for (int i = 0; i < descriptionSize; i++) {
                descList.add(new AttributedString(""));
            }
            for (AttributedString as : mainDesc) {
                if (lastArg != null && !as.toString().startsWith(lastArg)) {
                    continue;
                }
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                if (col > 0) {
                    asb.append(descList.get(row));
                    asb.append("\t");
                }
                asb.append(as);
                descList.remove(row);
                descList.add(row, asb.toAttributedString());
                row++;
                if (row >= descriptionSize) {
                    row = 0;
                    col++;
                }
            }
            out = new ArrayList<>(descList);
        }
        return out;
    }

    private List<AttributedString> compileOptionDescription(
            CommandDescription cmdDesc, String opt, int descriptionSize) {
        if (descriptionSize == 0 || !descriptionEnabled) {
            return new ArrayList<>();
        }
        List<AttributedString> out = new ArrayList<>();
        Map<String, List<AttributedString>> optsDesc = cmdDesc.options();

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
        for (String key : optsDesc.keySet()) {
            for (String k : key.split("\\s+")) {
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
            out.add(highlightOption(matched.get(0)));
            for (AttributedString as : optsDesc.get(matched.get(0))) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(8);
                asb.append("\t");
                asb.append(as);
                out.add(asb.toAttributedString());
            }
        } else if (matched.size() <= descriptionSize) {
            for (String key : matched) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                asb.append(highlightOption(key));
                asb.append("\t");
                asb.append(cmdDesc.optionDescription(key));
                out.add(asb.toAttributedString());
            }
        } else if (matched.size() <= 2 * descriptionSize) {
            List<AttributedString> keyList = new ArrayList<>();
            int row = 0;
            int columnWidth = 2 * tabs;
            while (columnWidth < 50) {
                columnWidth += tabs;
            }
            for (String key : matched) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                if (row < descriptionSize) {
                    asb.append(highlightOption(key));
                    asb.append("\t");
                    asb.append(cmdDesc.optionDescription(key));
                    if (asb.columnLength(reader.getTerminal()) > columnWidth - 2) {
                        AttributedString trunc = asb.columnSubSequence(0, columnWidth - 5, reader.getTerminal());
                        asb = new AttributedStringBuilder().tabs(tabs);
                        asb.append(trunc);
                        asb.append("...", new AttributedStyle(AttributedStyle.INVERSE));
                        asb.append("  ");
                    } else {
                        for (int i = asb.columnLength(reader.getTerminal()); i < columnWidth; i++) {
                            asb.append(" ");
                        }
                    }
                    keyList.add(asb.toAttributedString().columnSubSequence(0, columnWidth, reader.getTerminal()));
                } else {
                    asb.append(keyList.get(row - descriptionSize));
                    asb.append(highlightOption(key));
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
            for (String key : matched) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                asb.append(keyList.get(row));
                asb.append(highlightOption(key));
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

    private AttributedString highlightOption(String option) {
        if (syntaxHighlighter != null) {
            return syntaxHighlighter.apply(new AttributedString(option));
        }
        return new AttributedString(option);
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

    private Parser parser() {
        return reader.getParser();
    }

    private KeyMap<Binding> getKeyMap() {
        return reader.getKeyMaps().get(LineReader.MAIN);
    }

    private Buffer buffer() {
        return reader.getBuffer();
    }

    private List<String> args() {
        return reader.getParser()
                .parse(buffer().toString(), 0, ParseContext.COMPLETE)
                .words();
    }

    private String prevChar() {
        return String.valueOf((char) reader.getBuffer().prevChar());
    }

    private String lastBinding() {
        return reader.getLastBinding();
    }

    private void setTailTip(String tailTip) {
        reader.setTailTip(tailTip);
    }

    private void clearTailTip() {
        reader.setTailTip("");
    }

    private void setSuggestionType(SuggestionType type) {
        reader.setAutosuggestion(type);
    }

    private void setDescription(List<AttributedString> desc) {
        Status.getStatus(reader.getTerminal()).update(desc);
    }

    private void clearDescription() {
        doDescription(Collections.emptyList());
    }

    private void destroyDescription() {
        Status.getExistingStatus(reader.getTerminal()).ifPresent(Status::hide);
    }

    private void setErrorPattern(Pattern errorPattern) {
        reader.getHighlighter().setErrorPattern(errorPattern);
    }

    private void setErrorIndex(int errorIndex) {
        reader.getHighlighter().setErrorIndex(errorIndex);
    }

    private boolean autopairEnabled() {
        Binding binding = getKeyMap().getBound("(");
        return binding instanceof Reference && ((Reference) binding).name().equals(AP_INSERT);
    }

    // -- description resolver --

    private class DescriptionResolver {
        Map<String, CommandDescription> descriptions = new HashMap<>();
        Map<String, CommandDescription> temporaryDescs = new HashMap<>();
        Map<String, CommandDescription> volatileDescs = new HashMap<>();
        Function<CommandLine, CommandDescription> descFun;

        DescriptionResolver(Map<String, CommandDescription> descriptions) {
            this.descriptions = new HashMap<>(descriptions);
        }

        DescriptionResolver(Function<CommandLine, CommandDescription> descFun) {
            this.descFun = descFun;
        }

        void setDescriptions(Map<String, CommandDescription> descriptions) {
            this.descriptions = new HashMap<>(descriptions);
        }

        Pair<String, Boolean> evaluateCommandLine(String line, List<String> args, int curPos) {
            String cmd = null;
            CommandLine.Type descType = CommandLine.Type.METHOD;
            String head = line.substring(0, curPos);
            String tail = line.substring(curPos);
            if (prevChar().equals(")")) {
                descType = CommandLine.Type.SYNTAX;
                cmd = head;
            } else {
                if (line.length() == curPos) {
                    cmd = args != null && (args.size() > 1 || (args.size() == 1 && line.endsWith(" ")))
                            ? parser().getCommand(args.get(0))
                            : null;
                    descType = CommandLine.Type.COMMAND;
                }
                int brackets = 0;
                for (int i = head.length() - 1; i >= 0; i--) {
                    if (head.charAt(i) == ')') {
                        brackets++;
                    } else if (head.charAt(i) == '(') {
                        brackets--;
                    }
                    if (brackets < 0) {
                        descType = CommandLine.Type.METHOD;
                        head = head.substring(0, i);
                        cmd = head;
                        break;
                    }
                }
                if (descType == CommandLine.Type.METHOD) {
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
            if (cmd != null && descFun != null && !descriptions.containsKey(cmd) && !temporaryDescs.containsKey(cmd)) {
                CommandDescription c = descFun.apply(new CommandLine(line, head, tail, args, descType));
                if (descType == CommandLine.Type.COMMAND) {
                    if (!descriptionCache) {
                        volatileDescs.put(cmd, c);
                    } else if (c != null) {
                        descriptions.put(cmd, c);
                    } else {
                        temporaryDescs.put(cmd, null);
                    }
                } else {
                    temporaryDescs.put(cmd, c);
                }
            }
            return new Pair<>(cmd, descType == CommandLine.Type.COMMAND);
        }

        CommandDescription getDescription(String command) {
            CommandDescription out;
            if (descriptions.containsKey(command)) {
                out = descriptions.get(command);
            } else if (temporaryDescs.containsKey(command)) {
                out = temporaryDescs.get(command);
            } else {
                out = volatileDescs.remove(command);
            }
            return out;
        }

        void clearTemporaryDescs() {
            temporaryDescs.clear();
        }
    }

    private static class Pair<A, B> {
        final A first;
        final B second;

        Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }
}
