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
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.Pty;
import org.jline.terminal.impl.jansi.JansiWinSysTerminal;
import org.jline.terminal.impl.jna.JnaNativePty;
import org.jline.terminal.impl.jna.win.JnaWinSysTerminal;
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
    private Attributes attributes;
    private Size size;
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

    public TerminalBuilder attributes(Attributes attributes) {
        this.attributes = attributes;
        return this;
    }

    public TerminalBuilder size(Size size) {
        this.size = size;
        return this;
    }

    public Terminal build() throws IOException {
        String name = this.name;
        if (name == null) {
            name = "JLine terminal";
        }
        String encoding = this.encoding;
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }
        String type = this.type;
        if (type == null) {
            type = System.getenv("TERM");
        }
        if ((system != null && system) || (system == null && in == null && out == null)) {
            //
            // Cygwin support
            //
            if (OSUtils.IS_CYGWIN) {
                Pty pty = CygwinPty.current();
                return new PosixSysTerminal(name, type, pty, encoding, nativeSignals);
            }
            else if (OSUtils.IS_WINDOWS) {
                try {
                    return new JnaWinSysTerminal(name, nativeSignals);
                } catch (Throwable t) {
                    return new JansiWinSysTerminal(name, nativeSignals);
                }
            } else {
                Pty pty = null;
                try {
                    pty = JnaNativePty.current();
                } catch (Throwable t) {
                    // ignore
                }
                try {
                    pty = ExecPty.current();
                } catch (IOException e) {
                    // Ignore if not a tty
                }
                if (pty != null) {
                    return new PosixSysTerminal(name, type, pty, encoding, nativeSignals);
                } else {
                    return new ExternalTerminal(name, type, System.in, System.out, encoding);
                }
            }
        } else {
            try {
                Pty pty = JnaNativePty.open(attributes, size);
                return new PosixPtyTerminal(name, type, pty, in, out, encoding);
            } catch (Throwable t) {
                return new ExternalTerminal(name, type, in, out, encoding);
            }
        }
    }
}
