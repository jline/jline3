/*
 * Copyright (c) the original author(s).
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
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.FastBufferedOutputStream;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.OSUtils;
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

    protected final TerminalProvider provider;
    protected final NonBlockingInputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;

    /**
     * Creates a POSIX system terminal backed by the provided PTY, using the same charset for input
     * and output, and optionally enabling native signal handling.
     *
     * @param name the terminal name
     * @param type the terminal type (TERM)
     * @param pty the pseudo-terminal providing slave input/output streams for the terminal
     * @param encoding the charset used for both input and output encoding
     * @param nativeSignals if true, native OS signal handlers will be registered
     * @param signalHandler the initial handler to install for native signals
     * @throws IOException if an I/O error occurs while initializing the terminal
     */
    @SuppressWarnings("this-escape")
    public PosixSysTerminal(
            String name, String type, Pty pty, Charset encoding, boolean nativeSignals, SignalHandler signalHandler)
            throws IOException {
        this(null, name, type, pty, encoding, encoding, encoding, nativeSignals, signalHandler);
    }

    /**
     * Creates a POSIX system terminal backed by the given PTY and character encodings, without a
     * TerminalProvider.
     *
     * @param name the terminal name
     * @param type the terminal type (TERM)
     * @param pty the pseudoterminal providing slave input/output streams
     * @param encoding the primary charset for the terminal
     * @param inputEncoding the charset used for input decoding
     * @param outputEncoding the charset used for output encoding
     * @param nativeSignals whether native signal handlers should be registered
     * @param signalHandler the initial handler to install for native signals
     * @throws IOException if an I/O error occurs while initializing the terminal
     */
    @SuppressWarnings("this-escape")
    public PosixSysTerminal(
            String name,
            String type,
            Pty pty,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            boolean nativeSignals,
            SignalHandler signalHandler)
            throws IOException {
        this(null, name, type, pty, encoding, inputEncoding, outputEncoding, nativeSignals, signalHandler);
    }

    /**
     * Create a POSIX system terminal backed by the provided PTY, initializing non-blocking
     * input/output, reader/writer encodings, and optional native signal handlers.
     *
     * @param provider        optional TerminalProvider used for native signal registration; may be null
     * @param name            the terminal name
     * @param type            the terminal type (TERM)
     * @param pty             the PTY whose slave streams back this terminal
     * @param encoding        the primary charset for the terminal
     * @param inputEncoding   the charset used for the terminal reader
     * @param outputEncoding  the charset used for the terminal writer
     * @param nativeSignals   if true, register native OS signal handlers for all signals
     * @param signalHandler   the initial handler to install for native signals; if `SignalHandler.SIG_DFL`,
     *                        default native handlers will be registered
     * @throws IOException if the PTY streams or terminal I/O cannot be initialized
     */
    @SuppressWarnings({"this-escape", "squid:S107"})
    public PosixSysTerminal(
            TerminalProvider provider,
            String name,
            String type,
            Pty pty,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            boolean nativeSignals,
            SignalHandler signalHandler)
            throws IOException {
        super(name, type, pty, encoding, inputEncoding, outputEncoding, signalHandler);
        this.provider = provider;
        this.input = NonBlocking.nonBlocking(getName(), pty.getSlaveInput());
        this.output = new FastBufferedOutputStream(pty.getSlaveOutput());
        this.reader = NonBlocking.nonBlocking(getName(), input, inputEncoding());
        this.writer = new PrintWriter(new OutputStreamWriter(output, outputEncoding()));
        parseInfoCmp();
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandlers.put(signal, doRegisterDefaultSignal(signal.name()));
                } else {
                    nativeHandlers.put(signal, doRegisterSignal(signal.name(), () -> raise(signal)));
                }
            }
        }
        closer = PosixSysTerminal.this::close;
        ShutdownHooks.add(closer);
    }

    /**
     * Install a new handler for the given signal and synchronize native registration when the handler changes.
     *
     * If the new handler differs from the previous one, registers a native default handler when the new
     * handler is `SignalHandler.SIG_DFL`; otherwise registers a native handler that will raise the signal.
     *
     * @param signal the signal to update
     * @param handler the new handler to install for the signal
     * @return the previous handler for the signal
     */
    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        SignalHandler prev = super.handle(signal, handler);
        if (prev != handler) {
            if (handler == SignalHandler.SIG_DFL) {
                doRegisterDefaultSignal(signal.name());
            } else {
                doRegisterSignal(signal.name(), () -> raise(signal));
            }
        }
        return prev;
    }

    /**
     * Register a native signal handler by name, using the configured TerminalProvider if present.
     *
     * @param name    the signal name (for example, "INT" or "TERM")
     * @param handler the action to run when the signal is received
     * @return        an opaque registration object returned by the underlying registration mechanism;
     *                this value can be passed to the corresponding unregister method to remove the handler
     */
    private Object doRegisterSignal(String name, Runnable handler) {
        return provider != null ? provider.registerSignal(name, handler) : Signals.register(name, handler);
    }

    /**
     * Register the default native handler for the specified signal name.
     *
     * @param name the platform signal name (for example, "INT")
     * @return an opaque registration object representing the native registration; pass this to {@code unregister} when removing the handler
     */
    private Object doRegisterDefaultSignal(String name) {
        return provider != null ? provider.registerDefaultSignal(name) : Signals.registerDefault(name);
    }

    /**
     * Unregisters the native handler associated with the specified signal.
     *
     * @param name         the POSIX signal name (e.g., "INT", "TERM") whose handler should be removed
     * @param registration the registration token returned when the signal was registered
     */
    private void doUnregisterSignal(String name, Object registration) {
        if (provider != null) {
            provider.unregisterSignal(name, registration);
        } else {
            Signals.unregister(name, registration);
        }
    }

    /**
     * Determine if grapheme cluster mode is supported for this terminal.
     *
     * @return `true` if grapheme cluster mode is supported, `false` otherwise; on Windows this always
     *         returns `false` to avoid writing a DECRQM probe to raw stdout/stderr that may not be a
     *         real PTY and could contaminate process output.
     */
    @Override
    public boolean supportsGraphemeClusterMode() {
        // On Windows (Cygwin/MSYSTEM), the slave output goes to a raw
        // FileDescriptor (stdout/stderr) rather than a real PTY device.
        // Writing the DECRQM probe to such a descriptor contaminates the
        // process output when the fd is piped (e.g. subprocess with captured
        // output).  Detecting whether the fd is truly a terminal is unreliable
        // on Windows, so disable the probe entirely.
        if (OSUtils.IS_WINDOWS) {
            return false;
        }
        return super.supportsGraphemeClusterMode();
    }

    public NonBlockingReader reader() {
        checkClosed();
        return reader;
    }

    public PrintWriter writer() {
        checkClosed();
        return writer;
    }

    @Override
    public InputStream input() {
        checkClosed();
        return input;
    }

    @Override
    public OutputStream output() {
        checkClosed();
        return output;
    }

    /**
     * Closes the terminal and releases its resources: flushes pending output, removes the shutdown hook,
     * unregisters any native signal handlers, performs superclass shutdown, and closes the reader.
     *
     * @throws IOException if an I/O error occurs while flushing or closing the terminal streams
     */
    @Override
    protected void doClose() throws IOException {
        writer.flush();
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            doUnregisterSignal(entry.getKey().name(), entry.getValue());
        }
        super.doClose();
        reader.close();
    }
}
