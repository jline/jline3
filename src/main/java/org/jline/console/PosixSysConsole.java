/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.jansi.Pty;
import org.jline.utils.InputStreamReader;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.Signals;

import static org.jline.utils.Preconditions.checkNotNull;

public class PosixSysConsole extends AbstractPosixConsole {

    private final NonBlockingReader reader;
    private final PrintWriter writer;
    private final Map<Signal, Object> nativeHandlers = new HashMap<Signal, Object>();

    public PosixSysConsole(String type, String appName, URL inputrc, Map<String, String> variables, String encoding, boolean nativeSignals) throws IOException {
        super(type, appName, inputrc, variables, Pty.current());
        checkNotNull(encoding);
        InputStream in = new FileInputStream(FileDescriptor.in);
        OutputStream out = new FileOutputStream(FileDescriptor.out);
        this.reader = new NonBlockingReader(new InputStreamReader(in, encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(out, encoding));
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
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        super.close();
    }
}
