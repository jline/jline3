/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.HashMap;
import java.util.Map;

import org.jline.console.Console;
import org.jline.reader.impl.ConsoleReaderImpl;
import org.jline.reader.impl.history.history.MemoryHistory;

public final class ConsoleReaderBuilder {

    public static ConsoleReaderBuilder builder() {
        return new ConsoleReaderBuilder();
    }

    Console console;
    String appName;
    Map<String, Object> variables;
    History history;
    Completer completer;
    History memoryHistory;
    Highlighter highlighter;
    Parser parser;

    private ConsoleReaderBuilder() {
    }

    public ConsoleReaderBuilder console(Console console) {
        this.console = console;
        return this;
    }

    public ConsoleReaderBuilder appName(String appName) {
        this.appName = appName;
        return this;
    }

    public ConsoleReaderBuilder variables(Map<String, Object> variables) {
        Map<String, Object> old = this.variables;
        this.variables = variables;
        if (old != null) {
            this.variables.putAll(old);
        }
        return this;
    }

    public ConsoleReaderBuilder variable(String name, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        this.variables.put(name, value);
        return this;
    }

    public ConsoleReaderBuilder history(History history) {
        this.history = history;
        return this;
    }

    public ConsoleReaderBuilder completer(Completer completer) {
        this.completer = completer;
        return this;
    }

    public ConsoleReaderBuilder highlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
        return this;
    }

    public ConsoleReaderBuilder parser(Parser parser) {
        this.parser = parser;
        return this;
    }

    public ConsoleReader build() {
        ConsoleReaderImpl reader = new ConsoleReaderImpl(console, appName, variables);
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
        return reader;
    }

}
