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
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.FastBufferedOutputStream;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.NonCloseableInputStream;
import org.jline.utils.NonCloseableOutputStream;
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
 *   <li>{@link #doSetSize(Sized)}</li>
 * </ul>
 *
 * <p>The call chain is reduced from 7 layers to 4:</p>
 * <pre>
 *   Terminal → AbstractTerminal → AbstractUnixSysTerminal → subclass → native call
 * </pre>
 *
 * <p><strong>Important:</strong> the underlying system streams ({@code FileDescriptor.in},
 * {@code FileDescriptor.out}/{@code err}) are wrapped in {@link NonCloseableInputStream} /
 * {@link NonCloseableOutputStream}. Closing the terminal will shut down the pump thread and
 * release resources, but will <em>not</em> close the shared file descriptors. This prevents
 * breaking {@code System.in}/{@code System.out} for the rest of the JVM.</p>
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

        this.input =
                NonBlocking.nonBlocking(getName(), new NonCloseableInputStream(new FileInputStream(FileDescriptor.in)));
        FileDescriptor outFd;
        if (systemStream == SystemStream.Output) {
            outFd = FileDescriptor.out;
        } else if (systemStream == SystemStream.Error) {
            outFd = FileDescriptor.err;
        } else {
            throw new IllegalArgumentException("Invalid system stream for output: " + systemStream);
        }
        this.output = new FastBufferedOutputStream(new NonCloseableOutputStream(new FileOutputStream(outFd)));
        this.reader = NonBlocking.nonBlocking(getName(), input, inputEncoding());
        this.writer = new PrintWriter(new OutputStreamWriter(output, outputEncoding()));

        parseInfoCmp();

        if (nativeSignals) {
            for (Signal signal : Signal.values()) {
                Object nativeHandler;
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandler = provider.registerDefaultSignal(signal.name());
                } else {
                    nativeHandler = provider.registerSignal(signal.name(), () -> raise(signal));
                }
                // Registration returns null for platform-unsupported signals; ConcurrentHashMap rejects null values
                if (nativeHandler != null) {
                    nativeHandlers.put(signal, nativeHandler);
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
            Object nativeHandler;
            if (handler == SignalHandler.SIG_DFL) {
                nativeHandler = provider.registerDefaultSignal(signal.name());
            } else {
                nativeHandler = provider.registerSignal(signal.name(), () -> raise(signal));
            }
            // See constructor — skip null for unsupported signals
            if (nativeHandler != null) {
                nativeHandlers.put(signal, nativeHandler);
            }
        }
        return prev;
    }

    protected abstract Attributes doGetAttributes();

    protected abstract void doSetAttributes(Attributes attr);

    protected abstract Size doGetSize();

    protected abstract void doSetSize(Sized size);

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
        doSetSize(size);
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
                    // Before restoring original terminal attributes, switch to
                    // non-canonical mode with VMIN=0/VTIME=1 so any pump thread
                    // blocked in a native read() on stdin will time out within
                    // 100 ms. On macOS, a blocked read() on a tty can prevent
                    // tcsetattr() from completing, so the pump thread must exit
                    // its read() before we restore original attributes.
                    try {
                        Attributes unblock = doGetAttributes();
                        unblock.setLocalFlag(LocalFlag.ICANON, false);
                        unblock.setControlChar(ControlChar.VMIN, 0);
                        unblock.setControlChar(ControlChar.VTIME, 1);
                        doSetAttributes(unblock);
                    } finally {
                        input.close();
                    }
                } finally {
                    try {
                        doSetAttributes(originalAttributes);
                    } finally {
                        reader.close();
                    }
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
