/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jline.terminal.spi.Pty;
import org.jline.utils.FastBufferedOutputStream;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.ShutdownHooks.Task;
import org.jline.utils.Signals;

/**
 * Terminal implementation for POSIX systems using system streams.
 *
 * <p>
 * The PosixSysTerminal class provides a terminal implementation for POSIX systems
 * (Linux, macOS, etc.) that uses the system standard input and output streams.
 * It extends the AbstractPosixTerminal class and adds functionality specific to
 * system stream-based terminals.
 * </p>
 *
 * <p>
 * This implementation is used when connecting to the actual system terminal, such
 * as when running a console application in a terminal window. It provides access
 * to the standard input and output streams, allowing for interaction with the
 * user through the terminal.
 * </p>
 *
 * <p>
 * Key features of this implementation include:
 * </p>
 * <ul>
 *   <li>Direct access to system standard input and output</li>
 *   <li>Support for terminal attributes and size changes</li>
 *   <li>Support for non-blocking I/O</li>
 *   <li>Automatic restoration of terminal state on shutdown</li>
 * </ul>
 *
 * @see org.jline.terminal.impl.AbstractPosixTerminal
 * @see org.jline.terminal.spi.Pty
 */
public class PosixSysTerminal extends AbstractPosixTerminal {

    protected final NonBlockingInputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;

    @SuppressWarnings("this-escape")
    public PosixSysTerminal(
            String name, String type, Pty pty, Charset encoding, boolean nativeSignals, SignalHandler signalHandler)
            throws IOException {
        this(name, type, pty, encoding, encoding, encoding, encoding, nativeSignals, signalHandler);
    }

    @SuppressWarnings("this-escape")
    public PosixSysTerminal(
            String name,
            String type,
            Pty pty,
            Charset encoding,
            Charset stdinEncoding,
            Charset stdoutEncoding,
            Charset stderrEncoding,
            boolean nativeSignals,
            SignalHandler signalHandler)
            throws IOException {
        super(name, type, pty, encoding, stdinEncoding, stdoutEncoding, stderrEncoding, signalHandler);
        this.input = NonBlocking.nonBlocking(getName(), pty.getSlaveInput());
        this.output = new FastBufferedOutputStream(pty.getSlaveOutput());
        this.reader = NonBlocking.nonBlocking(getName(), input, stdinEncoding());
        this.writer = new PrintWriter(new OutputStreamWriter(output, stdoutEncoding()));
        parseInfoCmp();
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandlers.put(signal, Signals.registerDefault(signal.name()));
                } else {
                    nativeHandlers.put(signal, Signals.register(signal.name(), () -> raise(signal)));
                }
            }
        }
        closer = PosixSysTerminal.this::close;
        ShutdownHooks.add(closer);
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        SignalHandler prev = super.handle(signal, handler);
        if (prev != handler) {
            if (handler == SignalHandler.SIG_DFL) {
                Signals.registerDefault(signal.name());
            } else {
                Signals.register(signal.name(), () -> raise(signal));
            }
        }
        return prev;
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public InputStream input() {
        return input;
    }

    @Override
    public OutputStream output() {
        return output;
    }

    @Override
    protected void doClose() throws IOException {
        writer.flush();
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        super.doClose();
        // Do not call reader.close()
        reader.shutdown();
    }
}
