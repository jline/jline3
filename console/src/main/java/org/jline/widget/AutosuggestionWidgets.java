/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.widget;

import org.jline.reader.Buffer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.impl.BufferImpl;

/**
 * Creates and manages widgets for as you type command line suggestions.
 * Suggestions are created using a using command history.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class AutosuggestionWidgets extends Widgets {
    private boolean enabled = false;

    public AutosuggestionWidgets(LineReader reader) {
        super(reader);
        if (existsWidget("_autosuggest-forward-char")) {
            throw new IllegalStateException("AutosuggestionWidgets already created!");
        }
        addWidget("_autosuggest-forward-char", this::autosuggestForwardChar);
        addWidget("_autosuggest-end-of-line", this::autosuggestEndOfLine);
        addWidget("_autosuggest-forward-word", this::partialAccept);
        addWidget(AUTOSUGGEST_TOGGLE, this::toggleKeyBindings);
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
