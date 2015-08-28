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
import java.util.Map;

import org.jline.console.EmulatedConsole;
import org.jline.console.PosixPtyConsole;
import org.jline.console.PosixSysConsole;
import org.jline.console.WinSysConsole;
import org.jline.reader.ReaderImpl;

public final class JLine {

    private JLine() {
    }

    public static ConsoleBuilder console() {
        return new ConsoleBuilder();
    }

    public static ReaderBuilder reader() {
        return new ReaderBuilder();
    }

    public static class ConsoleBuilder {

        private InputStream in;
        private OutputStream out;
        private String type;
        private String encoding;
        private Boolean system;
        private Boolean posix;
        private boolean nativeSignals = true;

        private ConsoleBuilder() {
        }

        public ConsoleBuilder streams(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            return this;
        }

        public ConsoleBuilder system() {
            this.system = true;
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

        public Console build() throws IOException {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if ((system != null && system) || (system == null && in == null && out == null)) {
                if (isWindows) {
                    return new WinSysConsole(nativeSignals);
                } else {
                    String type = this.type;
                    if (type == null) {
                        type = System.getenv("TERM");
                    }
                    String encoding = this.encoding;
                    if (encoding == null) {
                        encoding = Charset.defaultCharset().name();
                    }
                    return new PosixSysConsole(type, encoding, nativeSignals);
                }
            } else if (system != null || (in != null && out != null)) {
                if (isWindows || posix == null || !posix) {
                    return new EmulatedConsole(type, in, out, encoding);
                } else {
                    return new PosixPtyConsole(type, in, out, encoding, null, null);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public static class ReaderBuilder {

        private Console console;
        private String appName;
        private URL inputrc;
        private String keyMap;
        private final Map<String, String> variables = new HashMap<>();

        private ReaderBuilder() {
        }

        public ReaderBuilder console(Console console) {
            this.console = console;
            return this;
        }

        public ReaderBuilder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public ReaderBuilder inputrc(URL inputrc) {
            this.inputrc = inputrc;
            return this;
        }

        public ReaderBuilder keyMap(String keymapName) {
            this.keyMap = keymapName;
            return this;
        }

        public ReaderBuilder variable(String name, String value) {
            this.variables.put(name, value);
            return this;
        }

        public Reader build() throws IOException {
            Console console = this.console;
            if (console == null) {
                console = JLine.console().build();
            }
            ReaderImpl reader = new ReaderImpl(console, appName, inputrc, variables);
            if (keyMap != null) {
                reader.setKeyMap(keyMap);
            }
            return reader;
        }
    }

}
