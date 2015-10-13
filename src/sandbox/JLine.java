/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jline.console.ExternalConsole;
import org.jline.console.ExecPty;
import org.jline.console.NativePty;
import org.jline.console.PosixPtyConsole;
import org.jline.console.PosixSysConsole;
import org.jline.console.Pty;
import org.jline.console.WinSysConsole;
import org.jline.reader.ConsoleReaderImpl;
import org.jline.reader.Parser;
import org.jline.reader.history.MemoryHistory;

public final class JLine {

    private JLine() {
    }

    public static Console console() throws IOException {
        return builder().build();
    }

    public static ConsoleBuilder builder() {
        return new ConsoleBuilder();
    }

    public static ConsoleReaderBuilder readerBuilder() {
        return new ConsoleReaderBuilder();
    }

    public static class ConsoleBuilder {

        private InputStream in;
        private OutputStream out;
        private String type;
        private String encoding;
        private Boolean system;
        private Boolean posix;
        private boolean nativeSignals = true;
        private Boolean nativePty;
        private final ConsoleReaderBuilder consoleReaderBuilder = JLine.readerBuilder();

        public ConsoleBuilder() {
        }

        public ConsoleBuilder streams(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            return this;
        }

        public ConsoleBuilder system(boolean system) {
            this.system = system;
            return this;
        }

        public ConsoleBuilder type(String type) {
            this.type = type;
            return this;
        }

        public ConsoleBuilder encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public ConsoleBuilder posix(boolean posix) {
            this.posix = posix;
            return this;
        }

        public ConsoleBuilder appName(String appName) {
            consoleReaderBuilder.appName(appName);
            return this;
        }

        public ConsoleBuilder variables(Map<String, Object> variables) {
            consoleReaderBuilder.variables(variables);
            return this;
        }

        public ConsoleBuilder variable(String name, String value) {
            consoleReaderBuilder.variable(name, value);
            return this;
        }

        public ConsoleBuilder history(History history) {
            consoleReaderBuilder.history(history);
            return this;
        }

        public ConsoleBuilder completer(Completer completer) {
            consoleReaderBuilder.completer(completer);
            return this;
        }

        public ConsoleBuilder highlighter(Highlighter highlighter) {
            consoleReaderBuilder.highlighter(highlighter);
            return this;
        }

        public ConsoleBuilder parser(Parser parser) {
            consoleReaderBuilder.parser(parser);
            return this;
        }

        public ConsoleBuilder nativePty(boolean nativePty) {
            this.nativePty = nativePty;
            return this;
        }

        public Console build() throws IOException {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if ((system != null && system) || (system == null && in == null && out == null)) {
                if (isWindows) {
                    return new WinSysConsole(nativeSignals, consoleReaderBuilder);
                } else {
                    String type = this.type;
                    if (type == null) {
                        type = System.getenv("TERM");
                    }
                    String encoding = this.encoding;
                    if (encoding == null) {
                        encoding = Charset.defaultCharset().name();
                    }
                    Pty pty;
                    if (nativePty == null) {
                        try {
                            pty = NativePty.current();
                        } catch (NoClassDefFoundError e) {
                            // TODO: log
                            pty = ExecPty.current();
                        }
                    } else if (nativePty) {
                        pty = NativePty.current();
                    } else {
                        pty = ExecPty.current();
                    }
                    return new PosixSysConsole(type, consoleReaderBuilder, pty, encoding, nativeSignals);
                }
            } else if ((system != null && !system) || (system == null && in != null && out != null)) {
                if (isWindows || posix == null || !posix) {
                    return new ExternalConsole(type, consoleReaderBuilder, in, out, encoding);
                } else {
                    Pty pty = NativePty.open(null, null); // TODO: non native pty are not supported
                    return new PosixPtyConsole(type, consoleReaderBuilder, pty, in, out, encoding);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public static class ConsoleReaderBuilder {

        Console console;
        String appName;
        Map<String, Object> variables;
        History history;
        Completer completer;
        History memoryHistory;
        Highlighter highlighter;
        Parser parser;

        public ConsoleReaderBuilder() {
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
}
