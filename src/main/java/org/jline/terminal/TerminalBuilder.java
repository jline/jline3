/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.impl.AbstractPosixTerminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.impl.ExecPty;
import org.jline.terminal.impl.ExternalTerminal;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.impl.Pty;
import org.jline.terminal.impl.jansi.JansiWinSysTerminal;
import org.jline.terminal.impl.jna.JnaNativePty;
import org.jline.terminal.impl.jna.win.JnaWinSysTerminal;
import org.jline.utils.Log;
import org.jline.utils.OSUtils;

/**
 * Builder class to create terminals.
 */
public final class TerminalBuilder {

    /**
     * Returns the default system terminal.
     * Terminals should be closed properly using the {@link Terminal#close()}
     * method in order to restore the original terminal state.
     *
     * This call is equivalent to:
     * <code>builder().build()</code>
     */
    public static Terminal terminal() throws IOException {
        return builder().build();
    }

    /**
     * Creates a new terminal builder instance.
     */
    public static TerminalBuilder builder() {
        return new TerminalBuilder();
    }

    private String name;
    private InputStream in;
    private OutputStream out;
    private String type;
    private String encoding;
    private Boolean system;
    private boolean jna = true;
    private Boolean dumb;
    private Attributes attributes;
    private Size size;
    private boolean nativeSignals = false;
    private Terminal.SignalHandler signalHandler = Terminal.SignalHandler.SIG_DFL;

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

    public TerminalBuilder jna(boolean jna) {
        this.jna = jna;
        return this;
    }

    public TerminalBuilder dumb(boolean dumb) {
        this.dumb = dumb;
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

    /**
     * Attributes to use when creating a non system terminal,
     * i.e. when the builder has been given the input and
     * outut streams using the {@link #streams(InputStream, OutputStream)} method
     * or when {@link #system(boolean)} has been explicitely called with
     * <code>false</code>.
     *
     * @see #size(Size)
     * @see #system(boolean)
     */
    public TerminalBuilder attributes(Attributes attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * Initial size to use when creating a non system terminal,
     * i.e. when the builder has been given the input and
     * outut streams using the {@link #streams(InputStream, OutputStream)} method
     * or when {@link #system(boolean)} has been explicitely called with
     * <code>false</code>.
     *
     * @see #attributes(Attributes)
     * @see #system(boolean)
     */
    public TerminalBuilder size(Size size) {
        this.size = size;
        return this;
    }

    public TerminalBuilder nativeSignals(boolean nativeSignals) {
        this.nativeSignals = nativeSignals;
        return this;
    }

    public TerminalBuilder signalHandler(Terminal.SignalHandler signalHandler) {
        this.signalHandler = signalHandler;
        return this;
    }

    public Terminal build() throws IOException {
        Terminal terminal = doBuild();
        Log.debug(() -> "Using terminal " + terminal.getClass().getSimpleName());
        if (terminal instanceof AbstractPosixTerminal) {
            Log.debug(() -> "Using pty " + ((AbstractPosixTerminal) terminal).getPty().getClass().getSimpleName());
        }
        return terminal;
    }

    private Terminal doBuild() throws IOException {
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
            type = System.getProperty("org.jline.terminal.type");
        }
        if (type == null) {
            type = System.getenv("TERM");
        }
        Boolean dumb = this.dumb;
        if (dumb == null) {
            String str = System.getProperty("org.jline.terminal.dumb");
            if (str != null) {
                dumb = Boolean.parseBoolean(str);
            }
        }
        if ((system != null && system) || (system == null && in == null && out == null)) {
            if (attributes != null || size != null) {
                Log.warn("Attributes and size fields are ignored when creating a system terminal");
            }
            IllegalStateException exception = new IllegalStateException("Unable to create a system terminal");
            //
            // Cygwin support
            //
            if (OSUtils.IS_CYGWIN) {
                try {
                    Pty pty = ExecPty.current();
                    return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
                } catch (IOException e) {
                    // Ignore if not a tty
                    Log.debug("Error creating exec based pty: ", e.getMessage(), e);
                    exception.addSuppressed(e);
                }
            }
            else if (OSUtils.IS_WINDOWS) {
                if (useJna()) {
                    try {
                        return new JnaWinSysTerminal(name, nativeSignals, signalHandler);
                    } catch (Throwable t) {
                        Log.debug("Error creating JNA based terminal: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
                try {
                    return new JansiWinSysTerminal(name, nativeSignals, signalHandler);
                } catch (Throwable t) {
                    Log.debug("Error creating JANSI based terminal: ", t.getMessage(), t);
                    exception.addSuppressed(t);
                }
            } else {
                Pty pty = null;
                if (useJna()) {
                    try {
                        pty = JnaNativePty.current();
                    } catch (Throwable t) {
                        // ignore
                        Log.debug("Error creating JNA based pty: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
                if (pty == null) {
                    try {
                        pty = ExecPty.current();
                    } catch (Throwable t) {
                        // Ignore if not a tty
                        Log.debug("Error creating exec based pty: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
                if (pty != null) {
                    return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
                }
            }
            if (dumb == null || dumb) {
                if (dumb == null) {
                    if (Log.isDebugEnabled()) {
                        Log.warn("Creating a dumb terminal", exception);
                    } else {
                        Log.warn("Unable to create a system terminal, creating a dumb terminal (enable debug logging for more information)");
                    }
                }
                return new DumbTerminal(name, type != null ? type : Terminal.TYPE_DUMB,
                                        new FileInputStream(FileDescriptor.in),
                                        new FileOutputStream(FileDescriptor.out),
                                        encoding, signalHandler);
            } else {
                throw exception;
            }
        } else {
            if (useJna()) {
                try {
                    Pty pty = JnaNativePty.open(attributes, size);
                    return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler);
                } catch (Throwable t) {
                    Log.debug("Error creating JNA based pty: ", t.getMessage(), t);
                }
            }
            Terminal terminal = new ExternalTerminal(name, type, in, out, encoding, signalHandler);
            if (attributes != null) {
                terminal.setAttributes(attributes);
            }
            if (size != null) {
                terminal.setSize(size);
            }
            return terminal;
        }
    }

    private boolean useJna() {
        return jna;
    }
}
