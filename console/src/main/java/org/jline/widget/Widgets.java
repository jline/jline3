/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.widget;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.Parser.ParseContext;
import org.jline.utils.AttributedString;
import org.jline.utils.Status;

/**
 * Create custom widgets by extending Widgets class
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public abstract class Widgets {
    public static final String TAILTIP_TOGGLE = "tailtip-toggle";
    public static final String TAILTIP_PANE = "tailtip-window";
    public static final String AUTOPAIR_TOGGLE = "autopair-toggle";
    public static final String AUTOSUGGEST_TOGGLE = "autosuggest-toggle";
    protected static final String AP_INSERT = "_autopair-insert";
    protected static final String AP_BACKWARD_DELETE_CHAR = "_autopair-backward-delete-char";
    protected static final String TT_ACCEPT_LINE = "_tailtip-accept-line";

    protected final LineReader reader;

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
     * @param orig widget original name
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
     * @param desc Text to be displayed on terminal status bar
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
}
