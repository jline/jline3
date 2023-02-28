/*
 * Copyright (c) 2002-2021, the original author(s).
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
import java.util.function.Function;

import org.jline.builtins.Options.HelpException;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CmdLine;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.Reference;
import org.jline.utils.*;

import static org.jline.keymap.KeyMap.key;

/**
 * Creates and manages widgets for as you type command line suggestions.
 * Suggestions are created using a command completer data and/or positional argument descriptions.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class TailTipWidgets extends Widgets {
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
    private final CommandDescriptions cmdDescs;
    private TipType tipType;
    private int descriptionSize;
    private boolean descriptionEnabled = true;
    private boolean descriptionCache = false;
    private Object readerErrors;

    /**
     * Creates tailtip widgets used in command line suggestions. Suggestions are created using a command
     * positional argument names. If argument descriptions do not exists command completer data will be used.
     * Status bar for argument descriptions will not be created.
     *
     * @param reader      LineReader.
     * @param tailTips    Commands options and positional argument descriptions.
     * @throws IllegalStateException     If widgets are already created.
     */
    public TailTipWidgets(LineReader reader, Map<String, CmdDesc> tailTips) {
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
    public TailTipWidgets(LineReader reader, Map<String, CmdDesc> tailTips, TipType tipType) {
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
    public TailTipWidgets(LineReader reader, Map<String, CmdDesc> tailTips, int descriptionSize) {
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
    public TailTipWidgets(LineReader reader, Map<String, CmdDesc> tailTips, int descriptionSize, TipType tipType) {
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
    public TailTipWidgets(LineReader reader, Function<CmdLine, CmdDesc> descFun, int descriptionSize, TipType tipType) {
        this(reader, null, descriptionSize, tipType, descFun);
    }

    private TailTipWidgets(
            LineReader reader,
            Map<String, CmdDesc> tailTips,
            int descriptionSize,
            TipType tipType,
            Function<CmdLine, CmdDesc> descFun) {
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
        addWidget("_tailtip-kill-line", this::tailtipKillLine);
        addWidget("_tailtip-kill-whole-line", this::tailtipKillWholeLine);
        addWidget("tailtip-window", this::toggleWindow);
        addWidget(TAILTIP_TOGGLE, this::toggleKeyBindings);
    }

    public void setTailTips(Map<String, CmdDesc> tailTips) {
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
            toggleKeyBindings();
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
        if (doTailTip(LineReader.EXPAND_OR_COMPLETE)) {
            if (lastBinding().equals("\t")) {
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
        Pair<String, Boolean> cmdkey;
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
            if (lastArg.startsWith("-")) {
                if (lastArg.matches("-[a-zA-Z][a-zA-Z0-9]+")) {
                    if (cmdDesc.optionWithValue(lastArg.substring(0, 2))) {
                        doDescription(compileOptionDescription(cmdDesc, lastArg.substring(0, 2), descriptionSize));
                        setTipType(tipType);
                    } else {
                        doDescription(compileOptionDescription(
                                cmdDesc, "-" + lastArg.substring(lastArg.length() - 1), descriptionSize));
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
            } else if (!widget.endsWith(LineReader.BACKWARD_DELETE_CHAR)) {
                setTipType(tipType);
            }
            if (bpsize > 0 && doTailTip) {
                List<ArgDesc> params = cmdDesc.getArgsDesc();
                if (!noCompleters) {
                    setSuggestionType(
                            tipType == TipType.COMPLETER ? SuggestionType.COMPLETER : SuggestionType.TAIL_TIP);
                }
                if (bpsize - 1 < params.size()) {
                    if (!lastArg.startsWith("-")) {
                        List<AttributedString> d;
                        if (!prevArg.matches("-[a-zA-Z]") || !cmdDesc.optionWithValue(prevArg)) {
                            d = params.get(bpsize - 1).getDescription();
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
                        tip.append(params.get(i).getName());
                        tip.append(" ");
                    }
                    setTailTip(tip.toString());
                } else if (!params.isEmpty()
                        && params.get(params.size() - 1).getName().startsWith("[")) {
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
            List<AttributedString> mod = new ArrayList<>(desc.subList(0, descriptionSize - 1));
            mod.add(asb.toAttributedString());
            addDescription(mod);
        } else {
            while (desc.size() != descriptionSize) {
                desc.add(new AttributedString(""));
            }
            addDescription(desc);
        }
    }

    private boolean autopairEnabled() {
        Binding binding = getKeyMap().getBound("(");
        return binding instanceof Reference && ((Reference) binding).name().equals(AP_INSERT);
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
            reader.setVariable(LineReader.ERRORS, readerErrors);
        } else {
            customBindings();
            if (descriptionEnabled) {
                initDescription(descriptionSize);
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

    private List<AttributedString> compileMainDescription(CmdDesc cmdDesc, int descriptionSize) {
        return compileMainDescription(cmdDesc, descriptionSize, null);
    }

    private List<AttributedString> compileMainDescription(CmdDesc cmdDesc, int descriptionSize, String lastArg) {
        if (descriptionSize == 0 || !descriptionEnabled) {
            return new ArrayList<>();
        }
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
            for (AttributedString as : mainDesc) {
                if (as.columnLength() >= tabs) {
                    tabs = as.columnLength() + 2;
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

    private List<AttributedString> compileOptionDescription(CmdDesc cmdDesc, String opt, int descriptionSize) {
        if (descriptionSize == 0 || !descriptionEnabled) {
            return new ArrayList<>();
        }
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
            out.add(HelpException.highlightSyntax(matched.get(0), resolver));
            for (AttributedString as : optsDesc.get(matched.get(0))) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(8);
                asb.append("\t");
                asb.append(as);
                out.add(asb.toAttributedString());
            }
        } else if (matched.size() <= descriptionSize) {
            for (String key : matched) {
                AttributedStringBuilder asb = new AttributedStringBuilder().tabs(tabs);
                asb.append(HelpException.highlightSyntax(key, resolver));
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
            for (String key : matched) {
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
        Map<String, CmdDesc> descriptions = new HashMap<>();
        Map<String, CmdDesc> temporaryDescs = new HashMap<>();
        Map<String, CmdDesc> volatileDescs = new HashMap<>();
        Function<CmdLine, CmdDesc> descFun;

        public CommandDescriptions(Map<String, CmdDesc> descriptions) {
            this.descriptions = new HashMap<>(descriptions);
        }

        public CommandDescriptions(Function<CmdLine, CmdDesc> descFun) {
            this.descFun = descFun;
        }

        public void setDescriptions(Map<String, CmdDesc> descriptions) {
            this.descriptions = new HashMap<>(descriptions);
        }

        public Pair<String, Boolean> evaluateCommandLine(String line, int curPos) {
            return evaluateCommandLine(line, args(), curPos);
        }

        public Pair<String, Boolean> evaluateCommandLine(String line, List<String> args) {
            return evaluateCommandLine(line, args, line.length());
        }

        private Pair<String, Boolean> evaluateCommandLine(String line, List<String> args, int curPos) {
            String cmd = null;
            CmdLine.DescriptionType descType = CmdLine.DescriptionType.METHOD;
            String head = line.substring(0, curPos);
            String tail = line.substring(curPos);
            if (prevChar().equals(")")) {
                descType = CmdLine.DescriptionType.SYNTAX;
                cmd = head;
            } else {
                if (line.length() == curPos) {
                    cmd = args != null && (args.size() > 1 || (args.size() == 1 && line.endsWith(" ")))
                            ? parser().getCommand(args.get(0))
                            : null;
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
            if (cmd != null && descFun != null && !descriptions.containsKey(cmd) && !temporaryDescs.containsKey(cmd)) {
                CmdDesc c = descFun.apply(new CmdLine(line, head, tail, args, descType));
                if (descType == CmdLine.DescriptionType.COMMAND) {
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
            return new Pair<>(cmd, descType == CmdLine.DescriptionType.COMMAND);
        }

        public CmdDesc getDescription(String command) {
            CmdDesc out;
            if (descriptions.containsKey(command)) {
                out = descriptions.get(command);
            } else if (temporaryDescs.containsKey(command)) {
                out = temporaryDescs.get(command);
            } else {
                out = volatileDescs.remove(command);
            }
            return out;
        }

        public void clearTemporaryDescs() {
            temporaryDescs.clear();
        }
    }

    static class Pair<U, V> {
        final U u;
        final V v;

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
