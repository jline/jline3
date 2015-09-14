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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.console.EmulatedConsole;
import org.jline.console.ExecPty;
import org.jline.console.NativePty;
import org.jline.console.PosixPtyConsole;
import org.jline.console.PosixSysConsole;
import org.jline.console.Pty;
import org.jline.console.WinSysConsole;
import org.jline.reader.ConsoleReaderImpl;
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

        public ConsoleBuilder inputrc(URL inputrc) {
            consoleReaderBuilder.inputrc(inputrc);
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

        public ConsoleBuilder completers(List<Completer> completers) {
            consoleReaderBuilder.completers(completers);
            return this;
        }

        public ConsoleBuilder highlighter(Highlighter highlighter) {
            consoleReaderBuilder.highlighter(highlighter);
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
                    return new EmulatedConsole(type, consoleReaderBuilder, in, out, encoding);
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
        URL inputrc;
        Map<String, String> variables = new HashMap<>();
        History history;
        List<Completer> completers;
        History memoryHistory;
        Highlighter highlighter;

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

        public ConsoleReaderBuilder inputrc(URL inputrc) {
            this.inputrc = inputrc;
            return this;
        }

        public ConsoleReaderBuilder variable(String name, String value) {
            this.variables.put(name, value);
            return this;
        }

        public ConsoleReaderBuilder history(History history) {
            this.history = history;
            return this;
        }

        public ConsoleReaderBuilder completers(List<Completer> completers) {
            this.completers = completers;
            return this;
        }

        public ConsoleReaderBuilder highlighter(Highlighter highlighter) {
            this.highlighter = highlighter;
            return this;
        }

        public ConsoleReader build() {
            ConsoleReaderImpl reader = new ConsoleReaderImpl(console, appName, inputrc, variables);
            if (history != null) {
                reader.setHistory(history);
            } else {
                if (memoryHistory == null) {
                    memoryHistory = new MemoryHistory();
                }
                reader.setHistory(memoryHistory);
            }
            if (completers != null) {
                reader.setCompleters(completers);
            }
            if (highlighter != null) {
                reader.setHighlighter(highlighter);
            }
            return reader;
        }
    }
}
