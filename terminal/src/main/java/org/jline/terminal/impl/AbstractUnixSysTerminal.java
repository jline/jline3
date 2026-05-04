/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.FastBufferedOutputStream;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.ShutdownHooks.Task;

/**
 * Base class for flattened POSIX system terminals that bypass the PTY abstraction.
 *
 * <p>Subclasses only need to implement four methods for platform-specific
 * attribute and size operations:</p>
 * <ul>
 *   <li>{@link #doGetAttributes()}</li>
 *   <li>{@link #doSetAttributes(Attributes)}</li>
 *   <li>{@link #doGetSize()}</li>
 *   <li>{@link #doSetSize(Size)}</li>
 * </ul>
 *
 * <p>The call chain is reduced from 7 layers to 4:</p>
 * <pre>
 *   Terminal → AbstractTerminal → AbstractUnixSysTerminal → subclass → native call
 * </pre>
 */
public abstract class AbstractUnixSysTerminal extends AbstractTerminal {

    protected static final int STDIN_FD = 0;
    protected static final int STDOUT_FD = 1;
    protected static final int STDERR_FD = 2;

    private final TerminalProvider provider;
    private final SystemStream systemStream;
    private final Attributes originalAttributes;
    private final boolean nativeSignals;
    private final NonBlockingInputStream input;
    private final OutputStream output;
    private final NonBlockingReader reader;
    private final PrintWriter writer;
    final Map<Signal, Object> nativeHandlers = new ConcurrentHashMap<>();
    private final Task closer;

    @SuppressWarnings({"this-escape", "squid:S107"})
    protected AbstractUnixSysTerminal(
            TerminalProvider provider,
            SystemStream systemStream,
            String name,
            String type,
            Charset encoding,
            Charset inputEncoding,
            Charset outputEncoding,
            boolean nativeSignals,
            SignalHandler signalHandler,
            Attributes originalAttributes)
            throws IOException {
        super(name, type, encoding, inputEncoding, outputEncoding, signalHandler);
        this.provider = provider;
        this.systemStream = systemStream;
        this.originalAttributes = originalAttributes;
        this.nativeSignals = nativeSignals;

        this.input = NonBlocking.nonBlocking(getName(), new FileInputStream(FileDescriptor.in));
        FileDescriptor outFd;
        if (systemStream == SystemStream.Output) {
            outFd = FileDescriptor.out;
        } else if (systemStream == SystemStream.Error) {
            outFd = FileDescriptor.err;
        } else {
            throw new IllegalArgumentException("Invalid system stream for output: " + systemStream);
        }
        this.output = new FastBufferedOutputStream(new FileOutputStream(outFd));
        this.reader = NonBlocking.nonBlocking(getName(), input, inputEncoding());
        this.writer = new PrintWriter(new OutputStreamWriter(output, outputEncoding()));

        parseInfoCmp();

        if (nativeSignals) {
            for (Signal signal : Signal.values()) {
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandlers.put(signal, provider.registerDefaultSignal(signal.name()));
                } else {
                    nativeHandlers.put(signal, provider.registerSignal(signal.name(), () -> raise(signal)));
                }
            }
        }

        closer = this::close;
        ShutdownHooks.add(closer);
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        SignalHandler prev = super.handle(signal, handler);
        if (nativeSignals && prev != handler) {
            Object previousNative = nativeHandlers.remove(signal);
            if (previousNative != null) {
                provider.unregisterSignal(signal.name(), previousNative);
            }
            if (handler == SignalHandler.SIG_DFL) {
                nativeHandlers.put(signal, provider.registerDefaultSignal(signal.name()));
            } else {
                nativeHandlers.put(signal, provider.registerSignal(signal.name(), () -> raise(signal)));
            }
        }
        return prev;
    }

    protected abstract Attributes doGetAttributes();

    protected abstract void doSetAttributes(Attributes attr);

    protected abstract Size doGetSize();

    protected abstract void doSetSize(Size size);

    @Override
    public Attributes getAttributes() {
        checkClosed();
        return doGetAttributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        checkClosed();
        doSetAttributes(attr);
    }

    @Override
    public Size getSize() {
        checkClosed();
        return doGetSize();
    }

    @Override
    public void setSize(Sized size) {
        checkClosed();
        doSetSize(new Size(size));
    }

    @Override
    public NonBlockingReader reader() {
        checkClosed();
        return reader;
    }

    @Override
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

    @Override
    public TerminalProvider getProvider() {
        return provider;
    }

    @Override
    public SystemStream getSystemStream() {
        return systemStream;
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return CursorSupport.getCursorPosition(this, discarded);
    }

    @Override
    protected void doClose() throws IOException {
        writer.flush();
        ShutdownHooks.remove(closer);
        try {
            for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
                try {
                    provider.unregisterSignal(entry.getKey().name(), entry.getValue());
                } catch (Exception ignore) {
                    // best-effort cleanup during close
                }
            }
        } finally {
            try {
                super.doClose();
            } finally {
                try {
                    doSetAttributes(originalAttributes);
                } finally {
                    reader.close();
                }
            }
        }
    }

    @Override
    public int getDefaultForegroundColor() {
        try {
            writer().write("\033]10;?\033\\");
            writer().flush();
            return ColorSupport.parseColorResponse(reader(), 10);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public int getDefaultBackgroundColor() {
        try {
            writer().write("\033]11;?\033\\");
            writer().flush();
            return ColorSupport.parseColorResponse(reader(), 11);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        Size size;
        try {
            size = doGetSize();
        } catch (Exception e) {
            size = null;
        }
        return getKind() + "[" + "name='" + name + '\'' + ", type='" + type + '\'' + ", size='" + size + '\'' + ']';
    }
}
