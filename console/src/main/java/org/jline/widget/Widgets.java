/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.widget;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.Parser;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.Reference;
import org.jline.reader.Widget;
import org.jline.utils.AttributedString;
import org.jline.utils.Status;

import static org.jline.keymap.KeyMap.alt;
import static org.jline.keymap.KeyMap.ctrl;

/**
 * Base class for creating custom widgets for JLine's LineReader.
 *
 * This abstract class provides methods for creating, managing, and interacting with
 * widgets in JLine. Widgets are reusable components that can be bound to key sequences
 * and provide specific functionality when invoked. Custom widget implementations should
 * extend this class to leverage its utility methods for widget management.
 */
public abstract class Widgets {
    public static final String TAILTIP_TOGGLE = "tailtip-toggle";
    public static final String TAILTIP_PANE = "tailtip-window";
    public static final String AUTOPAIR_TOGGLE = "autopair-toggle";
    public static final String AUTOSUGGEST_TOGGLE = "autosuggest-toggle";
    protected static final String AP_INSERT = "_autopair-insert";
    protected static final String AP_BACKWARD_DELETE_CHAR = "_autopair-backward-delete-char";
    protected static final String TT_ACCEPT_LINE = "_tailtip-accept-line";

    /** The LineReader instance associated with these widgets */
    protected final LineReader reader;

    /**
     * Creates a new Widgets instance for the specified LineReader.
     *
     * @param reader the LineReader to associate with these widgets
     */
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

    /**
     * Creates a named widget that delegates to the specified widget.
     * This is used internally to create widgets with specific names.
     *
     * @param name the name of the widget
     * @param widget the widget to delegate to
     * @return a new widget that delegates to the specified widget and has the specified name
     */
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
     * Resolve widget name if its alias is given as method parameter.
     * i.e. both method calls getWidget("yank") and getWidget(".yank") will return string ".yank"
     * @param name widget name or alias
     * @return widget name
     */
    public String getWidget(String name) {
        return widget(name).toString();
    }

    /**
     * Test if widget exists
     * @param name widget name or its alias
     * @return true if widget exists
     */
    public boolean existsWidget(String name) {
        try {
            widget(name);
            return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * Resolves a widget name to the corresponding widget.
     * If the name starts with a dot, it is treated as a builtin widget name.
     *
     * @param name the name of the widget to resolve
     * @return the resolved widget
     * @throws InvalidParameterException if no widget with the specified name exists
     */
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
     * Get lineReader's parser
     * @return The parser
     */
    public Parser parser() {
        return reader.getParser();
    }

    /**
     * Get lineReader's Main KeyMap
     * @return The KeyMap
     */
    public KeyMap<Binding> getKeyMap() {
        return reader.getKeyMaps().get(LineReader.MAIN);
    }

    /**
     * Get lineReader's buffer
     * @return The buffer
     */
    public Buffer buffer() {
        return reader.getBuffer();
    }

    /**
     * Replace lineReader buffer
     * @param buffer buffer that will be copied to the LineReader Buffer
     */
    public void replaceBuffer(Buffer buffer) {
        reader.getBuffer().copyFrom(buffer);
    }

    /**
     * Parse lineReader buffer and returns its arguments
     * @return command line arguments
     */
    public List<String> args() {
        return reader.getParser()
                .parse(buffer().toString(), 0, ParseContext.COMPLETE)
                .words();
    }

    /**
     * Access lineReader buffer and return its previous character
     * @return previous character
     */
    public String prevChar() {
        return String.valueOf((char) reader.getBuffer().prevChar());
    }

    /**
     * Access lineReader's buffer and return its current character
     * @return current character
     */
    public String currChar() {
        return String.valueOf((char) reader.getBuffer().currChar());
    }

    /**
     * Get lineReader's last binding
     * @return last binding
     */
    public String lastBinding() {
        return reader.getLastBinding();
    }

    /**
     * Write the string parameter to the lineReader's buffer
     * @param string string to be written
     */
    public void putString(String string) {
        reader.getBuffer().write(string);
    }

    /**
     * Get lineReader's command hint
     * @return Command hint.
     */
    public String tailTip() {
        return reader.getTailTip();
    }

    /**
     * Set lineReader's command hint to be added in the command line
     * @param tailTip command hint
     */
    public void setTailTip(String tailTip) {
        reader.setTailTip(tailTip);
    }

    /**
     * Set errorPattern to the lineReader's highlighter
     * @param errorPattern error pattern
     */
    public void setErrorPattern(Pattern errorPattern) {
        reader.getHighlighter().setErrorPattern(errorPattern);
    }

    /**
     * Set errorIndex to the lineReader's highlighter
     * @param errorIndex error index
     */
    public void setErrorIndex(int errorIndex) {
        reader.getHighlighter().setErrorIndex(errorIndex);
    }

    /**
     *  Clears command line command hint
     */
    public void clearTailTip() {
        reader.setTailTip("");
    }

    /**
     * Set lineReader's autosuggestion type
     * @param type autosuggestion type
     */
    public void setSuggestionType(SuggestionType type) {
        reader.setAutosuggestion(type);
    }

    /**
     * Set description text to the terminal status bar
     * @param desc description text
     */
    public void setDescription(List<AttributedString> desc) {
        Status.getStatus(reader.getTerminal()).update(desc);
    }

    /**
     *  Clears terminal status bar
     */
    public void clearDescription() {
        setDescription(Collections.emptyList());
    }

    /**
     *  Remove terminal status bar
     */
    public void destroyDescription() {
        Status.getExistingStatus(reader.getTerminal()).ifPresent(Status::hide);
    }
}
