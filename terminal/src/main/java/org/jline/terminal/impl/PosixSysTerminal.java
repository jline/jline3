/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
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

import org.jline.terminal.Attributes;
import org.jline.utils.NonBlocking;
import org.jline.terminal.spi.Pty;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.ShutdownHooks.Task;
import org.jline.utils.Signals;

public class PosixSysTerminal extends AbstractPosixTerminal {

    protected final NonBlockingInputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;
    private Attributes current;

    public PosixSysTerminal(String name, String type, Pty pty, Charset encoding,
                            boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(name, type, pty, encoding, signalHandler);
        this.input = new PosixInputStream(pty.getSlaveInput());
        this.output = pty.getSlaveOutput();
        this.reader = NonBlocking.nonBlocking(getName(), input, encoding());
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding()));
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
    public void close() throws IOException {
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        super.close();
        // Do not call reader.close()
        reader.shutdown();
    }

    @Override
    public void setAttributes(Attributes attr) {
        super.setAttributes(attr);
        current = new Attributes(attr);
    }

    class PosixInputStream extends NonBlockingInputStream {
        final InputStream in;
        int c = 0;

        PosixInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read(long timeout, boolean isPeek) throws IOException {
            checkInterrupted();
            if (c != 0) {
                int r = c;
                if (!isPeek) {
                    c = 0;
                }
                return r;
            } else {
                setNonBlocking();
                long start = System.currentTimeMillis();
                while (true) {
                    int r = in.read();
                    if (r >= 0) {
                        if (isPeek) {
                            c = r;
                        }
                        return r;
                    }
                    checkInterrupted();
                    long cur = System.currentTimeMillis();
                    if (timeout > 0 && cur - start > timeout) {
                        return NonBlockingInputStream.READ_EXPIRED;
                    }
                }
            }
        }

        private void setNonBlocking() {
            if (current == null
                    || current.getControlChar(Attributes.ControlChar.VMIN) != 0
                    || current.getControlChar(Attributes.ControlChar.VTIME) != 1) {
                Attributes attr = getAttributes();
                attr.setControlChar(Attributes.ControlChar.VMIN, 0);
                attr.setControlChar(Attributes.ControlChar.VTIME, 1);
                setAttributes(attr);
            }
        }
    }

}
