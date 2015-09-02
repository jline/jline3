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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.jline.JLine.ConsoleReaderBuilder;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.ShutdownHooks.Task;
import org.jline.utils.Signals;

import static org.jline.utils.Preconditions.checkNotNull;

public class PosixSysConsole extends AbstractPosixConsole {

    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;

    public PosixSysConsole(String type, ConsoleReaderBuilder consoleReaderBuilder, Pty pty, String encoding, boolean nativeSignals) throws IOException {
        super(type, consoleReaderBuilder, pty);
        checkNotNull(encoding);
        this.reader = new NonBlockingReader(new InputStreamReader(pty.getSlaveInput(), encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(pty.getSlaveOutput(), encoding));
        parseInfoCmp();
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                nativeHandlers.put(signal, Signals.register(signal.name(), new Runnable() {
                    public void run() {
                        raise(signal);
                    }
                }));
            }
        }
        closer = new Task() {
            @Override
            public void run() throws Exception {
                close();
            }
        };
        ShutdownHooks.add(closer);
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public void close() throws IOException {
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        super.close();
    }
}
