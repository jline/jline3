/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.HashMap;
import java.util.Map;

import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.history.MemoryHistory;
import org.jline.terminal.Terminal;

public final class LineReaderBuilder {

    public static LineReaderBuilder builder() {
        return new LineReaderBuilder();
    }

    Terminal terminal;
    String appName;
    Map<String, Object> variables;
    History history;
    Completer completer;
    History memoryHistory;
    Highlighter highlighter;
    Parser parser;
    Expander expander;

    private LineReaderBuilder() {
    }

    public LineReaderBuilder terminal(Terminal terminal) {
        this.terminal = terminal;
        return this;
    }

    public LineReaderBuilder appName(String appName) {
        this.appName = appName;
        return this;
    }

    public LineReaderBuilder variables(Map<String, Object> variables) {
        Map<String, Object> old = this.variables;
        this.variables = variables;
        if (old != null) {
            this.variables.putAll(old);
        }
        return this;
    }

    public LineReaderBuilder variable(String name, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        this.variables.put(name, value);
        return this;
    }

    public LineReaderBuilder history(History history) {
        this.history = history;
        return this;
    }

    public LineReaderBuilder completer(Completer completer) {
        this.completer = completer;
        return this;
    }

    public LineReaderBuilder highlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
        return this;
    }

    public LineReaderBuilder parser(Parser parser) {
        this.parser = parser;
        return this;
    }

    public LineReaderBuilder expander(Expander expander) {
        this.expander = expander;
        return this;
    }

    public LineReader build() {
        LineReaderImpl reader = new LineReaderImpl(terminal, appName, variables);
        if (history != null) {
            reader.setHistory(history);
        } else {
            if (memoryHistory == null) {
                memoryHistory = new MemoryHistory();
            }
            reader.setHistory(memoryHistory);
        }
        if (completer != null) {
            reader.setCompleter(completer);
        }
        if (highlighter != null) {
            reader.setHighlighter(highlighter);
        }
        if (parser != null) {
            reader.setParser(parser);
        }
        if (expander != null) {
            reader.setExpander(expander);
        }
        return reader;
    }

}
