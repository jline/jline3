/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.console.impl.CygwinPty;
import org.jline.console.impl.ExecPty;
import org.jline.console.impl.ExternalConsole;
import org.jline.console.impl.PosixSysConsole;
import org.jline.console.impl.Pty;
import org.jline.console.impl.WinSysConsole;
import org.jline.utils.OSUtils;

public final class ConsoleBuilder {

    public static Console console() throws IOException {
        return builder().build();
    }

    public static ConsoleBuilder builder() {
        return new ConsoleBuilder();
    }

    private String name;
    private InputStream in;
    private OutputStream out;
    private String type;
    private String encoding;
    private Boolean system;
    private boolean nativeSignals = true;

    private ConsoleBuilder() {
    }

    public ConsoleBuilder name(String name) {
        this.name = name;
        return this;
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

    public Console build() throws IOException {
        String name = this.name;
        if (name == null) {
            name = "JLine console";
        }
        if ((system != null && system) || (system == null && in == null && out == null)) {
            //
            // Cygwin support
            //
            if (OSUtils.IS_CYGWIN) {
                String type = this.type;
                if (type == null) {
                    type = System.getenv("TERM");
                }
                String encoding = this.encoding;
                if (encoding == null) {
                    encoding = Charset.defaultCharset().name();
                }
                Pty pty = CygwinPty.current();
                return new PosixSysConsole(name, type, pty, encoding, nativeSignals);
            }
            else if (OSUtils.IS_WINDOWS) {
                return new WinSysConsole(name, nativeSignals);
            } else {
                String type = this.type;
                if (type == null) {
                    type = System.getenv("TERM");
                }
                String encoding = this.encoding;
                if (encoding == null) {
                    encoding = Charset.defaultCharset().name();
                }
                Pty pty = ExecPty.current();
                return new PosixSysConsole(name, type, pty, encoding, nativeSignals);
            }
        } else {
            return new ExternalConsole(name, type, in, out, encoding);
        }
    }
}
