/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.impl.CygwinPty;
import org.jline.terminal.impl.ExecPty;
import org.jline.terminal.impl.ExternalTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.Pty;
import org.jline.terminal.impl.WinSysTerminal;
import org.jline.utils.OSUtils;

public final class TerminalBuilder {

    public static Terminal terminal() throws IOException {
        return builder().build();
    }

    public static TerminalBuilder builder() {
        return new TerminalBuilder();
    }

    private String name;
    private InputStream in;
    private OutputStream out;
    private String type;
    private String encoding;
    private Boolean system;
    private boolean nativeSignals = true;

    private TerminalBuilder() {
    }

    public TerminalBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TerminalBuilder streams(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        return this;
    }

    public TerminalBuilder system(boolean system) {
        this.system = system;
        return this;
    }

    public TerminalBuilder type(String type) {
        this.type = type;
        return this;
    }

    public TerminalBuilder encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public Terminal build() throws IOException {
        String name = this.name;
        if (name == null) {
            name = "JLine terminal";
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
                return new PosixSysTerminal(name, type, pty, encoding, nativeSignals);
            }
            else if (OSUtils.IS_WINDOWS) {
                return new WinSysTerminal(name, nativeSignals);
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
                return new PosixSysTerminal(name, type, pty, encoding, nativeSignals);
            }
        } else {
            return new ExternalTerminal(name, type, in, out, encoding);
        }
    }
}
